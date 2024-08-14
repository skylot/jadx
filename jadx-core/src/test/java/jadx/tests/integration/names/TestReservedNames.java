package jadx.tests.integration.names;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

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
		assertThat(getClassNodeFromSmali())
				.code()
				.doesNotContain("public String do;");
	}
}
