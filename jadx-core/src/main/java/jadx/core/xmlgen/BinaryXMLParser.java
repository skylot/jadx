package jadx.core.xmlgen;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeInfo;
import jadx.api.ICodeWriter;
import jadx.api.ResourcesLoader;
import jadx.core.dex.info.ConstStorage;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.StringUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.xmlgen.entry.ValuesParser;

/*
 * TODO:
 * Don't die when error occurs
 * Check error cases, maybe checked const values are not always the same
 * Better error messages
 * What to do, when Binary XML Manifest is > size(int)?
 * Check for missing chunk size types
 * Implement missing data types
 * Use line numbers to recreate EXACT AndroidManifest
 * Check Element chunk size
 */

public class BinaryXMLParser extends CommonBinaryParser {
	private static final Logger LOG = LoggerFactory.getLogger(BinaryXMLParser.class);

	private static final boolean ATTR_NEW_LINE = false;

	private final Map<Integer, String> resNames;
	private Map<String, String> nsMap;
	private Set<String> nsMapGenerated;
	private final Map<String, String> tagAttrDeobfNames = new HashMap<>();

	private ICodeWriter writer;
	private String[] strings;
	private String currentTag = "ERROR";
	private boolean firstElement;
	private ValuesParser valuesParser;
	private boolean isLastEnd = true;
	private boolean isOneLine = true;
	private int namespaceDepth = 0;
	private @Nullable int[] resourceIds;

	private final RootNode rootNode;
	private String appPackageName;

	private Map<String, ClassNode> classNameCache;

	public BinaryXMLParser(RootNode rootNode) {
		this.rootNode = rootNode;
		try {
			ConstStorage constStorage = rootNode.getConstValues();
			resNames = constStorage.getResourcesNames();
		} catch (Exception e) {
			throw new JadxRuntimeException("BinaryXMLParser init error", e);
		}
	}

