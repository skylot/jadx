package jadx.tests.integration.inline;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

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
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return f(0);")
				.containsOne("return false;")
				.doesNotContain(" = ");
	}
}
