package jadx.tests.integration.java8;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestLambdaInstance extends IntegrationTest {

	@SuppressWarnings("Convert2MethodRef")
	public static class TestCls {

		public Function<String, Integer> test() {
			return str -> this.call(str);
		}

		public Function<String, Integer> testMthRef() {
			return this::call;
		}

		public Integer call(String str) {
			return Integer.parseInt(str);
		}

		public Function<Integer, String> test2() {
			return num -> num.toString();
		}

		public Function<Integer, String> testMthRef2() {
			return Object::toString;
		}

		public void check() throws Exception {
			assertThat(test().apply("11")).isEqualTo(11);
			assertThat(testMthRef().apply("7")).isEqualTo(7);

			assertThat(test2().apply(15)).isEqualTo("15");
			assertThat(testMthRef2().apply(13)).isEqualTo("13");
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("lambda$")
				.doesNotContain("renamed")
				.containsLines(2,
						"return str -> {",
						indent() + "return call(str);",
						"};")
				// .containsOne("return Object::toString;") // TODO
				.containsOne("return this::call;");
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		getClassNode(TestCls.class);
	}

	@Test
	public void testFallback() {
		setFallback();
		getClassNode(TestCls.class);
	}
}
