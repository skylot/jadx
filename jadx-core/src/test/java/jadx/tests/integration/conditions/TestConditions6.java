package jadx.tests.integration.conditions;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestConditions6 extends IntegrationTest {

	public static class TestCls {
		public boolean test(List<String> l1, List<String> l2) {
			if (l2.size() > 0) {
				l1.clear();
			}
			return l1.size() == 0;
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("return l1.size() == 0;")
				.doesNotContain("else");
	}
}
