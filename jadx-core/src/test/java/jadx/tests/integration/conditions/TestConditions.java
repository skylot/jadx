package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestConditions extends IntegrationTest {

	public static class TestCls {
		public boolean test(boolean a, boolean b, boolean c) {
			return (a && b) || c;
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("(!a || !b) && !c")
				.contains("return (a && b) || c;");
	}
}
