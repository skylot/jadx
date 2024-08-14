package jadx.tests.integration.inline;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestGetterInlineNegative extends SmaliTest {

	// @formatter:off
	/*
		public class TestGetterInlineNegative {
			public static final String field = "some string";

			public static synthetic String getter() {
				return field;
			}

			public void test() {
				getter(); // inline will produce 'field;' and fail to compile with 'not a statement' error
			}

			public String test2() {
				return getter();
			}
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				.doesNotContain(indent() + "field;")
				.containsOne("return field;");
	}
}
