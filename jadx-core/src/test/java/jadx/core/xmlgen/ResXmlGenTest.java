package jadx.core.xmlgen;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;

import jadx.core.xmlgen.entry.RawNamedValue;
import jadx.core.xmlgen.entry.RawValue;
import jadx.core.xmlgen.entry.ResourceEntry;
import jadx.core.xmlgen.entry.ValuesParser;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResXmlGenTest {

	@Test
	void testSimpleAttr() {
		ResourceStorage resStorage = new ResourceStorage();
		ResourceEntry re = new ResourceEntry(2130903103, "jadx.gui.app", "attr", "size", "");
		re.setNamedValues(Lists.list(new RawNamedValue(16777216, new RawValue(16, 64))));
		resStorage.add(re);

		ValuesParser vp = new ValuesParser(null, resStorage.getResourcesNames());
		ResXmlGen resXmlGen = new ResXmlGen(resStorage, vp);
		List<ResContainer> files = resXmlGen.makeResourcesXml();

		assertEquals(1, files.size());
		assertEquals("res/values/attrs.xml", files.get(0).getName());
		assertEquals("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
				+ "<resources>\n"
				+ "    <attr name=\"size\" format=\"dimension\">\n"
				+ "    </attr>\n"
				+ "</resources>", adaptLineEndings(files.get(0).getText().toString()));
	}

	@Test
	void testAttrEnum() {
		ResourceStorage resStorage = new ResourceStorage();
		ResourceEntry re = new ResourceEntry(2130903103, "jadx.gui.app", "attr", "size", "");
		re.setNamedValues(
				Lists.list(new RawNamedValue(16777216, new RawValue(16, 65536)), new RawNamedValue(17039620, new RawValue(16, 1))));
		resStorage.add(re);

		ValuesParser vp = new ValuesParser(null, resStorage.getResourcesNames());
		ResXmlGen resXmlGen = new ResXmlGen(resStorage, vp);
		List<ResContainer> files = resXmlGen.makeResourcesXml();

		assertEquals(1, files.size());
		assertEquals("res/values/attrs.xml", files.get(0).getName());
		assertEquals("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
				+ "<resources>\n"
				+ "    <attr name=\"size\">\n"
				+ "        <enum name=\"android:string.aerr_wait\" value=\"1\" />\n"
				+ "    </attr>\n"
				+ "</resources>", adaptLineEndings(files.get(0).getText().toString()));
	}

	@Test
	void testAttrFlag() {
		ResourceStorage resStorage = new ResourceStorage();
		ResourceEntry re = new ResourceEntry(2130903103, "jadx.gui.app", "attr", "size", "");
		re.setNamedValues(
				Lists.list(new RawNamedValue(16777216, new RawValue(16, 131072)), new RawNamedValue(17039620, new RawValue(16, 1))));
		resStorage.add(re);

		ValuesParser vp = new ValuesParser(null, resStorage.getResourcesNames());
		ResXmlGen resXmlGen = new ResXmlGen(resStorage, vp);
		List<ResContainer> files = resXmlGen.makeResourcesXml();

		assertEquals(1, files.size());
		assertEquals("res/values/attrs.xml", files.get(0).getName());
		assertEquals("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
				+ "<resources>\n"
				+ "    <attr name=\"size\">\n"
				+ "        <flag name=\"android:string.aerr_wait\" value=\"1\" />\n"
				+ "    </attr>\n"
				+ "</resources>", adaptLineEndings(files.get(0).getText().toString()));
	}

	@Test
	void testAttrMin() {
		ResourceStorage resStorage = new ResourceStorage();
		ResourceEntry re = new ResourceEntry(2130903103, "jadx.gui.app", "attr", "size", "");
		re.setNamedValues(
				Lists.list(new RawNamedValue(16777216, new RawValue(16, 4)), new RawNamedValue(16777217, new RawValue(16, 1))));
		resStorage.add(re);

		ValuesParser vp = new ValuesParser(null, resStorage.getResourcesNames());
		ResXmlGen resXmlGen = new ResXmlGen(resStorage, vp);
		List<ResContainer> files = resXmlGen.makeResourcesXml();

		assertEquals(1, files.size());
		assertEquals("res/values/attrs.xml", files.get(0).getName());
		assertEquals("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
				+ "<resources>\n"
				+ "    <attr name=\"size\" format=\"integer\" min=\"1\">\n"
				+ "    </attr>\n"
				+ "</resources>", adaptLineEndings(files.get(0).getText().toString()));
	}

	@Test
	void testStyle() {
		ResourceStorage resStorage = new ResourceStorage();
		ResourceEntry re = new ResourceEntry(2130903103, "jadx.gui.app", "style", "JadxGui", "");
		re.setNamedValues(Lists.list(new RawNamedValue(16842836, new RawValue(1, 17170445))));
		resStorage.add(re);

		re = new ResourceEntry(2130903104, "jadx.gui.app", "style", "JadxGui.Dialog", "");
		re.setParentRef(2130903103);
		re.setNamedValues(new ArrayList<>());
		resStorage.add(re);
		ValuesParser vp = new ValuesParser(null, resStorage.getResourcesNames());
		ResXmlGen resXmlGen = new ResXmlGen(resStorage, vp);
		List<ResContainer> files = resXmlGen.makeResourcesXml();

		assertEquals(1, files.size());
		assertEquals("res/values/styles.xml", files.get(0).getName());
		assertEquals("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
				+ "<resources>\n"
				+ "    <style name=\"JadxGui\" parent=\"\">\n"
				+ "        <item name=\"android:windowBackground\">@android:color/transparent</item>\n"
				+ "    </style>\n"
				+ "    <style name=\"JadxGui.Dialog\" parent=\"@style/JadxGui\">\n"
				+ "    </style>\n"
				+ "</resources>", adaptLineEndings(files.get(0).getText().toString()));
	}

	@Test
	void testString() {
		ResourceStorage resStorage = new ResourceStorage();
		ResourceEntry re = new ResourceEntry(2130903103, "jadx.gui.app", "string", "app_name", "");
		re.setSimpleValue(new RawValue(3, 0));
		re.setNamedValues(Lists.list());
		resStorage.add(re);

		BinaryXMLStrings strings = new BinaryXMLStrings();
		strings.put(0, "Jadx Decompiler App");
		ValuesParser vp = new ValuesParser(strings, resStorage.getResourcesNames());
		ResXmlGen resXmlGen = new ResXmlGen(resStorage, vp);
		List<ResContainer> files = resXmlGen.makeResourcesXml();

		assertEquals(1, files.size());
		assertEquals("res/values/strings.xml", files.get(0).getName());
		assertEquals("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
				+ "<resources>\n"
				+ "    <string name=\"app_name\">Jadx Decompiler App</string>\n"
				+ "</resources>", adaptLineEndings(files.get(0).getText().toString()));
	}

	@Test
	void testStringFormattedFalse() {
		ResourceStorage resStorage = new ResourceStorage();
		ResourceEntry re = new ResourceEntry(2130903103, "jadx.gui.app", "string", "app_name", "");
		re.setSimpleValue(new RawValue(3, 0));
		re.setNamedValues(Lists.list());
		resStorage.add(re);

		BinaryXMLStrings strings = new BinaryXMLStrings();
		strings.put(0, "%s at %s");
		ValuesParser vp = new ValuesParser(strings, resStorage.getResourcesNames());
		ResXmlGen resXmlGen = new ResXmlGen(resStorage, vp);
		List<ResContainer> files = resXmlGen.makeResourcesXml();

		assertEquals(1, files.size());
		assertEquals("res/values/strings.xml", files.get(0).getName());
		assertEquals("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
				+ "<resources>\n"
				+ "    <string name=\"app_name\" formatted=\"false\">%s at %s</string>\n"
				+ "</resources>", adaptLineEndings(files.get(0).getText().toString()));
	}

	@Test
	void testArrayEscape() {
		ResourceStorage resStorage = new ResourceStorage();
		ResourceEntry re = new ResourceEntry(2130903103, "jadx.gui.app", "array", "single_quote_escape_sample", "");
		re.setNamedValues(
				Lists.list(new RawNamedValue(16777216, new RawValue(3, 0))));
		resStorage.add(re);

		BinaryXMLStrings strings = new BinaryXMLStrings();
		strings.put(0, "Let's go");
		ValuesParser vp = new ValuesParser(strings, resStorage.getResourcesNames());
		ResXmlGen resXmlGen = new ResXmlGen(resStorage, vp);
		List<ResContainer> files = resXmlGen.makeResourcesXml();

		assertEquals(1, files.size());
		assertEquals("res/values/arrays.xml", files.get(0).getName());
		assertEquals("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
				+ "<resources>\n"
				+ "    <array name=\"single_quote_escape_sample\">\n"
				+ "        <item>Let\\'s go</item>\n"
				+ "    </array>\n"
				+ "</resources>", adaptLineEndings(files.get(0).getText().toString()));
	}

	private static String adaptLineEndings(String input) {
		if (System.lineSeparator().equals("\n")) {
			return input; // no adaption necessary
		}
		return input.replaceAll(System.lineSeparator(), "\n");
	}
}
