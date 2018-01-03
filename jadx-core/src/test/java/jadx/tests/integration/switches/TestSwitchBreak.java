package jadx.tests.integration.switches;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class TestSwitchBreak extends IntegrationTest {

	public static class TestCls {
		public String test(int a) {
			String s = "";
			loop:
			while (a > 0) {
				switch (a % 4) {
					case 1:
						s += "1";
						break;
					case 3:
					case 4:
						s += "4";
						break;
					case 5:
						s += "+";
						break loop;
				}
				s += "-";
				a--;
			}
			return s;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("switch (a % 4) {"));
		assertEquals(4, count(code, "case "));
		assertEquals(3, count(code, "break;"));

		// TODO finish break with label from switch
		assertThat(code, containsOne("return s + \"+\";"));
	}
}
