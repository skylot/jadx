package jadx.tests.integration.switches;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;
import org.junit.Test;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TestSwitchReturnFromCase2 extends IntegrationTest {

	public static class TestCls {
		public boolean test(int a) {
			switch (a % 4) {
				case 2:
				case 3:
					if (a == 2) {
						return true;
					}
					return true;
			}
			return false;
		}

		public void check() {
			assertTrue(test(2));
			assertTrue(test(3));
			assertTrue(test(15));
			assertFalse(test(1));
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("switch (a % 4) {"));
	}
}
