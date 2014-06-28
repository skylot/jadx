package jadx.tests.utils;

import org.hamcrest.Matcher;

public class JadxMatchers {

	public static Matcher<String> countString(int count, String substring) {
		return new CountString(count, substring);
	}

	public static Matcher<String> containsOne(String substring) {
		return new CountString(1, substring);
	}
}
