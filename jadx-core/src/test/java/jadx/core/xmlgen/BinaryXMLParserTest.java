package jadx.core.xmlgen;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jadx.api.JadxArgs;
import jadx.core.dex.nodes.RootNode;

import static jadx.core.xmlgen.ParserConstants.ANDROID_NS_URL;
import static jadx.core.xmlgen.ParserConstants.RES_STRING_POOL_TYPE;
import static jadx.core.xmlgen.ParserConstants.RES_XML_END_ELEMENT_TYPE;
import static jadx.core.xmlgen.ParserConstants.RES_XML_END_NAMESPACE_TYPE;
import static jadx.core.xmlgen.ParserConstants.RES_XML_RESOURCE_MAP_TYPE;
import static jadx.core.xmlgen.ParserConstants.RES_XML_START_ELEMENT_TYPE;
import static jadx.core.xmlgen.ParserConstants.RES_XML_START_NAMESPACE_TYPE;
import static jadx.core.xmlgen.ParserConstants.RES_XML_TYPE;
import static jadx.core.xmlgen.ParserConstants.TYPE_INT_DEC;
import static jadx.core.xmlgen.ParserConstants.TYPE_STRING;
import static org.assertj.core.api.Assertions.assertThat;

class BinaryXMLParserTest {
	// string pool: attribute names with resource ids must come first
	private static final int STR_NAME = 0; // android:name
	private static final int STR_VERSION_CODE = 1; // android:versionCode
	private static final int STR_MANIFEST = 2;
	private static final int STR_PACKAGE = 3;
	private static final int STR_USES_PERMISSION = 4;
	private static final int STR_PKG_VALUE = 5;
	private static final int STR_PERMISSION_VALUE = 6;
	private static final int STR_ANDROID = 7;
	private static final int STR_ANDROID_URL = 8;

	private static final List<String> STRINGS = List.of(
			"name", "versionCode", "manifest", "package", "uses-permission",
			"com.example", "android.permission.INTERNET", "android", ANDROID_NS_URL);

	private static final int[] RES_IDS = { 0x01010003, 0x0101021b };

	@Test
	void testStrippedAttributeNamespace() throws IOException {
		String xml = parse(buildManifest(true));
		assertThat(xml).isEqualTo("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
				+ "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
				+ "    android:versionCode=\"1\"\n"
				+ "    package=\"com.example\">\n"
				+ "    <uses-permission android:name=\"android.permission.INTERNET\"/>\n"
				+ "</manifest>");
	}

	@Test
	void testStrippedAttributeNamespaceWithoutDeclaration() throws Exception {
		String xml = parse(buildManifest(false));
		// check that 'android' prefix is declared before use
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		Element manifest = doc.getDocumentElement();
		assertThat(manifest.getAttributeNS(ANDROID_NS_URL, "versionCode")).isEqualTo("1");
		assertThat(manifest.getAttribute("package")).isEqualTo("com.example");
		Element permission = (Element) manifest.getElementsByTagName("uses-permission").item(0);
		assertThat(permission.getAttributeNS(ANDROID_NS_URL, "name")).isEqualTo("android.permission.INTERNET");
	}

	private static String parse(byte[] data) throws IOException {
		JadxArgs args = new JadxArgs();
		args.setCodeNewLineStr("\n");
		BinaryXMLParser parser = new BinaryXMLParser(new RootNode(args));
		return parser.parse(new ByteArrayInputStream(data)).getCodeStr();
	}

