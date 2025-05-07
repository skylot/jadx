package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTryCatchFinally12 extends IntegrationTest {

	public static class TestCls {
		private StringBuilder sb;

		public void test1(int excType) {
			try {
				try {
					call(excType);
				} catch (NullPointerException e) {
					sb.append("-catch");
				}
				sb.append("-out");
			} finally {
				sb.append("-finally");
			}
		}

		public void test2(int excType) {
			try {
				try {
					call(excType);
				} catch (NullPointerException e) {
					sb.append("-catch");
				}
			} finally {
				sb.append("-finally");
			}
		}

		public void test3(int excType) {
			try {
				call(excType);
			} catch (NullPointerException e) {
				sb.append("-catch");
			} finally {
				sb.append("-finally");
			}
		}

		public void call(int excType) {
			sb.append("call");
			switch (excType) {
				case 1:
					sb.append("-npe");
					throw new NullPointerException();
				case 2:
					sb.append("-iae");
					throw new IllegalArgumentException();
			}
		}

		public String runTest(int testNumber, int excType) {
			sb = new StringBuilder();
			try {
				switch (testNumber) {
					case 1:
						test1(excType);
						break;
					case 2:
						test2(excType);
						break;
					case 3:
						test3(excType);
						break;
				}
			} catch (IllegalArgumentException e) {
				assertThat(excType).isEqualTo(2);
			}
			return sb.toString();
		}

		public void check() {
			assertThat(runTest(1, 0)).isEqualTo("call-out-finally");
			assertThat(runTest(1, 1)).isEqualTo("call-npe-catch-out-finally");
			assertThat(runTest(1, 2)).isEqualTo("call-iae-finally");

			assertThat(runTest(2, 0)).isEqualTo("call-finally");
			assertThat(runTest(2, 1)).isEqualTo("call-npe-catch-finally");
			assertThat(runTest(2, 2)).isEqualTo("call-iae-finally");

			assertThat(runTest(3, 0)).isEqualTo("call-finally");
			assertThat(runTest(3, 1)).isEqualTo("call-npe-catch-finally");
			assertThat(runTest(3, 2)).isEqualTo("call-iae-finally");
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.countString(3, "} finally {");
	}

	@Test
	public void testWithoutFinally() {
		getArgs().setExtractFinally(false);
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("} finally {")
				.countString(2 + 2 + 3, "sb.append(\"-finally\");");
	}
}
