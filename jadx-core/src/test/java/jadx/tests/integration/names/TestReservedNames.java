package jadx.tests.integration.names;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class TestReservedNames extends SmaliTest {
	/*
		public static class TestCls {

			public String do; // reserved name
			public String 0f; // invalid identifier

			public String try() {
				return this.do;
			}
		}
	*/

	@Test
	public void test() {
		ClassNode cls = getClassNodeFromSmaliWithPath("names", "TestReservedNames");
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("public String do;")));
	}
}
