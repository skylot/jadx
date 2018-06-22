package jadx.tests.integration.variables;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TestVariablesIfElseChain extends IntegrationTest {

	public static class TestCls {
		String used;

		public String test(int a) {
			if (a == 0) {
				use("zero");
			} else if (a == 1) {
				String r = m(a);
				if (r != null) {
					use(r);
				}
			} else if (a == 2) {
				String r = m(a);
				if (r != null) {
					use(r);
				}
			} else {
				return "miss";
			}
			return null;
		}

		public String m(int a) {
			return "hit" + a;
		}

		public void use(String s) {
			used = s;
		}

		public void check() {
			test(0);
			assertThat(used, is("zero"));
			test(1);
			assertThat(used, is("hit1"));
			test(2);
			assertThat(used, is("hit2"));
			assertThat(test(5), is("miss"));
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("return \"miss\";"));
		// and compilable
	}
}
