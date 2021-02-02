package jadx.tests.integration.java8;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestLambdaInArray extends IntegrationTest {

	public static class TestCls {

		public List<Function<String, Integer>> test() {
			return Arrays.asList(this::call1, this::call2);
		}

		private Integer call1(String s) {
			return null;
		}

		private Integer call2(String s) {
			return null;
		}

		public void check() throws Exception {
			assertThat(test()).hasSize(2);
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return Arrays.asList(this::call1, this::call2);");
	}
}
