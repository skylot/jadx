package jadx.tests.integration.arrays;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

public class TestArrays2 extends IntegrationTest {
	public static class TestCls {

		private static Object test4(int type) {
			if (type == 1) {
				return new int[]{1, 2};
			} else if (type == 2) {
				return new float[]{1, 2};
			} else if (type == 3) {
				return new short[]{1, 2};
			} else if (type == 4) {
				return new byte[]{1, 2};
			} else {
				return null;
			}
		}

		public void check() {
			assertThat(test4(4), instanceOf(byte[].class));
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("new int[]{1, 2}"));
	}
}
