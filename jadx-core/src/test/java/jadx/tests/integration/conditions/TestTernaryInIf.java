package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTernaryInIf extends IntegrationTest {

	public static class TestCls {
		public boolean test1(boolean a, boolean b, boolean c) {
			return a ? b : c;
		}

		public int test2(boolean a, boolean b, boolean c) {
			return (!a ? c : b) ? 1 : 2;
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("if")
				.doesNotContain("else")
				.containsOne("return a ? b : c;")
				.containsOneOf(
						"return (a ? b : c) ? 1 : 2;",
						"return (a ? !b : !c) ? 2 : 1;" // TODO: simplify this
				);
	}
}
