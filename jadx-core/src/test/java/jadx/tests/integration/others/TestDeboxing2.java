package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static jadx.tests.api.utils.JadxMatchers.countString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestDeboxing2 extends IntegrationTest {

	public static class TestCls {
		public long test(Long l) {
			if (l == null) {
				l = 0L;
			}
			return l;
		}

		public void check() {
			assertThat(test(null), is(0L));
			assertThat(test(0L), is(0L));
			assertThat(test(7L), is(7L));
		}
	}

	@Test
	public void test() {
		noDebugInfo();

		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("long test(Long l)"));
		assertThat(code, containsOne("if (l == null) {"));
		assertThat(code, containsOne("l = 0L;"));

		// checks for 'check' method
		assertThat(code, containsOne("test(null)"));
		assertThat(code, containsOne("test(0L)"));
		assertThat(code, countString(2, "is(0L)"));
		assertThat(code, containsOne("test(7L)"));
		assertThat(code, containsOne("is(7L)"));
	}
}
