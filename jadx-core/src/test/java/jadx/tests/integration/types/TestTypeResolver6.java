package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("this.obj = b ? this : makeObj();"));
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		getClassNode(TestCls.class);
	}
}
