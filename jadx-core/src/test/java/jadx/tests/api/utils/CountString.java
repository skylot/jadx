package jadx.tests.api.utils;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class CountString extends TypeSafeMatcher<String> {

	private final int count;
	private final String substring;

	public CountString(int count, String substring) {
		this.count = count;
		this.substring = substring;
	}

	@Override
	protected boolean matchesSafely(String item) {
		return this.count == count(item);
	}

	@Override
	public void describeMismatchSafely(String item, Description mismatchDescription) {
		mismatchDescription.appendText("found ").appendValue(count(item));
	}

	@Override
	public void describeTo(Description description) {
		description.appendText("containing <" + count + "> occurrence of ").appendValue(this.substring);
	}

	private int count(String string) {
		return TestUtils.count(string, this.substring);
	}
}
