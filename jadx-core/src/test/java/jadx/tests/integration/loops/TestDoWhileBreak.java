package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestDoWhileBreak extends IntegrationTest {

	public static class TestCls {

		public int test(int k) throws InterruptedException {
			int i = 3;
			do {
				if (k > 9) {
					i = 0;
					break;
				}
				i++;
			} while (i < 5);

			return i;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("while ("));
	}
}
