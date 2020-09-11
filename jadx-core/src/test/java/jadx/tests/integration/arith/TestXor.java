package jadx.tests.integration.arith;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestXor extends SmaliTest {

	@SuppressWarnings("PointlessBooleanExpression")
	public static class TestCls {
		public boolean test1() {
			return test() ^ true;
		}

		public boolean test2(boolean v) {
			return v ^ true;
		}

		public boolean test() {
			return true;
		}

		public void check() {
			assertThat(test1()).isFalse();
			assertThat(test2(true)).isFalse();
			assertThat(test2(false)).isTrue();
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return !test();")
				.containsOne("return !v;");
	}

	@Test
	public void smali() {
		// @formatter:off
		/*
			public boolean test1() {
				return test() ^ true;
			}

			public boolean test2() {
				return test() ^ false;
			}

			public boolean test() {
				return true;
			}
		 */
		// @formatter:on
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("return !test();")
				.containsOne("return test();");
	}

}
