package jadx.core.xmlgen;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ResourcesLoader;
import jadx.core.codegen.CodeWriter;
import jadx.core.dex.info.ConstStorage;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.StringUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.xmlgen.entry.ValuesParser;

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
	private static final boolean ATTR_NEW_LINE = false;

	private final Map<Integer, String> styleMap = new HashMap<>();
	private final Map<Integer, FieldNode> localStyleMap = new HashMap<>();
	private final Map<Integer, String> resNames;
	private final Map<String, String> nsMap = new HashMap<>();
	private Set<String> nsMapGenerated;
	private final Map<String, String> tagAttrDeobfNames = new HashMap<>();

	private CodeWriter writer;
	private String[] strings;
	private String currentTag = "ERROR";
	private boolean firstElement;
	private ValuesParser valuesParser;
	private boolean isLastEnd = true;
	private boolean isOneLine = true;
	private int namespaceDepth = 0;
	private int[] resourceIds;

	public BinaryXMLParser(RootNode root) {
		try {
			readAndroidRStyleClass();
			// add application constants
			ConstStorage constStorage = root.getConstValues();
			Map<Object, FieldNode> constFields = constStorage.getGlobalConstFields();
			for (Map.Entry<Object, FieldNode> entry : constFields.entrySet()) {
				Object key = entry.getKey();
				FieldNode field = entry.getValue();
				if (field.getType().equals(ArgType.INT) && key instanceof Integer) {
					localStyleMap.put((Integer) key, field);
				}
			}
			resNames = constStorage.getResourcesNames();
		} catch (Exception e) {
			throw new JadxRuntimeException("BinaryXMLParser init error", e);
		}
	}

	private void readAndroidRStyleClass() {
		try {
			Class<?> rStyleCls = Class.forName(ANDROID_R_STYLE_CLS);
			for (Field f : rStyleCls.getFields()) {
				styleMap.put(f.getInt(f.getType()), f.getName());
			}
		} catch (Exception th) {
			LOG.error("Android R class loading failed", th);
		}
	}

	public synchronized CodeWriter parse(InputStream inputStream) throws IOException {
		is = new ParserStream(inputStream);
		if (!isBinaryXml()) {
			return ResourcesLoader.loadToCodeWriter(inputStream);
		}
		nsMapGenerated = new HashSet<>();
		writer = new CodeWriter();
		writer.add("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		firstElement = true;
		decode();
		writer.finish();
		return writer;
	}

	private boolean isBinaryXml() throws IOException {
		is.mark(4);
		int v = is.readInt16(); // version
		int h = is.readInt16(); // header size
		if (v == 0x0003 && h == 0x0008) {
			return true;
		}
		is.reset();
		return false;
	}

	void decode() throws IOException {
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
				case RES_XML_CDATA_TYPE:
					parseCData();
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
					if (namespaceDepth == 0) {
						// skip padding on file end
						return;
					}
					die("Type: 0x" + Integer.toHexString(type) + " not yet implemented");
					break;
			}
		}
	}

	private void parseResourceMap() throws IOException {
		if (is.readInt16() != 0x8) {
			die("Header size of resmap is not 8!");
		}
		int size = is.readInt32();
		int len = (size - 8) / 4;
		resourceIds = new int[len];
		for (int i = 0; i < len; i++) {
			resourceIds[i] = is.readInt32();
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
		int beginURI = is.readInt32();
		
		String nsValue = getString(beginPrefix);
		if(!nsMap.containsValue(nsValue)) {
			nsMap.putIfAbsent(getString(beginURI), nsValue);
		}
		namespaceDepth++;
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
		int endURI = is.readInt32();
		namespaceDepth--;
		
		String nsValue = getString(endPrefix);
		if(!nsMap.containsValue(nsValue)) {
			nsMap.putIfAbsent(getString(endURI), nsValue);
		}
	}

	private void parseCData() throws IOException {
		if (is.readInt16() != 0x10) {
			die("CDATA header is not 0x10");
		}
		if (is.readInt32() != 0x1C) {
			die("CDATA header chunk is not 0x1C");
		}
		int lineNumber = is.readInt32();
		is.skip(4);

		int strIndex = is.readInt32();
		String str = getString(strIndex);
		if (!isLastEnd) {
			isLastEnd = true;
			writer.add(">");
		}
		writer.attachSourceLine(lineNumber);
		String escapedStr = StringUtils.escapeXML(str);
		writer.add(escapedStr);

		long size = is.readInt16();
		is.skip(size - 2);
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
		if (!isLastEnd && !"ERROR".equals(currentTag)) {
			writer.add(">");
		}
		isOneLine = true;
		isLastEnd = false;
		currentTag = getValidTagAttributeName(getString(startNSName));
		writer.startLine("<").add(currentTag);
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
		if ("manifest".equals(currentTag) || writer.getIndent() == 0) {
			for (Map.Entry<String, String> entry : nsMap.entrySet()) {
				String nsValue = entry.getValue();
				writer.add(" xmlns");
				if (nsValue != null && !nsValue.trim().isEmpty()) {
					writer.add(':');
					writer.add(nsValue);
				}
				writer.add("=\"").add(StringUtils.escapeXML(entry.getKey())).add("\"");
			}
		}
		boolean attrNewLine = attributeCount != 1 && ATTR_NEW_LINE;
		for (int i = 0; i < attributeCount; i++) {
			parseAttribute(i, attrNewLine);
		}
	}

	private void parseAttribute(int i, boolean newLine) throws IOException {
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

		if (newLine) {
			writer.startLine().addIndent();
		} else {
			writer.add(' ');
		}
		if (attributeNS != -1) {
			writer.add(getAttributeNS(attributeNS)).add(':');
		}
		String attrName = getValidTagAttributeName(getAttributeName(attributeName));
		writer.add(attrName).add("=\"");
		String decodedAttr = ManifestAttributes.getInstance().decode(attrName, attrValData);
		if (decodedAttr != null) {
			writer.add(StringUtils.escapeXML(decodedAttr));
		} else {
			decodeAttribute(attributeNS, attrValDataType, attrValData);
		}
		writer.add('"');
	}

	private String getAttributeNS(int attributeNS) {
		String attrUrl = getString(attributeNS);
		if (attrUrl == null || attrUrl.isEmpty()) {
			if (isResInternalId(attributeNS)) {
				return null;
			} else {
				attrUrl = ANDROID_NS_URL;
			}
		}
		String attrName = nsMap.get(attrUrl);
		if (attrName == null) {
			attrName = generateNameForNS(attrUrl);
		}
		return attrName;
	}
	
	private String generateNameForNS(String attrUrl) {
		for(int i = 1; ; i++) {
			String attrName = "ns" + i;
			if(!nsMap.containsValue(attrName) && !nsMapGenerated.contains(attrName)) {
				nsMapGenerated.add(attrName);
				// do not add generated value to nsMap
				// because attrUrl might be used in a neighbor element, but never defined
				writer.add("xmlns:").add(attrName)
					.add("=\"").add(attrUrl).add("\" ");
				return attrName;
			}
		}
	}

	private String getAttributeName(int id) {
		String str = getString(id);
		if (str == null || str.isEmpty()) {
			int resId = resourceIds[id];
			str = ValuesParser.getAndroidResMap().get(resId);
			if (str == null) {
				return "NOT_FOUND_0x" + Integer.toHexString(id);
			}
			// cut type before /
			int typeEnd = str.indexOf('/');
			if (typeEnd != -1) {
				return str.substring(typeEnd + 1);
			}
			return str;
		}
		return str;
	}

	private String getString(int strId) {
		if (0 <= strId && strId < strings.length) {
			return strings[strId];
		}
		return "NOT_FOUND_STR_0x" + Integer.toHexString(strId);
	}

	private void decodeAttribute(int attributeNS, int attrValDataType, int attrValData) {
		if (attrValDataType == TYPE_REFERENCE) {
			// reference custom processing
			String name = styleMap.get(attrValData);
			if (name != null) {
				writer.add("@");
				if (attributeNS != -1) {
					writer.add(getAttributeNS(attributeNS)).add(':');
				}
				writer.add("style/").add(name.replaceAll("_", "."));
			} else {
				FieldNode field = localStyleMap.get(attrValData);
				if (field != null) {
					String cls = field.getParentClass().getShortName().toLowerCase();
					writer.add("@");
					if ("id".equals(cls)) {
						writer.add('+');
					}
					writer.add(cls).add("/").add(field.getName());
				} else {
					String resName = resNames.get(attrValData);
					if (resName != null) {
						writer.add("@");
						if (resName.startsWith("id/")) {
							writer.add("+");
						}
						writer.add(resName);
					} else {
						resName = ValuesParser.getAndroidResMap().get(attrValData);
						if (resName != null) {
							writer.add("@android:").add(resName);
						} else if (attrValData == 0) {
							writer.add("@null");
						} else {
							writer.add("0x").add(Integer.toHexString(attrValData));
						}
					}
				}
			}
		} else {
			String str = valuesParser.decodeValue(attrValDataType, attrValData);
			writer.add(str != null ? StringUtils.escapeXML(str) : "null");
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
		int elementNameId = is.readInt32();
		String elemName = getValidTagAttributeName(getString(elementNameId));
		if (currentTag.equals(elemName) && isOneLine && !isLastEnd) {
			writer.add("/>");
		} else {
			writer.startLine("</");
			writer.attachSourceLine(endLineNumber);
//			if (elementNS != -1) {
//				writer.add(getString(elementNS)).add(':');
//			}
			writer.add(elemName).add(">");
		}
		isLastEnd = true;
		if (writer.getIndent() != 0) {
			writer.decIndent();
		}
	}
	
	private String getValidTagAttributeName(String originalName) {
		if(XMLChar.isValidName(originalName)) {
			return originalName;
		}
		if(tagAttrDeobfNames.containsKey(originalName)) {
			return tagAttrDeobfNames.get(originalName);
		}
		String generated;
		do {
			generated = generateTagAttrName();
		}
		while(tagAttrDeobfNames.containsValue(generated));
		tagAttrDeobfNames.put(originalName, generated);
		return generated;
	}
	
	private static String generateTagAttrName() {
		final int length = 6;
		Random r = new Random();
		StringBuilder sb = new StringBuilder();
		for(int i = 1; i <= length; i++) {
			sb.append((char)(r.nextInt(26) + 'a'));
		}
		return sb.toString();
	}
}
