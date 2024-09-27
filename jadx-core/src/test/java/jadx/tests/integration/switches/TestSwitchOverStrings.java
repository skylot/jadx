package jadx.tests.integration.switches;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSwitchOverStrings extends IntegrationTest {

	/**
	 * Strings 'frewhyh', 'phgafkp' and 'ucguedt' have same hash code.
	 */
	public static class TestCls {

		public int test(String str) {
			switch (str) {
				case "frewhyh":
					return 1;
				case "phgafkp":
					return 2;
				case "test":
				case "test2":
					return 3;
				case "other":
					return 4;
				default:
					return 0;
			}
		}

		public void check() {
			assertThat(test("frewhyh")).isEqualTo(1);
			assertThat(test("phgafkp")).isEqualTo(2);
			assertThat(test("test")).isEqualTo(3);
			assertThat(test("test2")).isEqualTo(3);
			assertThat(test("other")).isEqualTo(4);
			assertThat(test("unknown")).isEqualTo(0);
			assertThat(test("ucguedt")).isEqualTo(0);
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("case -603257287:")
				.doesNotContain("c = ")
				.containsOne("case \"frewhyh\":")
				.countString(5, "return ");
	}
}
