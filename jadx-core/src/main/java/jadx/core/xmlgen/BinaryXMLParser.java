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

	private static final Charset STRING_CHARSET = Charset.forName("UTF-16LE");

	private CodeWriter writer;
	private InputStream input;
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
		input = inputStream;
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
		if (cInt16() != 0x0003) {
			die("Version is not 3");
		}
		if (cInt16() != 0x0008) {
			die("Size of header is not 8");
		}
		cInt32();
		while (input.available() != 0) {
			int type = cInt16();
			switch (type) {
				case 0x0001:
					parseStringPool();
					break;
				case 0x0180:
					parseResourceMap();
					break;
				case 0x0100:
					parseNameSpace();
					break;
				case 0x0101:
					parseNameSpaceEnd();
					break;
				case 0x0102:
					parseElement();
					break;
				case 0x0103:
					parseElementEnd();
					break;
				case 0x0000:
					// NullType is just doing nothing
					break;

				default:
					die("Type: " + Integer.toHexString(type) + " not yet implemented");
					break;
			}
		}
	}

	private void parseStringPool() throws IOException {
		if (cInt16() != 0x001c) {
			die("Header header size not 28");
		}
		int hsize = cInt32();
		int stringCount = cInt32();
		int styleCount = cInt32();
		int flags = cInt32();
		int stringsStart = cInt32();
		int stylesStart = cInt32();
		int[] stringsOffsets = new int[stringCount];
		for (int i = 0; i < stringCount; i++) {
			stringsOffsets[i] = cInt32();
		}
		strings = new String[stringCount];
		for (int i = 0; i < stringCount; i++) {
			int off = 8 + stringsStart + stringsOffsets[i];
			int strlen = cInt16();
			byte[] str = new byte[strlen * 2];
			readToArray(str);
			strings[i] = new String(str, STRING_CHARSET);
			cInt16();
		}
	}

	private void parseResourceMap() throws IOException {
		if (cInt16() != 0x8) {
			die("Header size of resmap is not 8!");
		}
		int rhsize = cInt32();
		int[] ids = new int[(rhsize - 8) / 4];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = cInt32();
		}
	}

	private void parseNameSpace() throws IOException {
		if (cInt16() != 0x10) {
			die("NAMESPACE header is not 0x0010");
		}
		if (cInt32() != 0x18) {
			die("NAMESPACE header chunk is not 0x18 big");
		}
		int beginLineNumber = cInt32();
		int comment = cInt32();
		int beginPrefix = cInt32();
		nsPrefix = strings[beginPrefix];
		int beginURI = cInt32();
		nsURI = strings[beginURI];
	}

	private void parseNameSpaceEnd() throws IOException {
		if (cInt16() != 0x10) {
			die("NAMESPACE header is not 0x0010");
		}
		if (cInt32() != 0x18) {
			die("NAMESPACE header chunk is not 0x18 big");
		}
		int endLineNumber = cInt32();
		int comment = cInt32();
		int endPrefix = cInt32();
		nsPrefix = strings[endPrefix];
		int endURI = cInt32();
		nsURI = strings[endURI];
	}

	private void parseElement() throws IOException {
		if (firstElement) {
			firstElement = false;
		} else {
			writer.incIndent();
		}
		if (cInt16() != 0x10) {
			die("ELEMENT HEADER SIZE is not 0x10");
		}
		// TODO: Check element chunk size
		cInt32();
		int elementBegLineNumber = cInt32();
		int comment = cInt32();
		int startNS = cInt32();
		int startNSName = cInt32(); // actually is elementName...
		if (!wasOneLiner && !"ERROR".equals(currentTag) && !currentTag.equals(strings[startNSName])) {
			writer.add(">");
		}
		wasOneLiner = false;
		currentTag = strings[startNSName];
		writer.startLine("<").add(strings[startNSName]);
		writer.attachSourceLine(elementBegLineNumber);
		int attributeStart = cInt16();
		if (attributeStart != 0x14) {
			die("startNS's attributeStart is not 0x14");
		}
		int attributeSize = cInt16();
		if (attributeSize != 0x14) {
			die("startNS's attributeSize is not 0x14");
		}
		int attributeCount = cInt16();
		int idIndex = cInt16();
		int classIndex = cInt16();
		int styleIndex = cInt16();
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
		int attributeNS = cInt32();
		int attributeName = cInt32();
		int attributeRawValue = cInt32();
		int attrValSize = cInt16();
		if (attrValSize != 0x08) {
			die("attrValSize != 0x08 not supported");
		}
		if (cInt8() != 0) {
			die("res0 is not 0");
		}
		int attrValDataType = cInt8();
		int attrValData = cInt32();
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
		if (cInt16() != 0x10) {
			die("ELEMENT END header is not 0x10");
		}
		if (cInt32() != 0x18) {
			die("ELEMENT END header chunk is not 0x18 big");
		}
		int endLineNumber = cInt32();
		int comment = cInt32();
		int elementNS = cInt32();
		int elementName = cInt32();
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

	private int cInt8() throws IOException {
		return input.read();
	}

	private int cInt16() throws IOException {
		int b1 = input.read();
		int b2 = input.read();
		return (b2 & 0xFF) << 8 | (b1 & 0xFF);
	}

	private int cInt32() throws IOException {
		InputStream in = input;
		int b1 = in.read();
		int b2 = in.read();
		int b3 = in.read();
		int b4 = in.read();
		return b4 << 24 | (b3 & 0xFF) << 16 | (b2 & 0xFF) << 8 | (b1 & 0xFF);
	}

	private void readToArray(byte[] arr) throws IOException {
		int count = arr.length;
		int pos = input.read(arr, 0, count);
		while (pos < count) {
			int read = input.read(arr, pos, count - pos);
			if (read == -1) {
				throw new IOException("No data, can't read " + count + " bytes");
			}
			pos += read;
		}
	}

	private void die(String message) {
		throw new JadxRuntimeException("Decode error: " + message);
	}
}
