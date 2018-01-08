package jadx.tests.integration.generics;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestGenerics4 extends IntegrationTest {

	public static class TestCls {

		public static Class<?> method(int i) {
			Class<?>[] a = new Class<?>[0];
			return a[a.length - i];
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("Class<?>[] a ="));
		assertThat(code, not(containsString("Class[] a =")));
	}
}
