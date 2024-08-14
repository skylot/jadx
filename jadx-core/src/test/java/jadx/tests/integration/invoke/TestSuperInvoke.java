package jadx.tests.integration.invoke;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

public class TestSuperInvoke extends IntegrationTest {

	public class A {
		public int a() {
			return 1;
		}
	}

	public class B extends A {
		@Override
		public int a() {
			return super.a() + 2;
		}

		public int test() {
			return a();
		}
	}

	public void check() {
		assertThat(new B().test()).isEqualTo(3);
	}

	@Test
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestSuperInvoke.class))
				.code()
				.countString(2, "return super.a() + 2;");
	}
}
