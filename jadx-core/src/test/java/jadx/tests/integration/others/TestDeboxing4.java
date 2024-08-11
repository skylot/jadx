package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestDeboxing4 extends IntegrationTest {

	public static class TestCls {

		public boolean test(Integer i) {
			return ((Integer) 1).equals(i);
		}

		public void check() {
			assertThat(test(null)).isFalse();
			assertThat(test(0)).isFalse();
			assertThat(test(1)).isTrue();
		}
	}

	@Test
	public void test() {
		noDebugInfo();

		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("return 1.equals(num);");
	}

	@Test
	@NotYetImplemented("Inline boxed types")
	public void testInline() {
		noDebugInfo();

		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return ((Integer) 1).equals(i);");
	}
}
