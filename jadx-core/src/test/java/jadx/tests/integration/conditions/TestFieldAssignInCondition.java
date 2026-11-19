package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * Issue #2963 Case 3: assignment vs. comparison precedence in 'if' condition.
 */
public class TestFieldAssignInCondition extends IntegrationTest {

	public static class TestCls {
		public static class Buffer {
			private int lengthOfLastRead;
		}

		private static int staticVal;

		private final Buffer buf = new Buffer();
		private final int[] arr = new int[1];

		private int read(int v) {
			return v;
		}

		@SuppressWarnings("checkstyle:InnerAssignment")
		public int testInstanceField(int v) {
			if ((this.buf.lengthOfLastRead = read(v)) == -1) {
				return -1;
			}
			return this.buf.lengthOfLastRead;
		}

		@SuppressWarnings("checkstyle:InnerAssignment")
		public int testStaticField(int v) {
			if ((staticVal = read(v)) <= 0) {
				return 0;
			}
			return staticVal;
		}

		@SuppressWarnings("checkstyle:InnerAssignment")
		public int testArrayElement(int v) {
			if ((this.arr[0] = read(v)) == -1) {
				return -1;
			}
			return this.arr[0];
		}

		public class Inner {
			@SuppressWarnings("checkstyle:InnerAssignment")
			public int testInnerAccess(int v) {
				if ((TestCls.this.buf.lengthOfLastRead = read(v)) == -1) {
					return -1;
				}
				return TestCls.this.buf.lengthOfLastRead;
			}

			@SuppressWarnings("checkstyle:InnerAssignment")
			public int testInnerStaticAccess(int v) {
				if ((staticVal = read(v)) <= 0) {
					return 0;
				}
				return staticVal;
			}
		}

		public void check() {
			assertThat(testInstanceField(-1)).isEqualTo(-1);
			assertThat(testInstanceField(5)).isEqualTo(5);
			assertThat(testStaticField(0)).isEqualTo(0);
			assertThat(testStaticField(7)).isEqualTo(7);
			assertThat(testArrayElement(-1)).isEqualTo(-1);
			assertThat(testArrayElement(3)).isEqualTo(3);
			Inner inner = new Inner();
			assertThat(inner.testInnerAccess(-1)).isEqualTo(-1);
			assertThat(inner.testInnerAccess(9)).isEqualTo(9);
			assertThat(inner.testInnerStaticAccess(0)).isEqualTo(0);
			assertThat(inner.testInnerStaticAccess(11)).isEqualTo(11);
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("= read(v) == -1")
				.doesNotContain("= read(v) <= 0");
	}
}
