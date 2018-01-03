package jadx.tests.integration.loops;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class TestTryCatchInLoop extends IntegrationTest {

	public static class TestCls {
		int c = 0;

		public int test() {
			while (true) {
				try {
					exc();
					break;
				} catch (Exception e) {
					//
				}
			}
			if (c == 5) {
				System.out.println(c);
			}
			return 0;
		}

		private void exc() throws Exception {
			c++;
			if (c < 3) {
				throw new Exception();
			}
		}

		public void check() {
			test();
			assertEquals(3, c);
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("} catch (Exception e) {"));
		assertThat(code, containsOne("break;"));
	}
}
