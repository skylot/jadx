package jadx.tests.integration.inline;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestTernaryCast extends IntegrationTest {

	public static class TestCls {
		public String test(boolean b, Object obj, CharSequence cs) {
			return (String) (b ? obj : cs);
		}

		public void check() {
			assertThat(test(true, "a", "b"), Matchers.is("a"));
			assertThat(test(false, "a", "b"), Matchers.is("b"));
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("return (String) (b ? obj : cs);"));
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		getClassNode(TestCls.class);
	}
}
