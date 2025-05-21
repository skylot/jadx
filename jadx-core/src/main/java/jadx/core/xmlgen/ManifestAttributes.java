package jadx.core.xmlgen;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jadx.api.security.IJadxSecurity;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.xmlgen.entry.RawNamedValue;
import jadx.core.xmlgen.entry.ResourceEntry;
import jadx.core.xmlgen.entry.ValuesParser;

// TODO: move to Android specific module!

/**
 * Load and store Android Manifest attributes specification
 */
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

		public void addValue(long key, String value) {
			values.put(key, value);
		}

		@Override
		public String toString() {
			return "[" + type + ", " + values + ']';
		}
	}

	private final IJadxSecurity security;

	/**
	 * Map containing default Android resource attribute definitions.
	 * Keys are Android attribute names (e.g., "android:layout_width"),
	 * and values are their corresponding {@link MAttr} objects.
	 */
	private final Map<String, MAttr> attrMap = new HashMap<>();
	private final Map<String, MAttr> appAttrMap = new HashMap<>();

	public ManifestAttributes(IJadxSecurity security) {
		this.security = security;
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
			doc = security.parseXml(xmlStream);
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
					attrMap.put("android:" + name, attr);
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
							attr.addValue(key, nameNode.getNodeValue());
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
			if (attrName.contains(":")) {
				attrName = attrName.split(":", 2)[1];
			}
			attr = appAttrMap.get(attrName);
			if (attr == null) {
				return null;
			}
		}

		Map<Long, String> attrValuesMap = attr.getValues();
		if (attr.getType() == MAttrType.ENUM) {
			return attrValuesMap.get(value);
		} else if (attr.getType() == MAttrType.FLAG) {
			List<String> flagList = new ArrayList<>();
			List<Long> attrKeys = new ArrayList<>(attrValuesMap.keySet());
			attrKeys.sort((a, b) -> Long.compare(b, a)); // sort descending
			for (Long key : attrKeys) {
				String attrValue = attrValuesMap.get(key);
				if (value == key) {
					flagList.add(attrValue);
					break;
				} else if ((key != 0) && ((value & key) == key)) {
					flagList.add(attrValue);
					value ^= key;
				}
			}
			return String.join("|", flagList);
		}
		return null;
	}

	public void updateAttributes(IResTableParser parser) {
		appAttrMap.clear();

		ResourceStorage resStorage = parser.getResStorage();
		ValuesParser vp = new ValuesParser(parser.getStrings(), resStorage.getResourcesNames());

		for (ResourceEntry ri : resStorage.getResources()) {
			if (ri.getProtoValue() != null) {
				// Aapt proto decoder resolves attributes by itself.
				continue;
			}

			if (ri.getTypeName().equals("attr") && ri.getNamedValues().size() > 1) {
				RawNamedValue first = ri.getNamedValues().get(0);
				MAttrType attrTyp;
				int attrTypeVal = first.getRawValue().getData() & 0xff0000;
				if (attrTypeVal == ValuesParser.ATTR_TYPE_FLAGS) {
					attrTyp = MAttrType.FLAG;
				} else if (attrTypeVal == ValuesParser.ATTR_TYPE_ENUM) {
					attrTyp = MAttrType.ENUM;
				} else {
					continue;
				}
				MAttr attr = new MAttr(attrTyp);
				for (int i = 1; i < ri.getNamedValues().size(); i++) {
					RawNamedValue rv = ri.getNamedValues().get(i);
					String value = vp.decodeNameRef(rv.getNameRef());
					attr.addValue(rv.getRawValue().getData(), value.startsWith("id.") ? value.substring(3) : value);
				}
				appAttrMap.put(ri.getKeyName(), attr);
			}
		}
	}
}
