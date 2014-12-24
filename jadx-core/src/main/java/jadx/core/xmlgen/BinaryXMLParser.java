package jadx.core.xmlgen;

import jadx.core.codegen.CodeWriter;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
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

	private byte[] bytes;
	private String[] strings;
	private int count;

	private String nsPrefix = "ERROR";
	private String nsURI = "ERROR";
	private String currentTag = "ERROR";

	private boolean firstElement;
	private boolean wasOneLiner = false;

	private CodeWriter writer;
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

	public void parse(byte[] xmlBytes, File out) {
		LOG.debug("Decoding AndroidManifest.xml, output: {}", out);

		writer = new CodeWriter();
		writer.add("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		bytes = xmlBytes;
		count = 0;
		firstElement = true;
		decode();
		writer.save(out);
	}

	void decode() {
		if (cInt16(bytes, count) != 0x0003) {
			die("Version is not 3");
		}
		if (cInt16(bytes, count) != 0x0008) {
			die("Size of header is not 8");
		}
		if (cInt32(bytes, count) != bytes.length) {
			die("Size of manifest doesn't match");
		}
		while ((count + 2) <= bytes.length) {
			int type = cInt16(bytes, count);
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

	private void parseStringPool() {
		if (cInt16(bytes, count) != 0x001c) {
			die("Header header size not 28");
		}
		int hsize = cInt32(bytes, count);
		int stringCount = cInt32(bytes, count);
		int styleCount = cInt32(bytes, count);
		int flags = cInt32(bytes, count);
		int stringsStart = cInt32(bytes, count);
		int stylesStart = cInt32(bytes, count);
		int[] stringsOffsets = new int[stringCount];
		for (int i = 0; i < stringCount; i++) {
			stringsOffsets[i] = cInt32(bytes, count);
		}
		strings = new String[stringCount];
		for (int i = 0; i < stringCount; i++) {
			int off = 8 + stringsStart + stringsOffsets[i];
			int strlen = cInt16(bytes, off);
			byte[] str = new byte[strlen * 2];
			System.arraycopy(bytes, count, str, 0, strlen * 2);
			count += strlen * 2;
			strings[i] = new String(str, Charset.forName("UTF-16LE"));
			count += 2;
		}
	}

	private void parseResourceMap() {
		if (cInt16(bytes, count) != 0x8) {
			die("Header size of resmap is not 8!");
		}
		int rhsize = cInt32(bytes, count);
		int[] ids = new int[(rhsize - 8) / 4];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = cInt32(bytes, count);
		}
	}

	private void parseNameSpace() {
		if (cInt16(bytes, count) != 0x0010) {
			die("NAMESPACE header is not 0x0010");
		}
		if (cInt32(bytes, count) != 0x18) {
			die("NAMESPACE header chunk is not 0x18 big");
		}
		int beginLineNumber = cInt32(bytes, count);
		int comment = cInt32(bytes, count);
		int beginPrefix = cInt32(bytes, count);
		nsPrefix = strings[beginPrefix];
		int beginURI = cInt32(bytes, count);
		nsURI = strings[beginURI];
	}

	private void parseNameSpaceEnd() {
		if (cInt16(bytes, count) != 0x0010) {
			die("NAMESPACE header is not 0x0010");
		}
		if (cInt32(bytes, count) != 0x18) {
			die("NAMESPACE header chunk is not 0x18 big");
		}
		int endLineNumber = cInt32(bytes, count);
		int comment = cInt32(bytes, count);
		int endPrefix = cInt32(bytes, count);
		nsPrefix = strings[endPrefix];
		int endURI = cInt32(bytes, count);
		nsURI = strings[endURI];
	}

	private void parseElement() {
		if (firstElement) {
			firstElement = false;
		} else {
			writer.incIndent();
		}
		if (cInt16(bytes, count) != 0x0010) {
			die("ELEMENT HEADER SIZE is not 0x10");
		}
		count += 4; // TODO: Check element chunk size
		int elementBegLineNumber = cInt32(bytes, count);
		int comment = cInt32(bytes, count);
		int startNS = cInt32(bytes, count);
		int startNSName = cInt32(bytes, count); // actually is elementName...
		if (!wasOneLiner && !"ERROR".equals(currentTag) && !currentTag.equals(strings[startNSName])) {
			writer.add(">");
		}
		wasOneLiner = false;
		currentTag = strings[startNSName];
		writer.startLine("<").add(strings[startNSName]);
		int attributeStart = cInt16(bytes, count);
		if (attributeStart != 0x14) {
			die("startNS's attributeStart is not 0x14");
		}
		int attributeSize = cInt16(bytes, count);
		if (attributeSize != 0x14) {
			die("startNS's attributeSize is not 0x14");
		}
		int attributeCount = cInt16(bytes, count);
		int idIndex = cInt16(bytes, count);
		int classIndex = cInt16(bytes, count);
		int styleIndex = cInt16(bytes, count);
		if ("manifest".equals(strings[startNSName])) {
			writer.add(" xmlns:\"").add(nsURI).add("\"");
		}
		if (attributeCount > 0) {
			writer.add(" ");
		}
		for (int i = 0; i < attributeCount; i++) {
			int attributeNS = cInt32(bytes, count);
			int attributeName = cInt32(bytes, count);
			int attributeRawValue = cInt32(bytes, count);
			int attrValSize = cInt16(bytes, count);
			if (attrValSize != 0x08) {
				die("attrValSize != 0x08 not supported");
			}
			if (cInt8(bytes, count) != 0) {
				die("res0 is not 0");
			}
			int attrValDataType = cInt8(bytes, count);
			int attrValData = cInt32(bytes, count);
			if (attributeNS != -1) {
				writer.add(nsPrefix).add(':');
			}
			String attrName = strings[attributeName];
			writer.add(attrName).add("=\"");
			String decodedAttr = attributes.decode(attrName, attrValData);
			if (decodedAttr != null) {
				writer.add(decodedAttr);
			} else {
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
			writer.add('"');
			if ((i + 1) < attributeCount) {
				writer.add(" ");
			}
		}
	}

	private void parseElementEnd() {
		if (cInt16(bytes, count) != 0x0010) {
			die("ELEMENT END header is not 0x0010");
		}
		if (cInt32(bytes, count) != 0x18) {
			die("ELEMENT END header chunk is not 0x18 big");
		}
		int endLineNumber = cInt32(bytes, count);
		int comment = cInt32(bytes, count);
		int elementNS = cInt32(bytes, count);
		int elementName = cInt32(bytes, count);
		if (currentTag.equals(strings[elementName])) {
			writer.add(" />");
			wasOneLiner = true;
		} else {
			writer.startLine("</");
			if (elementNS != -1) {
				writer.add(strings[elementNS]).add(':');
			}
			writer.add(strings[elementName]).add(">");
		}
		if (writer.getIndent() != 0) {
			writer.decIndent();
		}
	}

	private int cInt8(byte[] bytes, int offset) {
		byte[] tmp = new byte[4];
		tmp[3] = bytes[count++];
		return ByteBuffer.wrap(tmp).getInt();
	}

	private int cInt16(byte[] bytes, int offset) {
		byte[] tmp = new byte[4];
		tmp[3] = bytes[count++];
		tmp[2] = bytes[count++];
		return ByteBuffer.wrap(tmp).getInt();
	}

	private int cInt32(byte[] bytes, int offset) {
		byte[] tmp = new byte[4];
		for (int i = 0; i < 4; i++) {
			tmp[3 - i] = bytes[count + i];
		}
		count += 4;
		return ByteBuffer.wrap(tmp).getInt();
	}

	private void die(String message) {
		throw new JadxRuntimeException("Decode error: " + message);
	}
}