	/**
	 * Manifest as written by obfuscators which strip namespace from attributes (namespace index is -1)
	 */
	private static byte[] buildManifest(boolean withNamespace) throws IOException {
		ByteArrayOutputStream body = new ByteArrayOutputStream();
		body.write(stringPool());
		body.write(resourceMap());
		if (withNamespace) {
			body.write(namespace(RES_XML_START_NAMESPACE_TYPE));
		}
		body.write(startElement(STR_MANIFEST,
				attr(STR_VERSION_CODE, -1, TYPE_INT_DEC, 1),
				attr(STR_PACKAGE, STR_PKG_VALUE, TYPE_STRING, STR_PKG_VALUE)));
		body.write(startElement(STR_USES_PERMISSION,
				attr(STR_NAME, STR_PERMISSION_VALUE, TYPE_STRING, STR_PERMISSION_VALUE)));
		body.write(endElement(STR_USES_PERMISSION));
		body.write(endElement(STR_MANIFEST));
		if (withNamespace) {
			body.write(namespace(RES_XML_END_NAMESPACE_TYPE));
		}
		byte[] bodyBytes = body.toByteArray();
		return chunk(RES_XML_TYPE, 8, bodyBytes.length + 8).put(bodyBytes).array();
	}

	private static byte[] stringPool() {
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		int[] offsets = new int[STRINGS.size()];
		for (int i = 0; i < STRINGS.size(); i++) {
			offsets[i] = data.size();
			byte[] bytes = STRINGS.get(i).getBytes(StandardCharsets.UTF_8);
			data.write(bytes.length); // chars count
			data.write(bytes.length); // bytes count
			data.writeBytes(bytes);
			data.write(0);
		}
		while (data.size() % 4 != 0) {
			data.write(0);
		}
		int stringsStart = 0x1c + offsets.length * 4;
		ByteBuffer buf = chunk(RES_STRING_POOL_TYPE, 0x1c, stringsStart + data.size());
		buf.putInt(STRINGS.size());
		buf.putInt(0); // styles count
		buf.putInt(ParserConstants.UTF8_FLAG);
		buf.putInt(stringsStart);
		buf.putInt(0); // styles start
		for (int offset : offsets) {
			buf.putInt(offset);
		}
		return buf.put(data.toByteArray()).array();
	}

	private static byte[] resourceMap() {
		ByteBuffer buf = chunk(RES_XML_RESOURCE_MAP_TYPE, 8, 8 + RES_IDS.length * 4);
		for (int id : RES_IDS) {
			buf.putInt(id);
		}
		return buf.array();
	}

	private static byte[] namespace(int type) {
		ByteBuffer buf = chunk(type, 0x10, 0x18);
		buf.putInt(1); // line
		buf.putInt(-1); // comment
		buf.putInt(STR_ANDROID);
		buf.putInt(STR_ANDROID_URL);
		return buf.array();
	}

	private static byte[] startElement(int name, byte[]... attrs) {
		ByteBuffer buf = chunk(RES_XML_START_ELEMENT_TYPE, 0x10, 0x24 + attrs.length * 0x14);
		buf.putInt(1); // line
		buf.putInt(-1); // comment
		buf.putInt(-1); // namespace
		buf.putInt(name);
		buf.putShort((short) 0x14); // attributes start
		buf.putShort((short) 0x14); // attribute size
		buf.putShort((short) attrs.length);
		buf.putShort((short) 0); // id index
		buf.putShort((short) 0); // class index
		buf.putShort((short) 0); // style index
		for (byte[] attr : attrs) {
			buf.put(attr);
		}
		return buf.array();
	}

	private static byte[] attr(int name, int rawValue, int dataType, int data) {
		ByteBuffer buf = ByteBuffer.allocate(0x14).order(ByteOrder.LITTLE_ENDIAN);
		buf.putInt(-1); // namespace removed
		buf.putInt(name);
		buf.putInt(rawValue);
		buf.putShort((short) 8); // value size
		buf.put((byte) 0);
		buf.put((byte) dataType);
		buf.putInt(data);
		return buf.array();
	}

	private static byte[] endElement(int name) {
		ByteBuffer buf = chunk(RES_XML_END_ELEMENT_TYPE, 0x10, 0x18);
		buf.putInt(1); // line
		buf.putInt(-1); // comment
		buf.putInt(-1); // namespace
		buf.putInt(name);
		return buf.array();
	}

	private static ByteBuffer chunk(int type, int headerSize, int size) {
		ByteBuffer buf = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
		buf.putShort((short) type);
		buf.putShort((short) headerSize);
		buf.putInt(size);
		return buf;
	}
}
