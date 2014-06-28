package jadx.tests.utils;

import org.hamcrest.core.SubstringMatcher;

public class CountString extends SubstringMatcher {

	private final int count;

	public CountString(int count, String substring) {
		super(substring);
		this.count = count;
	}

	@Override
	protected boolean evalSubstringOf(String string) {
		return this.count == countStr(string, substring);
	}

	@Override
	protected String relationship() {
		return "containing " + count + " occurrence of";
	}

	private static int countStr(String string, String substring) {
		int cnt = 0;
		int idx = 0;
		while ((idx = string.indexOf(substring, idx)) != -1) {
			idx++;
			cnt++;
		}
		return cnt;
	}
}
