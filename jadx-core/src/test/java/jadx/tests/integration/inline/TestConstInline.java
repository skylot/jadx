package jadx.tests.integration.inline;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestConstInline extends IntegrationTest {

	public static class TestCls {
		public boolean test() {
			try {
				return f(0);
			} catch (Exception e) {
				return false;
			}
		}

		public boolean f(int i) {
			return true;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("return f(0);"));
		assertThat(code, containsOne("return false;"));
		assertThat(code, not(containsString(" = ")));
	}
}
