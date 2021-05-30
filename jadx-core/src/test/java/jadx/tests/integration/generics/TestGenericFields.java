package jadx.tests.integration.generics;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestGenericFields extends IntegrationTest {

	public static class TestCls {

		public static class Summary {
			Value<Amount> price;
		}

		public static class Value<T> {
			T value;
		}

		public static class Amount {
			String cur;
			int val;
		}

		public String test(Summary summary) {
			Amount amount = summary.price.value;
			return amount.val + " " + amount.cur;
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("T t = ")
				.containsOne("Amount amount =");
	}
}
