package jadx.tests.integration.names;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

public class TestReservedNames extends SmaliTest {

	// @formatter:off
	/*
		public static class TestCls {

			public String do; // reserved name
			public String 0f; // invalid identifier

			public String try() {
				return this.do;
			}
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		ClassNode cls = getClassNodeFromSmaliWithPath("names", "TestReservedNames");
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("public String do;")));
	}
}
