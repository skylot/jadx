package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestConditions22 extends SmaliTest {

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
			verify(1, 2, 3);
			verify(1, 1, 0);
			verify(2, 3, 4);
			verify(2, 2, 0);
			verify(3, 4, 5);
			verify(3, 3, 0);
			verify(4, 4, 0);
		}

		private static void verify(int a, int b, int result) {
			assertThat(test(a, b)).isEqualTo(result);
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

	@Test
	public void testSmali() {
		allowWarnInCode(); // TODO: don't add 'duplicated region' warning for small and/or constant code
		forceDecompiledCheck();
		assertThat(getClassNodeFromSmali())
				.code();
	}
}
