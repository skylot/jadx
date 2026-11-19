package jadx.tests.integration.trycatch;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTryFinallyCatchVarDecl extends IntegrationTest {

	public static class TestCls {
		interface Resource extends Closeable {
			void use(Object arg) throws IOException;
		}

		public static void normalize(Object from, Object to, Map<String, String> props) throws IOException {
			if (props == null) {
				props = new HashMap<>();
			}
			props.put("limit", "-1");
			Resource temp = open(from);
			try {
				Resource os = open(temp);
				Resource j = null;
				try {
					j = open(from);
					j.use(os);
					j = null;
					os.close();
					os = null;

					os = open(to);
					os.use(temp);
				} finally {
					if (j != null) {
						j.close();
					}
					if (os != null) {
						os.close();
					}
				}
			} finally {
				temp.close();
			}
		}

		public static Throwable catchIntoParam(Throwable err, Runnable r) {
			try {
				r.run();
			} catch (Throwable ex) {
				err = ex;
			}
			return err;
		}

		private static Resource open(Object arg) throws IOException {
			return null;
		}
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.D8_J11, TestProfile.JAVA11 })
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("normalize(");
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.D8_J11, TestProfile.JAVA11 })
	public void testNoDebug() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("normalize(")
				.doesNotContain("th = th;");
	}
}
