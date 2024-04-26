package jadx.plugins.input.aab.parsers;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.android.aapt.Resources.XmlAttribute;
import com.android.aapt.Resources.XmlElement;
import com.android.aapt.Resources.XmlNamespace;
import com.android.aapt.Resources.XmlNode;

import jadx.api.ICodeInfo;
import jadx.api.ICodeWriter;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.StringUtils;
import jadx.core.utils.android.AndroidResourcesMap;
import jadx.core.xmlgen.XMLChar;
import jadx.core.xmlgen.XmlDeobf;
import jadx.core.xmlgen.XmlGenUtils;

public class ResXmlProtoParser extends CommonProtoParser {
	private Map<String, String> nsMap;
	private final Map<String, String> tagAttrDeobfNames = new HashMap<>();

	private ICodeWriter writer;

	private final RootNode rootNode;
	private String currentTag;
	private String appPackageName;
	private final boolean isPrettyPrint;

	public ResXmlProtoParser(RootNode rootNode) {
		this.rootNode = rootNode;
		this.isPrettyPrint = !rootNode.getArgs().isSkipXmlPrettyPrint();
	}

	public synchronized ICodeInfo parse(InputStream inputStream) throws IOException {
		nsMap = new HashMap<>();
		writer = rootNode.makeCodeWriter();
		writer.add("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		decode(decodeProto(inputStream));
		nsMap = null;
		return writer.finish();
	}

	private void decode(XmlNode n) throws IOException {
		if (n.hasSource()) {
			writer.attachSourceLine(n.getSource().getLineNumber());
		}
		writer.add(StringUtils.escapeXML(n.getText().trim()));
		if (n.hasElement()) {
			decode(n.getElement());
		}
	}

	private void decode(XmlElement e) throws IOException {
		String tag = deobfClassName(e.getName());
		tag = getValidTagAttributeName(tag);
		currentTag = tag;
		writer.startLine('<').add(tag);

		decodeNamespaces(e);
		decodeAttributes(e);

		if (e.getChildCount() > 0) {
			writer.add('>');
			writer.incIndent();
			for (int i = 0; i < e.getChildCount(); i++) {
				Map<String, String> oldNsMap = new HashMap<>(nsMap);
				decode(e.getChild(i));
				nsMap = oldNsMap;
			}
			writer.decIndent();
			writer.startLine("</").add(tag).add('>');
		} else {
			writer.add(" />");
		}
	}

	private void decodeNamespaces(XmlElement e) {
		int nsCount = e.getNamespaceDeclarationCount();
		boolean newLine = nsCount != 1 && isPrettyPrint;
		if (nsCount > 0) {
			writer.add(' ');
		}
		for (int i = 0; i < nsCount; i++) {
			decodeNamespace(e.getNamespaceDeclaration(i), newLine, i == nsCount - 1);
		}
	}

	private void decodeNamespace(XmlNamespace n, boolean newLine, boolean isLastElement) {
		String prefix = n.getPrefix();
		String uri = n.getUri();
		nsMap.put(uri, prefix);
		writer.add("xmlns:").add(prefix).add("=\"").add(uri).add('"');
		if (isLastElement) {
			return;
		}
		if (newLine) {
			writer.startLine().addIndent();
		} else {
			writer.add(' ');
		}
	}

	private void decodeAttributes(XmlElement e) {
		int attrsCount = e.getAttributeCount();
		boolean newLine = attrsCount != 1 && isPrettyPrint;
		if (attrsCount > 0) {
			writer.add(' ');
			if (isPrettyPrint) {
				writer.startLine().addIndent();
			}
		}
		Set<String> attrCache = new HashSet<>();
		for (int i = 0; i < attrsCount; i++) {
			decodeAttribute(e.getAttribute(i), attrCache, newLine, i == attrsCount - 1);
		}
	}

	private void decodeAttribute(XmlAttribute a, Set<String> attrCache, boolean newLine, boolean isLastElement) {
		String name = getAttributeFullName(a);
		if (XmlDeobf.isDuplicatedAttr(name, attrCache)) {
			return;
		}
		String value = deobfClassName(getAttributeValue(a));
		writer.add(name).add("=\"").add(StringUtils.escapeXML(value)).add('\"');
		memorizePackageName(name, value);
		if (isLastElement) {
			return;
		}
		if (newLine) {
			writer.startLine().addIndent();
		} else {
			writer.add(' ');
		}
	}

	private String getAttributeFullName(XmlAttribute a) {
		String namespaceUri = a.getNamespaceUri();
		String namespace = null;
		if (!namespaceUri.isEmpty()) {
			namespace = nsMap.get(namespaceUri);
		}

		String attrName = a.getName();
		if (attrName.isEmpty()) {
			// some optimization tools clear the name because the Android platform doesn't need it
			int resId = a.getResourceId();
			String str = AndroidResourcesMap.getResName(resId);
			if (str != null) {
				namespace = nsMap.get(ANDROID_NS_URL);
				// cut type before /
				int typeEnd = str.indexOf('/');
				if (typeEnd != -1) {
					attrName = str.substring(typeEnd + 1);
				} else {
					attrName = str;
				}
			} else {
				attrName = "_unknown_";
			}
		}

		return namespace != null ? namespace + ":" + attrName : attrName;
	}

	private String getAttributeValue(XmlAttribute a) {
		if (!a.getValue().isEmpty()) {
			return a.getValue();
		}
		return parse(a.getCompiledItem());
	}

	private void memorizePackageName(String attrName, String attrValue) {
		if ("manifest".equals(currentTag) && "package".equals(attrName)) {
			appPackageName = attrValue;
		}
	}

	private String deobfClassName(String className) {
		String newName = XmlDeobf.deobfClassName(rootNode, className, appPackageName);
		if (newName != null) {
			return newName;
		}
		return className;
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

	private XmlNode decodeProto(InputStream inputStream) throws IOException {
		return XmlNode.parseFrom(XmlGenUtils.readData(inputStream));
	}
}
