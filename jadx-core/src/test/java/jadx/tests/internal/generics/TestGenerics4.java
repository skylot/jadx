package jadx.tests.internal.generics;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestGenerics4 extends InternalJadxTest {

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
		System.out.println(code);

		assertThat(code, containsString("Class<?>[] a ="));
		assertThat(code, not(containsString("Class[] a =")));
	}
}
