package jadx.tests.integration.fallback;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestFallbackMode extends IntegrationTest {

	public static class TestCls {

		public int test(int a) {
			while (a < 10) {
				a++;
			}
			return a;
		}
	}

	@Test
	public void test() {
		useDexInput();
		setFallback();
		disableCompilation();

		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("public int test(int r2) {")
				.containsOne("r1 = this;")
				.containsOne("L0:")
				.containsOne("L7:")
				.containsOne("int r2 = r2 + 1")
				.doesNotContain("throw new UnsupportedOperationException");
	}
}
