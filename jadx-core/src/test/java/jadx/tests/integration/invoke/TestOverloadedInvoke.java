package jadx.tests.integration.invoke;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestOverloadedInvoke extends IntegrationTest {

	public static class TestCls {
		public static final int N = 10;

		public void test() {
			int[][][] arr = new int[N][N][N];
			use(arr, -1);
			use(arr[0], -2);
		}

		public void use(Object[][] arr, Object obj) {
		}

		public void use(int[][] arr, int i) {
		}
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.D8_J11, TestProfile.JAVA8 })
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("use(iArr[0], -2);")
				.containsOne("use((Object[][]) iArr, (Object) (-1));");
		// TODO: don't add unnecessary casts
		// .containsOne("use(iArr, -1);");
		// TODO: replace call `Array.newInstance` with new array creation: `new int[N][N][N]`
		// .containsOne("new int[10][10][10];");
	}
}
