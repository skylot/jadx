package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.api.ICodeWriter;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestElseIfCodeStyle extends IntegrationTest {

	@SuppressWarnings("unused")
	public static class TestCls {

		public void test(String str) {
			if ("a".equals(str)) {
				call(1);
			} else if ("b".equals(str)) {
				call(2);
			} else if ("c".equals(str)) {
				call(3);
			}
		}

		private void call(int i) {
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("!\"c\".equals(str)")
				.doesNotContain("{" + ICodeWriter.NL + indent(2) + "} else {"); // no empty `then` block
	}
}
