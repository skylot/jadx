package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("this.runnable = b ? this : makeRunnable();"));
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		getClassNode(TestCls.class);
	}
}
