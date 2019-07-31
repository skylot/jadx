package jadx.tests.functional;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.diffblue.deeptestutils.Reflector;

import jadx.api.JadxArgs;
import jadx.core.utils.StringUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class StringUtilsTest {

	private StringUtils stringUtils;

	@Test
	@SuppressWarnings("AvoidEscapedUnicodeCharacters")
	public void testStringUnescape() {
		JadxArgs args = new JadxArgs();
		args.setEscapeUnicode(true);
		stringUtils = new StringUtils(args);

		checkStringUnescape("", "");
		checkStringUnescape("'", "'");
		checkStringUnescape("a", "a");
		checkStringUnescape("\n", "\\n");
		checkStringUnescape("\t", "\\t");
		checkStringUnescape("\r", "\\r");
		checkStringUnescape("\b", "\\b");
		checkStringUnescape("\f", "\\f");
		checkStringUnescape("\\", "\\\\");
		checkStringUnescape("\"", "\\\"");
		checkStringUnescape("\u1234", "\\u1234");
	}

	private void checkStringUnescape(String input, String result) {
		assertThat(stringUtils.unescapeString(input), is('"' + result + '"'));
	}

	@Test
	public void testCharUnescape() {
		stringUtils = new StringUtils(new JadxArgs());

		checkCharUnescape('a', "a");
		checkCharUnescape(' ', " ");
		checkCharUnescape('\n', "\\n");
		checkCharUnescape('\'', "\\\'");
		checkCharUnescape('\0', "\\u0000");
	}

	private void checkCharUnescape(char input, String result) {
		assertThat(stringUtils.unescapeChar(input), is('\'' + result + '\''));
	}

	@Test
	public void testEscape() {
		Assert.assertEquals("_", StringUtils.escape(","));
		Assert.assertEquals("A", StringUtils.escape("["));
		Assert.assertEquals("", StringUtils.escape("*"));
		Assert.assertEquals("foo", StringUtils.escape("foo"));
	}

	@Test
	public void testEscapeXML() {
		Assert.assertEquals("2", StringUtils.escapeXML("2"));
		Assert.assertEquals("&apos;", StringUtils.escapeXML("\'"));
		Assert.assertEquals("&lt;&amp;", StringUtils.escapeXML("<&"));
		Assert.assertEquals("\\0", StringUtils.escapeXML("\u0000"));
		Assert.assertEquals("&quot;", StringUtils.escapeXML("\""));
		Assert.assertEquals("\\\\", StringUtils.escapeXML("\\"));
		Assert.assertEquals("&gt;", StringUtils.escapeXML(">"));
		Assert.assertEquals("&amp;", StringUtils.escapeXML("&"));
	}

	@Test
	public void testEscapeResValue() {
		Assert.assertEquals("(", StringUtils.escapeResValue("("));
		Assert.assertEquals("&amp;", StringUtils.escapeResValue("&"));
	}

	@Test
	public void testEscapeResStrValue() {
		Assert.assertEquals("/", StringUtils.escapeResStrValue("/"));
		Assert.assertEquals("\\\'", StringUtils.escapeResStrValue("\'"));
		Assert.assertEquals("\\'", StringUtils.escapeResStrValue("\'"));
		Assert.assertEquals("2", StringUtils.escapeResStrValue("2"));
		Assert.assertEquals("\\\"", StringUtils.escapeResStrValue("\""));
	}

	@Test
	public void testEscapeWhiteSpaceChar1()
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

		final char c = '\r';
		final Class<?> stringUtils = Reflector.forName("jadx.core.utils.StringUtils");
		final Method escapeWhiteSpaceChar =
				stringUtils.getDeclaredMethod("escapeWhiteSpaceChar", Reflector.forName("char"));
		escapeWhiteSpaceChar.setAccessible(true);

		Assert.assertEquals("\\r", escapeWhiteSpaceChar.invoke(null, c));
	}

	@Test
	public void testEscapeWhiteSpaceChar2()
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

		final char c = '\b';
		final Class<?> stringUtils = Reflector.forName("jadx.core.utils.StringUtils");
		final Method escapeWhiteSpaceChar =
				stringUtils.getDeclaredMethod("escapeWhiteSpaceChar", Reflector.forName("char"));
		escapeWhiteSpaceChar.setAccessible(true);

		Assert.assertEquals("\\b", escapeWhiteSpaceChar.invoke(null, c));
	}

	@Test
	public void testEscapeWhiteSpaceChar3()
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

		final char c = '\t';
		final Class<?> stringUtils = Reflector.forName("jadx.core.utils.StringUtils");
		final Method escapeWhiteSpaceChar =
				stringUtils.getDeclaredMethod("escapeWhiteSpaceChar", Reflector.forName("char"));
		escapeWhiteSpaceChar.setAccessible(true);

		Assert.assertEquals("\\t", escapeWhiteSpaceChar.invoke(null, c));
	}

	@Test
	public void testEscapeWhiteSpaceChar4()
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

		final char c = '\n';
		final Class<?> stringUtils = Reflector.forName("jadx.core.utils.StringUtils");
		final Method escapeWhiteSpaceChar =
				stringUtils.getDeclaredMethod("escapeWhiteSpaceChar", Reflector.forName("char"));
		escapeWhiteSpaceChar.setAccessible(true);

		Assert.assertEquals("\\n", escapeWhiteSpaceChar.invoke(null, c));
	}

	@Test
	public void testEscapeWhiteSpaceChar5()
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

		final char c = '\f';
		final Class<?> stringUtils = Reflector.forName("jadx.core.utils.StringUtils");
		final Method escapeWhiteSpaceChar =
				stringUtils.getDeclaredMethod("escapeWhiteSpaceChar", Reflector.forName("char"));
		escapeWhiteSpaceChar.setAccessible(true);

		Assert.assertEquals("\\f", escapeWhiteSpaceChar.invoke(null, c));
	}

	@Test
	public void testNotEmpty() {
		Assert.assertTrue(StringUtils.notEmpty("foobar"));
		Assert.assertFalse(StringUtils.notEmpty(null));
	}

	@Test
	public void testIsEmpty() {
		Assert.assertFalse(StringUtils.isEmpty("3"));
		Assert.assertTrue(StringUtils.isEmpty(null));
	}

	@Test
	public void testCountMatches() {
		Assert.assertEquals(0, StringUtils.countMatches("foobar", ""));
		Assert.assertEquals(0, StringUtils.countMatches("", "foobar"));
		Assert.assertEquals(1, StringUtils.countMatches("foobarbaz", "baz"));
	}
}
