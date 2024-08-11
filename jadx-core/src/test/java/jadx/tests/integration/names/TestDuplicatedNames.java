package jadx.tests.integration.names;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestDuplicatedNames extends SmaliTest {

	// @formatter:off
	/*
		public static class TestCls {

			public Object fieldName;
			public String fieldName;

			public Object run() {
				return this.fieldName;
			}

			public String run() {
				return this.fieldName;
			}
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		commonChecks();
	}

	@Test
	public void testWithDeobf() {
		enableDeobfuscation();
		commonChecks();
	}

	private void commonChecks() {
		assertThat(getClassNodeFromSmaliWithPath("names", "TestDuplicatedNames"))
				.code()
				.containsOne("Object fieldName;")
				.containsOne("String f0fieldName")
				.containsOne("this.fieldName")
				.containsOne("this.f0fieldName")
				.containsOne("public Object run() {")
				.containsOne("public String m0run() {");
	}
}
