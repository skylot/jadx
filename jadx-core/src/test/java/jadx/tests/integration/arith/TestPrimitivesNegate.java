package jadx.tests.integration.arith;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestPrimitivesNegate extends IntegrationTest {

	@SuppressWarnings("UnnecessaryUnaryMinus")
	public static class TestCls {
		public double test() {
			double[] arr = new double[5];
			arr[0] = -20;
			arr[0] += -79;
			return arr[0];
		}

		public void check() {
			assertThat(test()).isEqualTo(-99);
		}
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.JAVA8 })
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("dArr[0] = -20.0d;")
				.containsOne("dArr[0] = dArr[0] - 79.0d;");
	}
}
