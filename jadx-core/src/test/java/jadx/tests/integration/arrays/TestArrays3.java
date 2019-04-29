package jadx.tests.integration.arrays;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestArrays3 extends IntegrationTest {
	public static class TestCls {

		private Object test(byte[] bArr) {
			return new Object[] { bArr };
		}

		public void check() {
			byte[] inputArr = { 1, 2 };
			Object result = test(inputArr);
			assertThat(result, instanceOf(Object[].class));
			assertThat(((Object[]) result)[0], is(inputArr));
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("return new Object[]{bArr};"));
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("return new Object[]{bArr};"));
	}
}
