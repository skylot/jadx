package jadx.core.xmlgen;

import jadx.core.codegen.CodeWriter;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.xmlgen.entry.ValuesParser;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* TODO:
	Don't die when error occurs
	Check error cases, maybe checked const values are not always the same
	Better error messages
	What to do, when Binary XML Manifest is > size(int)?
	Check for missing chunk size types
	Implement missing data types
	Use line numbers to recreate EXACT AndroidManifest
	Check Element chunk size
*/

public class BinaryXMLParser extends CommonBinaryParser {

	private static final Logger LOG = LoggerFactory.getLogger(BinaryXMLParser.class);
	private static final String ANDROID_R_STYLE_CLS = "android.R$style";

	private CodeWriter writer;
	private String[] strings;

	private String nsPrefix = "ERROR";
	private String nsURI = "ERROR";
	private String currentTag = "ERROR";

	private boolean firstElement;
	private boolean wasOneLiner = false;

	private final Map<Integer, String> styleMap = new HashMap<Integer, String>();
	private final Map<Integer, FieldNode> localStyleMap = new HashMap<Integer, FieldNode>();
	private final Map<Integer, String> resNames;
	private ValuesParser valuesParser;

	private final ManifestAttributes attributes;

	public BinaryXMLParser(RootNode root) {
		try {
			try {
				Class<?> rStyleCls = Class.forName(ANDROID_R_STYLE_CLS);
				for (Field f : rStyleCls.getFields()) {
					styleMap.put(f.getInt(f.getType()), f.getName());
				}
			} catch (Throwable th) {
				LOG.error("R class loading failed", th);
			}
			// add application constants
			for (DexNode dexNode : root.getDexNodes()) {
				for (Map.Entry<Object, FieldNode> entry : dexNode.getConstFields().entrySet()) {
					Object key = entry.getKey();
					FieldNode field = entry.getValue();
					if (field.getType().equals(ArgType.INT) && key instanceof Integer) {
						localStyleMap.put((Integer) key, field);
					}
				}
			}

			resNames = root.getResourcesNames();

			attributes = new ManifestAttributes();
			attributes.parse();
		} catch (Exception e) {
			throw new JadxRuntimeException("BinaryXMLParser init error", e);
		}
	}

	public synchronized CodeWriter parse(InputStream inputStream) throws IOException {
		writer = new CodeWriter();
		writer.add("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		is = new ParserStream(inputStream);
		firstElement = true;
		decode();
		writer.finish();
		return writer;
	}

	void decode() throws IOException {
		if (is.readInt16() != 0x0003) {
			die("Version is not 3");
		}
		if (is.readInt16() != 0x0008) {
			die("Size of header is not 8");
		}
		int size = is.readInt32();
		while (is.getPos() < size) {
			int type = is.readInt16();
			switch (type) {
				case RES_NULL_TYPE:
					// NullType is just doing nothing
					break;
				case RES_STRING_POOL_TYPE:
					strings = parseStringPoolNoType();
					valuesParser = new ValuesParser(strings, resNames);
					break;
				case RES_XML_RESOURCE_MAP_TYPE:
					parseResourceMap();
					break;
				case RES_XML_START_NAMESPACE_TYPE:
					parseNameSpace();
					break;
				case RES_XML_END_NAMESPACE_TYPE:
					parseNameSpaceEnd();
					break;
				case RES_XML_START_ELEMENT_TYPE:
					parseElement();
					break;
				case RES_XML_END_ELEMENT_TYPE:
					parseElementEnd();
					break;

				default:
					die("Type: 0x" + Integer.toHexString(type) + " not yet implemented");
					break;
			}
		}
	}

	private void parseResourceMap() throws IOException {
		if (is.readInt16() != 0x8) {
			die("Header size of resmap is not 8!");
		}
		int rhsize = is.readInt32();
		int[] ids = new int[(rhsize - 8) / 4];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = is.readInt32();
		}
	}

	private void parseNameSpace() throws IOException {
		if (is.readInt16() != 0x10) {
			die("NAMESPACE header is not 0x0010");
		}
		if (is.readInt32() != 0x18) {
			die("NAMESPACE header chunk is not 0x18 big");
		}
		int beginLineNumber = is.readInt32();
		int comment = is.readInt32();
		int beginPrefix = is.readInt32();
		nsPrefix = strings[beginPrefix];
		int beginURI = is.readInt32();
		nsURI = strings[beginURI];
	}

