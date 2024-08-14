package jadx.tests.integration.variables;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

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
			assertThat(used).isEqualTo("zero");
			test(1);
			assertThat(used).isEqualTo("hit1");
			test(2);
			assertThat(used).isEqualTo("hit2");
			assertThat(test(5)).isEqualTo("miss");
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return \"miss\";");
		// and compilable
	}
}
