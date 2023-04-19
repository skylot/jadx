package jadx.tests.integration.java8;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestLambdaReturn extends IntegrationTest {

	@SuppressWarnings("unused")
	public static class TestCls {
		interface Function0<R> {
			R apply();
		}

		public static class T2 {
			public long l;

			public T2(long l) {
				this.l = l;
			}

			public void w() {
			}
		}

		public Byte test(Byte b1) {
			Function0<Void> f1 = () -> {
				new T2(94L).w();
				return null;
			};
			f1.apply();
			return null;
		}
	}

	@TestWithProfiles(TestProfile.DX_J8)
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsLines(2,
						"Function0<Void> f1 = () -> {",
						indent() + "new T2(94L).w();",
						indent() + "return null;",
						"};");
	}

	@TestWithProfiles(TestProfile.D8_J11_DESUGAR)
	public void testLambda() {
		getClassNode(TestCls.class);
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		getClassNode(TestCls.class);
	}
}
