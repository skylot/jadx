package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * Issue #2963 Case 1: illegal 'return;' emitted inside 'static { }' initializer.
 */
public class TestStaticInitReturn extends IntegrationTest {

	public static class TestCls {
		public static final int IMPL;

		static {
			int version = getVersion();
			if (version >= 24) {
				IMPL = 24;
			} else if (version >= 23) {
				IMPL = 23;
			} else if (version >= 21) {
				IMPL = 21;
			} else if (version >= 19) {
				IMPL = 19;
			} else if (version >= 17) {
				IMPL = 17;
			} else if (version >= 16) {
				IMPL = 16;
			} else if (version >= 15) {
				IMPL = 15;
			} else if (version >= 14) {
				IMPL = 14;
			} else if (version >= 11) {
				IMPL = 11;
			} else {
				IMPL = 0;
			}
		}

		private static int getVersion() {
			return 21;
		}

		public void check() {
			assertThat(IMPL).isEqualTo(21);
		}
	}

	public static class TestEarlyGuardCls {
		public static int value;

		static {
			int version = getVersion();
			if (version == -999) {
				throw new IllegalStateException("invalid version");
			}
			if (version >= 0) {
				int a = version + 1;
				int b = a * 2;
				value = b + 3;
			}
		}

		private static int getVersion() {
			return 5;
		}

		public void check() {
			assertThat(value).isEqualTo(15);
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("return;")
				.contains("else if");
	}

	@Test
	public void testEarlyGuard() {
		assertThat(getClassNode(TestEarlyGuardCls.class))
				.code()
				.doesNotContain("return;");
	}
}
