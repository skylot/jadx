package jadx.core.xmlgen;

import jadx.core.utils.exceptions.JadxException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
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

public class ManifestAttributes {
	private static final Logger LOG = LoggerFactory.getLogger(ManifestAttributes.class);

	private static final String MANIFEST_ATTR_XML = "/android/attrs_manifest.xml";

	private enum MAttrType {
		ENUM, FLAG
	}

	private static class MAttr {
		private final MAttrType type;
		private final Map<Integer, String> values = new LinkedHashMap<Integer, String>();

		public MAttr(MAttrType type) {
			this.type = type;
		}

		public MAttrType getType() {
			return type;
		}

		public Map<Integer, String> getValues() {
			return values;
		}

		@Override
		public String toString() {
			return "[" + type + ", " + values + "]";
		}
	}

	private final Document doc;
	private final Map<String, MAttr> attrMap = new HashMap<String, MAttr>();

	public ManifestAttributes() throws Exception {
		InputStream xmlStream = null;
		try {
			xmlStream = ManifestAttributes.class.getResourceAsStream(MANIFEST_ATTR_XML);
			if (xmlStream == null) {
				throw new JadxException(MANIFEST_ATTR_XML + " not found in classpath");
			}
			DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			doc = dBuilder.parse(xmlStream);
		} finally {
			if (xmlStream != null) {
				xmlStream.close();
			}
		}
	}

	public void parse() {
		NodeList nodeList = doc.getChildNodes();
		for (int count = 0; count < nodeList.getLength(); count++) {
			Node node = nodeList.item(count);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				if (node.hasChildNodes()) {
					parseAttrList(node.getChildNodes());
				}
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
							int key;
							String nodeValue = valueNode.getNodeValue();
							if (attr.getType() == MAttrType.ENUM) {
								key = Integer.parseInt(nodeValue);
							} else {
								nodeValue = nodeValue.replace("0x", "");
								key = Integer.parseInt(nodeValue, 16);
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

	public String decode(String attrName, int value) {
		MAttr attr = attrMap.get(attrName);
		if (attr == null) {
			return null;
		}
		if (attr.getType() == MAttrType.ENUM) {
			String name = attr.getValues().get(value);
			if (name != null) {
				return name;
			}
		} else if (attr.getType() == MAttrType.FLAG) {
			StringBuilder sb = new StringBuilder();
			for (Map.Entry<Integer, String> entry : attr.getValues().entrySet()) {
				if ((value & entry.getKey()) != 0) {
					sb.append(entry.getValue()).append('|');
				}
			}
			if (sb.length() != 0) {
				return sb.deleteCharAt(sb.length() - 1).toString();
			}
		}
		return "UNKNOWN_DATA_0x" + Integer.toHexString(value);
	}
}
