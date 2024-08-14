package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestTypeResolver6a extends IntegrationTest {

	public static class TestCls implements Runnable {
		public final Runnable runnable;

		public TestCls(boolean b) {
			this.runnable = b ? this : makeRunnable();
		}

		public Runnable makeRunnable() {
			return new Runnable() {
				@Override
				public void run() {
					// do nothing
				}
			};
		}

		@Override
		public void run() {
			// do nothing
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("this.runnable = b ? this : makeRunnable();");
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		getClassNode(TestCls.class);
	}
}
