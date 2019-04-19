package jadx.tests.integration.loops;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

public class TestNestedLoops5 extends IntegrationTest {

	public static class TestCls {

		public int testFor() {
			int tmp = 1;
			for (int i = 10; i > -1; i--) {
				if (i > tmp) {
					for (int j = 0; j < 54; j++) {
						if (tmp> j) {
							return 0;
						}
					}
				}
				tmp++;
			}
			return tmp;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("continue;")));
	}
}
