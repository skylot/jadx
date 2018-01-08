package jadx.tests.integration.conditions;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static jadx.tests.api.utils.JadxMatchers.countString;
import static org.junit.Assert.assertThat;

public class TestNestedIf extends IntegrationTest {

	public static class TestCls {
		private boolean a0 = false;
		private int a1 = 1;
		private int a2 = 2;
		private int a3 = 1;
		private int a4 = 2;

		public boolean test1() {
			if (a0) {
				if (a1 == 0 || a2 == 0) {
					return false;
				}
			} else if (a3 == 0 || a4 == 0) {
				return false;
			}
			test1();
			return true;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("if (this.a0) {"));
		assertThat(code, containsOne("if (this.a1 == 0 || this.a2 == 0) {"));
		assertThat(code, containsOne("} else if (this.a3 == 0 || this.a4 == 0) {"));
		assertThat(code, countString(2, "return false;"));
		assertThat(code, containsOne("test1();"));
		assertThat(code, containsOne("return true;"));

	}
}