	private void parseNameSpaceEnd() throws IOException {
		if (is.readInt16() != 0x10) {
			die("NAMESPACE header is not 0x0010");
		}
		if (is.readInt32() != 0x18) {
			die("NAMESPACE header chunk is not 0x18 big");
		}
		int endLineNumber = is.readInt32();
		int comment = is.readInt32();
		int endPrefix = is.readInt32();
		nsPrefix = strings[endPrefix];
		int endURI = is.readInt32();
		nsURI = strings[endURI];
	}

	private void parseElement() throws IOException {
		if (firstElement) {
			firstElement = false;
		} else {
			writer.incIndent();
		}
		if (is.readInt16() != 0x10) {
			die("ELEMENT HEADER SIZE is not 0x10");
		}
		// TODO: Check element chunk size
		is.readInt32();
		int elementBegLineNumber = is.readInt32();
		int comment = is.readInt32();
		int startNS = is.readInt32();
		int startNSName = is.readInt32(); // actually is elementName...
		if (!wasOneLiner && !"ERROR".equals(currentTag) && !currentTag.equals(strings[startNSName])) {
			writer.add(">");
		}
		wasOneLiner = false;
		currentTag = strings[startNSName];
		writer.startLine("<").add(strings[startNSName]);
		writer.attachSourceLine(elementBegLineNumber);
		int attributeStart = is.readInt16();
		if (attributeStart != 0x14) {
			die("startNS's attributeStart is not 0x14");
		}
		int attributeSize = is.readInt16();
		if (attributeSize != 0x14) {
			die("startNS's attributeSize is not 0x14");
		}
		int attributeCount = is.readInt16();
		int idIndex = is.readInt16();
		int classIndex = is.readInt16();
		int styleIndex = is.readInt16();
		if ("manifest".equals(strings[startNSName])) {
			writer.add(" xmlns:\"").add(nsURI).add("\"");
		}
		if (attributeCount > 0) {
			writer.add(" ");
		}
		for (int i = 0; i < attributeCount; i++) {
			parseAttribute(i);
			writer.add('"');
			if (i + 1 < attributeCount) {
				writer.add(" ");
			}
		}
	}

	private void parseAttribute(int i) throws IOException {
		int attributeNS = is.readInt32();
		int attributeName = is.readInt32();
		int attributeRawValue = is.readInt32();
		int attrValSize = is.readInt16();
		if (attrValSize != 0x08) {
			die("attrValSize != 0x08 not supported");
		}
		if (is.readInt8() != 0) {
			die("res0 is not 0");
		}
		int attrValDataType = is.readInt8();
		int attrValData = is.readInt32();
		if (attributeNS != -1) {
			writer.add(nsPrefix).add(':');
		}
		String attrName = strings[attributeName];
		writer.add(attrName).add("=\"");
		String decodedAttr = attributes.decode(attrName, attrValData);
		if (decodedAttr != null) {
			writer.add(decodedAttr);
		} else {
			decodeAttribute(attributeNS, attrValDataType, attrValData);
		}
	}

	private void decodeAttribute(int attributeNS, int attrValDataType, int attrValData) {
		if (attrValDataType == TYPE_REFERENCE) {
			// reference custom processing
			String name = styleMap.get(attrValData);
			if (name != null) {
				writer.add("@*");
				if (attributeNS != -1) {
					writer.add(nsPrefix).add(':');
				}
				writer.add("style/").add(name.replaceAll("_", "."));
			} else {
				FieldNode field = localStyleMap.get(attrValData);
				if (field != null) {
					String cls = field.getParentClass().getShortName().toLowerCase();
					writer.add("@").add(cls).add("/").add(field.getName());
				} else {
					String resName = resNames.get(attrValData);
					if (resName != null) {
						writer.add("@").add(resName);
					} else {
						writer.add("0x").add(Integer.toHexString(attrValData));
					}
				}
			}
		} else {
			String str = valuesParser.decodeValue(attrValDataType, attrValData);
			writer.add(str != null ? str : "null");
		}
	}

	private void parseElementEnd() throws IOException {
		if (is.readInt16() != 0x10) {
			die("ELEMENT END header is not 0x10");
		}
		if (is.readInt32() != 0x18) {
			die("ELEMENT END header chunk is not 0x18 big");
		}
		int endLineNumber = is.readInt32();
		int comment = is.readInt32();
		int elementNS = is.readInt32();
		int elementName = is.readInt32();
		if (currentTag.equals(strings[elementName])) {
			writer.add(" />");
			wasOneLiner = true;
		} else {
			writer.startLine("</");
			writer.attachSourceLine(endLineNumber);
			if (elementNS != -1) {
				writer.add(strings[elementNS]).add(':');
			}
			writer.add(strings[elementName]).add(">");
		}
		if (writer.getIndent() != 0) {
			writer.decIndent();
		}
	}
}
