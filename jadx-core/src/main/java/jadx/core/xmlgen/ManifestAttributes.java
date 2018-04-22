package jadx.core.xmlgen;

import javax.xml.parsers.DocumentBuilder;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jadx.core.utils.exceptions.JadxRuntimeException;

public class ManifestAttributes {
	private static final Logger LOG = LoggerFactory.getLogger(ManifestAttributes.class);

	private static final String ATTR_XML = "/android/attrs.xml";
	private static final String MANIFEST_ATTR_XML = "/android/attrs_manifest.xml";

	private enum MAttrType {
		ENUM, FLAG
	}

	private static class MAttr {
		private final MAttrType type;
		private final Map<Long, String> values = new LinkedHashMap<>();

		public MAttr(MAttrType type) {
			this.type = type;
		}

		public MAttrType getType() {
			return type;
		}

		public Map<Long, String> getValues() {
			return values;
		}

		@Override
		public String toString() {
			return "[" + type + ", " + values + "]";
		}
	}

	private final Map<String, MAttr> attrMap = new HashMap<>();

	private static ManifestAttributes instance;

	public static ManifestAttributes getInstance() {
		if (instance == null) {
			try {
				instance = new ManifestAttributes();
			} catch (Exception e) {
				LOG.error("Failed to create ManifestAttributes", e);
			}
		}
		return instance;
	}

	private ManifestAttributes() {
		parseAll();
	}

	private void parseAll() {
		parse(loadXML(ATTR_XML));
		parse(loadXML(MANIFEST_ATTR_XML));
		LOG.debug("Loaded android attributes count: {}", attrMap.size());
	}

	private Document loadXML(String xml) {
		Document doc;
		try (InputStream xmlStream = ManifestAttributes.class.getResourceAsStream(xml)) {
			if (xmlStream == null) {
				throw new JadxRuntimeException(xml + " not found in classpath");
			}
			DocumentBuilder dBuilder = XmlSecurity.getSecureDbf().newDocumentBuilder();
			doc = dBuilder.parse(xmlStream);
		} catch (Exception e) {
			throw new JadxRuntimeException("Xml load error, file: " + xml, e);
		}
		return doc;
	}

	private void parse(Document doc) {
		NodeList nodeList = doc.getChildNodes();
		for (int count = 0; count < nodeList.getLength(); count++) {
			Node node = nodeList.item(count);
			if (node.getNodeType() == Node.ELEMENT_NODE
					&& node.hasChildNodes()) {
				parseAttrList(node.getChildNodes());
			}
		}
	}

	private void parseAttrList(NodeList nodeList) {
		for (int count = 0; count < nodeList.getLength(); count++) {
			Node tempNode = nodeList.item(count);
			if (tempNode.getNodeType() == Node.ELEMENT_NODE
					&& tempNode.hasAttributes()
					&& tempNode.hasChildNodes()) {
				String name = null;
				NamedNodeMap nodeMap = tempNode.getAttributes();
				for (int i = 0; i < nodeMap.getLength(); i++) {
					Node node = nodeMap.item(i);
					if (node.getNodeName().equals("name")) {
						name = node.getNodeValue();
						break;
					}
				}
				if (name != null && tempNode.getNodeName().equals("attr")) {
					parseValues(name, tempNode.getChildNodes());
				} else {
					parseAttrList(tempNode.getChildNodes());
				}
			}
		}
	}

	private void parseValues(String name, NodeList nodeList) {
		MAttr attr = null;
		for (int count = 0; count < nodeList.getLength(); count++) {
			Node tempNode = nodeList.item(count);
			if (tempNode.getNodeType() == Node.ELEMENT_NODE
					&& tempNode.hasAttributes()) {
				if (attr == null) {
					if (tempNode.getNodeName().equals("enum")) {
						attr = new MAttr(MAttrType.ENUM);
					} else if (tempNode.getNodeName().equals("flag")) {
						attr = new MAttr(MAttrType.FLAG);
					}
					if (attr == null) {
						return;
					}
					attrMap.put(name, attr);
				}
				NamedNodeMap attributes = tempNode.getAttributes();
				Node nameNode = attributes.getNamedItem("name");
				if (nameNode != null) {
					Node valueNode = attributes.getNamedItem("value");
					if (valueNode != null) {
						try {
							long key;
							String nodeValue = valueNode.getNodeValue();
							if (nodeValue.startsWith("0x")) {
								nodeValue = nodeValue.substring(2);
								key = Long.parseLong(nodeValue, 16);
							} else {
								key = Long.parseLong(nodeValue);
							}
							attr.getValues().put(key, nameNode.getNodeValue());
						} catch (NumberFormatException e) {
							LOG.debug("Failed parse manifest number", e);
						}
					}
				}
			}
		}
	}

	public String decode(String attrName, long value) {
		MAttr attr = attrMap.get(attrName);
		if (attr == null) {
			return null;
		}
		if (attr.getType() == MAttrType.ENUM) {
			return attr.getValues().get(value);
		} else if (attr.getType() == MAttrType.FLAG) {
			StringBuilder sb = new StringBuilder();
			for (Map.Entry<Long, String> entry : attr.getValues().entrySet()) {
				if (value == entry.getKey()) {
					sb = new StringBuilder(entry.getValue() + "|");
					break;
				} else if ((value & entry.getKey()) == entry.getKey()) {
					sb.append(entry.getValue()).append('|');
				}
			}
			if (sb.length() != 0) {
				return sb.deleteCharAt(sb.length() - 1).toString();
			}
		}
		return null;
	}
}
