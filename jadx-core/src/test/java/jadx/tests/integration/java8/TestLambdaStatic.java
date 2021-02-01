package jadx.tests.integration.java8;

import java.util.concurrent.Callable;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestLambdaStatic extends IntegrationTest {

	public static class TestCls {
		public Callable<String> test1() {
			return () -> "test";
		}

		public Callable<String> test2(String str) {
			return () -> str;
		}

		public Function<String, Integer> test3(String a) {
			return (b) -> Integer.parseInt(a) - Integer.parseInt(b);
		}

		public Function<String, Integer> test4() {
			return Integer::parseInt;
		}

		@SuppressWarnings("Convert2MethodRef")
		public Function<String, Integer> test4a() {
			return s -> Integer.parseInt(s);
		}

		public Function<String, Integer> test5() {
			String str = Integer.toString(3);
			return (s) -> Integer.parseInt(str) - Integer.parseInt(s);
		}

		public void check() throws Exception {
			assertThat(test1().call()).isEqualTo("test");
			assertThat(test2("a").call()).isEqualTo("a");
			assertThat(test3("3").apply("1")).isEqualTo(2);
			assertThat(test4().apply("5")).isEqualTo(5);
			assertThat(test4a().apply("7")).isEqualTo(7);
			assertThat(test5().apply("1")).isEqualTo(2);
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("lambda$")
				.doesNotContain("renamed")
				.containsLines(2,
						"return () -> {",
						indent() + "return \"test\";",
						"};")
				.containsLines(2,
						"return () -> {",
						indent() + "return str;",
						"};")
				.containsOne("return Integer::parseInt;");
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
