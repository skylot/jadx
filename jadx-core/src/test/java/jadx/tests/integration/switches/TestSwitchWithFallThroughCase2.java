package jadx.tests.integration.switches;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class TestSwitchWithFallThroughCase2 extends IntegrationTest {

	@SuppressWarnings("fallthrough")
	public static class TestCls {
		public String test(int a, boolean b, boolean c) {
			String str = "";
			if (a > 0) {
				switch (a % 4) {
					case 1:
						str += ">";
						if (a == 5 && b) {
							if (c) {
								str += "1";
							} else {
								str += "!c";
							}
							break;
						}
					case 2:
						if (b) {
							str += "2";
						}
						break;
					case 3:
						break;
					default:
						str += "default";
						break;
				}
				str += "+";
			}
			if (b && c) {
				str += "-";
			}
			return str;
		}

		public void check() {
			assertEquals(">1+-", test(5, true, true));
			assertEquals(">2+-", test(1, true, true));
			assertEquals("+-", test(3, true, true));
			assertEquals("default+-", test(16, true, true));
			assertEquals("-", test(-1, true, true));
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("switch (a % 4) {"));
		assertThat(code, containsOne("if (a == 5 && b) {"));
		assertThat(code, containsOne("if (b) {"));
	}
}
