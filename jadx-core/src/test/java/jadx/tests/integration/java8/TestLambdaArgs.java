package jadx.tests.integration.java8;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestLambdaArgs extends IntegrationTest {

	public static class TestCls {
		public void test1() {
			call1(a -> -a);
		}

		public void test2() {
			call2((a, b) -> a - b);
		}

		private void call1(Function<Integer, Integer> func) {
		}

		private void call2(BiFunction<Integer, Integer, Integer> func) {
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("call1(a ->")
				.containsOne("call2((a, b) ->");
	}
}
