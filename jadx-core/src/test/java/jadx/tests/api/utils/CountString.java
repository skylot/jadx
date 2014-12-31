package jadx.tests.api.utils;

import org.hamcrest.Description;
import org.hamcrest.core.SubstringMatcher;

public class CountString extends SubstringMatcher {

	private final int count;

	public CountString(int count, String substring) {
		super(substring);
		this.count = count;
	}

	@Override
	protected boolean evalSubstringOf(String string) {
		return this.count == count(string);
	}

	@Override
	protected String relationship() {
		return "containing <" + count + "> occurrence of";
	}

	@Override
	public void describeMismatchSafely(String item, Description mismatchDescription) {
		mismatchDescription.appendText("found ").appendValue(count(item));
	}

	private int count(String string) {
		return TestUtils.count(string, substring);
	}
}
