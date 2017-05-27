package jadx.tests.integration.arrays;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

public class TestArrays3 extends IntegrationTest {
	public static class TestCls {

		private Object test(byte[] bArr) {
			return new Object[]{bArr};
		}

		public void check() {
			assertThat(test(new byte[]{1, 2}), instanceOf(Object[].class));
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("return new Object[]{bArr};"));
	}
}