	public synchronized ICodeInfo parse(InputStream inputStream) throws IOException {
		is = new ParserStream(inputStream);
		if (!isBinaryXml()) {
			return ResourcesLoader.loadToCodeWriter(inputStream);
		}
		nsMapGenerated = new HashSet<>();
		nsMap = new HashMap<>();
		writer = rootNode.makeCodeWriter();
		writer.add("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		firstElement = true;
		decode();
		nsMap = null;
		ICodeInfo codeInfo = writer.finish();
		this.classNameCache = null; // reset class name cache
		return codeInfo;
	}

	private boolean isBinaryXml() throws IOException {
		is.mark(4);
		int v = is.readInt16(); // version
		int h = is.readInt16(); // header size
		// Some APK Manifest.xml the version is 0
		if (h == 0x0008) {
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
		int headerSize = is.readInt16();
		if (headerSize > 0x10) {
			LOG.warn("Invalid namespace header");
		} else if (headerSize < 0x10) {
			die("NAMESPACE header is not 0x10 big");
		}
		int size = is.readInt32();
		if (size > 0x18) {
			LOG.warn("Invalid namespace size");
		} else if (size < 0x18) {
			die("NAMESPACE header chunk is not 0x18 big");
		}

		int beginLineNumber = is.readInt32();
		int comment = is.readInt32();
		int beginPrefix = is.readInt32();
		int beginURI = is.readInt32();
		is.skip(headerSize - 0x10);

		String nsKey = getString(beginURI);
		String nsValue = getString(beginPrefix);
		if (StringUtils.notBlank(nsKey) && !nsMap.containsValue(nsValue)) {
			nsMap.putIfAbsent(nsKey, nsValue);
		}
		namespaceDepth++;
	}

	private void parseNameSpaceEnd() throws IOException {
		int headerSize = is.readInt16();
		if (headerSize > 0x10) {
			LOG.warn("Invalid namespace end");
		} else if (headerSize < 0x10) {
			die("NAMESPACE end is not 0x10 big");
		}
		int dataSize = is.readInt32();
		if (dataSize > 0x18) {
			LOG.warn("Invalid namespace size");
		} else if (dataSize < 0x18) {
			die("NAMESPACE header chunk is not 0x18 big");
		}
		int endLineNumber = is.readInt32();
		int comment = is.readInt32();
		int endPrefix = is.readInt32();
		int endURI = is.readInt32();
		is.skip(headerSize - 0x10);
		namespaceDepth--;

		String nsKey = getString(endURI);
		String nsValue = getString(endPrefix);
		if (StringUtils.notBlank(nsKey) && !nsMap.containsValue(nsValue)) {
			nsMap.putIfAbsent(nsKey, nsValue);
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
			writer.add('>');
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
			writer.add('>');
		}
		isOneLine = true;
		isLastEnd = false;
		currentTag = deobfClassName(getString(startNSName));
		currentTag = getValidTagAttributeName(currentTag);
		writer.startLine('<').add(currentTag);
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
				String nsValue = getValidTagAttributeName(entry.getValue());
				writer.add(" xmlns");
				if (nsValue != null && !nsValue.trim().isEmpty()) {
					writer.add(':');
					writer.add(nsValue);
				}
				writer.add("=\"").add(StringUtils.escapeXML(entry.getKey())).add('"');
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
		is.skip(3);
		int attrValDataType = is.readInt8();
		int attrValData = is.readInt32();

		if (newLine) {
			writer.startLine().addIndent();
		} else {
			writer.add(' ');
		}
		String shortNsName = null;
		if (attributeNS != -1) {
			shortNsName = getAttributeNS(attributeNS);
			writer.add(shortNsName).add(':');
		}
		String attrName = getValidTagAttributeName(getAttributeName(attributeName));
		writer.add(attrName).add("=\"");
		String decodedAttr = ManifestAttributes.getInstance().decode(attrName, attrValData);
		if (decodedAttr != null) {
			memorizePackageName(attrName, decodedAttr);
			if (isDeobfCandidateAttr(shortNsName, attrName)) {
				decodedAttr = deobfClassName(decodedAttr);
			}
			attachClassNode(writer, attrName, decodedAttr);
			writer.add(StringUtils.escapeXML(decodedAttr));
		} else {
			decodeAttribute(attributeNS, attrValDataType, attrValData,
					shortNsName, attrName);
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
		String attrName;
		if (ANDROID_NS_URL.equals(attrUrl)) {
			attrName = ANDROID_NS_VALUE;
			nsMap.put(ANDROID_NS_URL, attrName);
		} else {
			for (int i = 1;; i++) {
				attrName = "ns" + i;
				if (!nsMapGenerated.contains(attrName) && !nsMap.containsValue(attrName)) {
					nsMapGenerated.add(attrName);
					// do not add generated value to nsMap
					// because attrUrl might be used in a neighbor element, but never defined
					break;
				}
			}
		}
		writer.add("xmlns:").add(attrName).add("=\"").add(attrUrl).add("\" ");
		return attrName;
	}

	private String getAttributeName(int id) {
		// As the outcome of https://github.com/skylot/jadx/issues/1208
		// Android seems to favor entries from AndroidResMap and only if
		// there is no entry uses the values form the XML string pool
		if (resourceIds != null && 0 <= id && id < resourceIds.length) {
			int resId = resourceIds[id];
			String str = ValuesParser.getAndroidResMap().get(resId);
			if (str != null) {
				// cut type before /
				int typeEnd = str.indexOf('/');
				if (typeEnd != -1) {
					return str.substring(typeEnd + 1);
				}
				return str;
			}
		}

		String str = getString(id);
		if (str == null || str.isEmpty()) {
			return "NOT_FOUND_0x" + Integer.toHexString(id);
		}
		return str;
	}

	private String getString(int strId) {
		if (0 <= strId && strId < strings.length) {
			return strings[strId];
		}
		return "NOT_FOUND_STR_0x" + Integer.toHexString(strId);
	}

	private void decodeAttribute(int attributeNS, int attrValDataType, int attrValData,
			String shortNsName, String attrName) {
		if (attrValDataType == TYPE_REFERENCE) {
			// reference custom processing
			String resName = resNames.get(attrValData);
			if (resName != null) {
				writer.add('@');
				if (resName.startsWith("id/")) {
					writer.add('+');
				}
				writer.add(resName);
			} else {
				String androidResName = ValuesParser.getAndroidResMap().get(attrValData);
				if (androidResName != null) {
					writer.add("@android:").add(androidResName);
				} else if (attrValData == 0) {
					writer.add("@null");
				} else {
					writer.add("0x").add(Integer.toHexString(attrValData));
				}
			}
		} else {
			String str = valuesParser.decodeValue(attrValDataType, attrValData);
			memorizePackageName(attrName, str);
			if (isDeobfCandidateAttr(shortNsName, attrName)) {
				str = deobfClassName(str);
			}
			attachClassNode(writer, attrName, str);
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
		String elemName = deobfClassName(getString(elementNameId));
		elemName = getValidTagAttributeName(elemName);
		if (currentTag.equals(elemName) && isOneLine && !isLastEnd) {
			writer.add("/>");
		} else {
			writer.startLine("</");
			writer.attachSourceLine(endLineNumber);
			// if (elementNS != -1) {
			// writer.add(getString(elementNS)).add(':');
			// }
			writer.add(elemName).add('>');
		}
		isLastEnd = true;
		if (writer.getIndent() != 0) {
			writer.decIndent();
		}
	}

	private String getValidTagAttributeName(String originalName) {
		if (XMLChar.isValidName(originalName)) {
			return originalName;
		}
		if (tagAttrDeobfNames.containsKey(originalName)) {
			return tagAttrDeobfNames.get(originalName);
		}
		String generated;
		do {
			generated = generateTagAttrName();
		} while (tagAttrDeobfNames.containsValue(generated));
		tagAttrDeobfNames.put(originalName, generated);
		return generated;
	}

	private static String generateTagAttrName() {
		final int length = 6;
		Random r = new Random();
		StringBuilder sb = new StringBuilder();
		for (int i = 1; i <= length; i++) {
			sb.append((char) (r.nextInt(26) + 'a'));
		}
		return sb.toString();
	}

	private void attachClassNode(ICodeWriter writer, String attrName, String clsName) {
		if (!writer.isMetadataSupported()) {
			return;
		}
		if (clsName == null || !attrName.equals("name")) {
			return;
		}
		String clsFullName;
		if (clsName.startsWith(".")) {
			clsFullName = appPackageName + clsName;
		} else {
			clsFullName = clsName;
		}
		if (classNameCache == null) {
			classNameCache = rootNode.buildFullAliasClassCache();
		}
		ClassNode classNode = classNameCache.get(clsFullName);
		if (classNode != null) {
			writer.attachAnnotation(classNode);
		}
	}

	private String deobfClassName(String className) {
		String newName = XmlDeobf.deobfClassName(rootNode, className, appPackageName);
		if (newName != null) {
			return newName;
		}
		return className;
	}

	private boolean isDeobfCandidateAttr(String shortNsName, String attrName) {
		String fullName;
		if (shortNsName != null) {
			fullName = shortNsName + ':' + attrName;
		} else {
			return false;
		}
		return "android:name".equals(fullName);
	}

	private void memorizePackageName(String attrName, String attrValue) {
		if ("manifest".equals(currentTag) && "package".equals(attrName)) {
			appPackageName = attrValue;
		}
	}
}
