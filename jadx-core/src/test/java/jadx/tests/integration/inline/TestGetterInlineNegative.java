package jadx.tests.integration.inline;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

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
		ClassNode cls = getClassNodeFromSmali();
		String code = cls.getCode().toString();

		assertThat(code, not(containsString(indent() + "field;")));
		assertThat(code, containsOne("return field;"));
	}
}
