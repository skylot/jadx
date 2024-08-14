package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestTypeResolver6 extends IntegrationTest {

	public static class TestCls {
		public final Object obj;

		public TestCls(boolean b) {
			this.obj = b ? this : makeObj();
		}

		public Object makeObj() {
			return new Object();
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("this.obj = b ? this : makeObj();");
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		getClassNode(TestCls.class);
	}
}
