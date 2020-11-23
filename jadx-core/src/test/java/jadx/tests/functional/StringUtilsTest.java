package jadx.tests.functional;

import org.junit.jupiter.api.Test;

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
		checkCharUnescape('\'', "\\'");

		assertThat(stringUtils.unescapeChar('\0'), is("0"));
	}

	private void checkCharUnescape(char input, String result) {
		assertThat(stringUtils.unescapeChar(input), is('\'' + result + '\''));
	}

	@Test
	public void testResStrValueEscape() {
		checkResStrValueEscape("line\nnew line", "line\\nnew line");
		checkResStrValueEscape("can't", "can\\'t");
		checkResStrValueEscape("quote\"end", "quote\\\"end");
	}

	private void checkResStrValueEscape(String input, String result) {
		assertThat(StringUtils.escapeResStrValue(input), is(result));
	}
}
