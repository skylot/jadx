package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestConditions14 extends IntegrationTest {

	@SuppressWarnings({ "EqualsReplaceableByObjectsCall", "ConstantConditions" })
	public static class TestCls {

		public static boolean test(Object a, Object b) {
			boolean r = a == null ? b != null : !a.equals(b);
			if (r) {
				return false;
			}
			System.out.println("r=" + r);
			return true;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("boolean r = a == null ? b != null : !a.equals(b);"));
		assertThat(code, containsOne("if (r) {"));
		assertThat(code, containsOne("System.out.println(\"r=\" + r);"));
	}
}
