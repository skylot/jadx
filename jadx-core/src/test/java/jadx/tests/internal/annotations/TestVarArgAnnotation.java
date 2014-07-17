package jadx.tests.internal.annotations;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class TestVarArgAnnotation extends InternalJadxTest {

	public static class TestCls {

		void test1(int... a) {
		}

		void test2(int i, Object... a) {
		}

		void test3(int[] a) {
		}

		void call() {
			test1(1, 2);
			test2(3, "1", 7);
			test3(new int[]{5, 8});
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		System.out.println(code);

		assertThat(code, containsString("void test1(int... a) {"));
		assertThat(code, containsString("void test2(int i, Object... a) {"));

		// TODO:
		assertThat(code, containsString("test1(new int[]{1, 2});"));
		assertThat(code, containsString("test2(3, objArr);"));

		// negative case
		assertThat(code, containsString("void test3(int[] a) {"));
		assertThat(code, containsString("test3(new int[]{5, 8});"));
	}
}
