package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestLoopCondition extends IntegrationTest {

	public static class TestCls {
		public void test(java.util.ArrayList<String> list) {
			for (int i = 0; i != 16 && i < 255; i++) {
				list.set(i, "ABC");
				if (i == 128) {
					return;
				}
				list.set(i, "DEF");
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("list.set(i, \"ABC\")"));
		assertThat(code, containsOne("list.set(i, \"DEF\")"));
	}
}
