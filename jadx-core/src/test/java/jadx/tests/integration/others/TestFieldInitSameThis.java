package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestFieldInitSameThis extends IntegrationTest {

	public static class TestCls {
		public final int value;

		public TestCls() {
			this.value = System.identityHashCode(this);
		}

		public TestCls(long ignored) {
			this.value = System.identityHashCode(this);
		}

		public void check() {
			TestCls first = new TestCls();
			TestCls second = new TestCls(0L);
			assertThat(first.value).isEqualTo(System.identityHashCode(first));
			assertThat(second.value).isEqualTo(System.identityHashCode(second));
		}
	}

	@Test
	public void test() {
		getArgs().setDebugInfo(false);
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("final int value = System.identityHashCode(this);");
	}
}
