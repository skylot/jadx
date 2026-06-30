package jadx.tests.integration.arrays;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestNewArrayOfArrays extends IntegrationTest {

	public static class TestCls {
		public static long[][] anewarrayOfArray(int n) {
			return new long[n][]; // anewarray [J
		}

		public static String[][] anewarrayOfObjectArray(int n) {
			return new String[n][]; // anewarray [Ljava/lang/String;
		}

		public static int[][][] anewarrayDeep(int n) {
			return new int[n][][]; // anewarray [[I
		}

		public static int[][] multiAnewarray(int a, int b) {
			return new int[a][b]; // multianewarray [[I
		}
	}

	@Test
	public void test() {
		useJavaInput();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("new long[n][]")
				.containsOne("new String[n][]")
				.containsOne("new int[n][][]")
				.containsOne("new int[a][b]");
	}
}
