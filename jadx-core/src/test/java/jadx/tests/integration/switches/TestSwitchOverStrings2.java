package jadx.tests.integration.switches;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSwitchOverStrings2 extends IntegrationTest {

	public static class TestCls {

		public int test(String str) {
			switch (str) {
				case "branch1":
				case "branch2":
					return 1;
				case "branch3":
				case "branch4":
				default:
					return 0;
			}
		}

		public void check() {
			assertThat(test("branch1")).isEqualTo(1);
			assertThat(test("branch2")).isEqualTo(1);
			assertThat(test("branch3")).isEqualTo(0);
			assertThat(test("branch4")).isEqualTo(0);
			assertThat(test("other")).isEqualTo(0);
			assertThat(test("other2")).isEqualTo(0);
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.countString(4, "case ")
				.countString(1, "default:")
				.countString(2, "return ");
	}
}
