package jadx.tests.integration.others;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestDeboxing5 extends IntegrationTest {

	@SuppressWarnings("WrapperTypeMayBePrimitive")
	public static class TestCls {
		private static String type;

		public static void test(String[] args) {
			Float f = (float) -47.99;
			Boolean b = args.length == 0;
			Object o = ((b) ? false : f);
			call(o);
		}

		public static void call(Object o) {
			if (o instanceof Boolean) {
				type = "Boolean";
			}
			if (o instanceof Float) {
				type = "Float";
			}
		}

		private static void verify(String[] arr, String str) {
			type = null;
			test(arr);
			assertThat(type).isEqualTo(str);
		}

		public void check() {
			verify(new String[0], "Boolean");
			verify(new String[] { "1" }, "Float");
		}
	}

	@TestWithProfiles(TestProfile.D8_J11)
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("boolean valueOf");
	}
}
