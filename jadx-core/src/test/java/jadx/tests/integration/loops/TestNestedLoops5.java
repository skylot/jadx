package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestNestedLoops5 extends IntegrationTest {

	public static class TestCls {

		public int testFor() {
			int tmp = 1;
			for (int i = 5; i > -1; i--) {
				if (i > tmp) {
					for (int j = 1; j < 5; j++) {
						if (tmp > j * 100) {
							return 0;
						}
					}
				}
				tmp++;
			}
			return tmp;
		}

		public void check() {
			assertEquals(7, testFor());
		}
	}

	@Test
	@NotYetImplemented
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("continue;")));
	}
}
