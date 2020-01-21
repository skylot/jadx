package jadx.tests.integration.switches;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestSwitchReturnFromCase extends IntegrationTest {

	public static class TestCls {
		public void test(int a) {
			if (a > 1000) {
				return;
			}
			String s = null;
			switch (a % 10) {
				case 1:
					s = "1";
					break;
				case 2:
					s = "2";
					break;
				case 3:
				case 4:
					s = "4";
					break;
				case 5:
					break;
				case 6:
					return;
			}
			if (s == null) {
				s = "5";
			}
			System.out.println(s);
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("switch (a % 10) {"));

		// case 5: removed
		assertEquals(5, count(code, "case "));
		assertEquals(3, count(code, "break;"));

		assertThat(code, containsOne("s = \"1\";"));
		assertThat(code, containsOne("s = \"2\";"));
		assertThat(code, containsOne("s = \"4\";"));
		assertThat(code, containsOne("s = \"5\";"));
	}
}
