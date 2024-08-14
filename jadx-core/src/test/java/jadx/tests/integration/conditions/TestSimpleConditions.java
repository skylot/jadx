package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestSimpleConditions extends IntegrationTest {

	public static class TestCls {
		public boolean test1(boolean[] a) {
			return (a[0] && a[1] && a[2]) || (a[3] && a[4]);
		}

		public boolean test2(boolean[] a) {
			return a[0] || a[1] || a[2] || a[3];
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("return (a[0] && a[1] && a[2]) || (a[3] && a[4]);")
				.contains("return a[0] || a[1] || a[2] || a[3];");
	}
}
