package jadx.tests;

import jadx.core.utils.StringUtils;
import junit.framework.TestCase;

public class StringUtilsTest extends TestCase {

	public void testUnescape() {
		unescapeTest("\n", "\\n");
		unescapeTest("\t", "\\t");
		unescapeTest("\r", "\\r");
		unescapeTest("\b", "\\b");
		unescapeTest("\f", "\\f");
		unescapeTest("\\", "\\\\");
		unescapeTest("\"", "\\\"");
		unescapeTest("'", "'");

		unescapeTest("\u1234", "\\u1234");

		unescapeCharTest('\'', "'\\\''");
	}

	private void unescapeTest(String input, String expected) {
		assertEquals("\"" + expected + "\"", StringUtils.unescapeString(input));
	}

	private void unescapeCharTest(char input, String expected) {
		assertEquals(expected, StringUtils.unescapeChar(input));
	}

}
