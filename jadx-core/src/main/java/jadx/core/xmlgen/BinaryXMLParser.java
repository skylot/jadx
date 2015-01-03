package jadx.core.xmlgen;

import jadx.core.codegen.CodeWriter;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
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

public class BinaryXMLParser {

	private static final Logger LOG = LoggerFactory.getLogger(BinaryXMLParser.class);

	private static final Charset STRING_CHARSET_UTF16 = Charset.forName("UTF-16LE");
	private static final Charset STRING_CHARSET_UTF8 = Charset.forName("UTF-8");

	private static final int RES_NULL_TYPE = 0x0000;
	private static final int RES_STRING_POOL_TYPE = 0x0001;
	private static final int RES_TABLE_TYPE = 0x0002;

	private static final int RES_XML_TYPE = 0x0003;
	private static final int RES_XML_FIRST_CHUNK_TYPE = 0x0100;
	private static final int RES_XML_START_NAMESPACE_TYPE = 0x0100;
	private static final int RES_XML_END_NAMESPACE_TYPE = 0x0101;
	private static final int RES_XML_START_ELEMENT_TYPE = 0x0102;
	private static final int RES_XML_END_ELEMENT_TYPE = 0x0103;
	private static final int RES_XML_CDATA_TYPE = 0x0104;
	private static final int RES_XML_LAST_CHUNK_TYPE = 0x017f;
	private static final int RES_XML_RESOURCE_MAP_TYPE = 0x0180;

	private static final int RES_TABLE_PACKAGE_TYPE = 0x0200;
	private static final int RES_TABLE_TYPE_TYPE = 0x0201;
	private static final int RES_TABLE_TYPE_SPEC_TYPE = 0x0202;

	// string pool flags
	private static final int SORTED_FLAG = 1;
	private static final int UTF8_FLAG = 1 << 8;

	private CodeWriter writer;
	private ParserStream is;
	private String[] strings;

	private String nsPrefix = "ERROR";
	private String nsURI = "ERROR";
	private String currentTag = "ERROR";

	private boolean firstElement;
	private boolean wasOneLiner = false;

	private Map<Integer, String> styleMap = new HashMap<Integer, String>();
	private Map<Integer, FieldNode> localStyleMap = new HashMap<Integer, FieldNode>();

	private final ManifestAttributes attributes;

	public BinaryXMLParser(RootNode root) {
		try {
			for (Field f : AndroidR.style.class.getFields()) {
				styleMap.put(f.getInt(f.getType()), f.getName());
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
			attributes = new ManifestAttributes();
			attributes.parse();
		} catch (Exception e) {
			throw new JadxRuntimeException("BinaryXMLParser init error", e);
		}
	}

	public synchronized CodeWriter parse(InputStream inputStream) {
		writer = new CodeWriter();
		writer.add("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		is = new ParserStream(inputStream);
		firstElement = true;
		try {
			decode();
		} catch (IOException e) {
			LOG.debug("Binary xml decode failed", e);
			CodeWriter cw = new CodeWriter();
			cw.add("Error decode binary xml");
			cw.startLine(Utils.getStackTrace(e));
			return cw;
		}
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
					parseStringPool();
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
					die("Type: " + Integer.toHexString(type) + " not yet implemented");
					break;
			}
		}
	}

	private void parseStringPool() throws IOException {
		if (is.readInt16() != 0x001c) {
			die("Header header size not 28");
		}
		int hsize = is.readInt32();
		int stringCount = is.readInt32();
		int styleCount = is.readInt32();
		int flags = is.readInt32();
		int stringsStart = is.readInt32();
		int stylesStart = is.readInt32();
		// skip string offsets
		is.skip(stringCount * 4);
		strings = new String[stringCount];
		if ((flags & UTF8_FLAG) != 0) {
			// UTF-8
			long start = is.getPos();
			for (int i = 0; i < stringCount; i++) {
				int charsCount = is.decodeLength8();
				int len = is.decodeLength8();
				strings[i] = new String(is.readArray(len), STRING_CHARSET_UTF8);
				int zero = is.readInt8();
				if (zero != 0) {
					die("Not a trailing zero at string end: " + zero + ", " + strings[i]);
				}
			}
			long shift = is.getPos() - start;
			if (shift % 2 != 0) {
				is.skip(1);
			}
		} else {
			// UTF-16
			for (int i = 0; i < stringCount; i++) {
				int len = is.decodeLength16();
				strings[i] = new String(is.readArray(len * 2), STRING_CHARSET_UTF16);
				int zero = is.readInt16();
				if (zero != 0) {
					die("Not a trailing zero at string end: " + zero + ", " + strings[i]);
				}
			}
		}
		if (styleCount != 0) {
			die("Styles parsing in string pool not yet implemented");
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
			if ((i + 1) < attributeCount) {
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
		switch (attrValDataType) {
			case 0x3:
				writer.add(strings[attrValData]);
				break;

			case 0x10:
				writer.add(String.valueOf(attrValData));
				break;

			case 0x12:
				// FIXME: What to do, when data is always -1?
				if (attrValData == 0) {
					writer.add("false");
				} else if (attrValData == 1 || attrValData == -1) {
					writer.add("true");
				} else {
					writer.add("UNKNOWN_BOOLEAN_TYPE");
				}
				break;

			case 0x1:
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
						writer.add("0x").add(Integer.toHexString(attrValData));
					}
				}
				break;

			default:
				writer.add("UNKNOWN_DATA_TYPE_" + attrValDataType);
				break;
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

	private void die(String message) {
		throw new JadxRuntimeException("Decode error: " + message
				+ ", position: 0x" + Long.toHexString(is.getPos()));
	}
}
