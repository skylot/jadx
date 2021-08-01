package jadx.tests.integration.java8;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestLambdaConstructor extends IntegrationTest {

	public static class TestCls {

		public Supplier<Exception> test() {
			return RuntimeException::new;
		}

		public void check() throws Exception {
			assertThat(test().get()).isInstanceOf(RuntimeException.class);
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return RuntimeException::new;");
	}

	@Test
	public void testFallback() {
		setFallback();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("r0 = java.lang.RuntimeException::new")
				.containsOne("return r0");
	}
}
