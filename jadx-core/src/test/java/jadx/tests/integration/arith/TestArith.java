package jadx.tests.integration.arith;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestArith extends IntegrationTest {

	public static class TestCls {

		public static final int F = 7;

		public int test(int a) {
			a += 2;
			use(a);
			return a;
		}

		public int test2(int a) {
			a++;
			use(a);
			return a;
		}

		private static void use(int i) {
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code();
	}

	@Test
	@NotYetImplemented
	public void test2() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("a += 2;")
				.contains("a++;");
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code();
	}

	@Test
	@NotYetImplemented
	public void testNoDebug2() {
		noDebugInfo();
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("i += 2;")
				.contains("i++;");
	}
}
