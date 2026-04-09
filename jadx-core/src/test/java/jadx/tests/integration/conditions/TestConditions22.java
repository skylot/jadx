package jadx.tests.integration.conditions;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestConditions22 extends IntegrationTest {

	public static class TestCls {

		public static int test(int i, int j) {
			int k;
			if (i == 1) {
				if (j == 2) {
					k = 3;
				} else {
					k = 0;
				}
			} else if (i == 2) {
				if (j == 3) {
					k = 4;
				} else {
					k = 0;
				}
			} else if (i == 3) {
				if (j == 4) {
					k = 5;
				} else {
					k = 0;
				}
			} else {
				k = 0;
			}
			System.out.println("k = " + k);
			return k;
		}

		public void check() {
			assertThat(test(1, 2)).isEqualTo(3);
			assertThat(test(1, 1)).isEqualTo(0);
			assertThat(test(2, 3)).isEqualTo(4);
			assertThat(test(2, 2)).isEqualTo(0);
			assertThat(test(3, 4)).isEqualTo(5);
			assertThat(test(3, 3)).isEqualTo(0);
			assertThat(test(4, 4)).isEqualTo(0);
		}
	}

	@TestWithProfiles(TestProfile.JAVA17)
	public void testJava() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne(indent(2) + "if (")
				.containsOne(indent(2) + "} else if (")
				.containsOne(indent(2) + "} else {");
	}
}
