package jadx.tests.integration.others;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

public class TestDeboxing4 extends IntegrationTest {

	public static class TestCls {

		public boolean test(Integer i) {
			return ((Integer) 1).equals(i);
		}

		public void check() {
			assertThat(test(null), Matchers.is(false));
			assertThat(test(0), Matchers.is(false));
			assertThat(test(1), Matchers.is(true));
		}
	}

	@Test
	public void test() {
		noDebugInfo();

		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("return 1.equals(num);")));
	}

	@Test
	@NotYetImplemented("Inline boxed types")
	public void testInline() {
		noDebugInfo();

		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("return ((Integer) 1).equals(i);"));
	}
}
