package jadx.tests.internal.conditions;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static jadx.tests.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;

public class TestConditions14 extends InternalJadxTest {

	public static class TestCls {

		public static boolean test(Object a, Object b) {
			boolean r = a == null ? b != null : !a.equals(b);
			if (r) {
				return false;
			}
			System.out.println("1");
			return true;
		}

//		public static boolean test2(Object a, Object b) {
//			if (a == null ? b != null : !a.equals(b)) {
//				return false;
//			}
//			System.out.println("2");
//			return true;
//		}

	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		System.out.println(code);

		assertThat(code, containsOne("boolean r = a == null ? b != null : !a.equals(b);"));
		assertThat(code, containsOne("if (r) {"));
		assertThat(code, containsOne("System.out.println(\"1\");"));

	}
}
