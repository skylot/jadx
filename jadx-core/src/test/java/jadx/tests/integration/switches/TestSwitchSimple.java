package jadx.tests.integration.switches;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestSwitchSimple extends IntegrationTest {

	public static class TestCls {
		public void test(int a) {
			String s = null;
			switch (a % 4) {
				case 1:
					s = "1";
					break;
				case 2:
					s = "2";
					break;
				case 3:
					s = "3";
					break;
				case 4:
					s = "4";
					break;
				default:
					System.out.println("Not Reach");
					break;
			}
			System.out.println(s);
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertEquals(5, count(code, "break;"));
		assertEquals(1, count(code, "System.out.println(s);"));
		assertEquals(1, count(code, "System.out.println(\"Not Reach\");"));

		assertThat(code, not(containsString("switch ((a % 4)) {")));
		assertThat(code, containsString("switch (a % 4) {"));
	}
}
