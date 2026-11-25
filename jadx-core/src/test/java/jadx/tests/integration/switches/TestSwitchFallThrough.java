package jadx.tests.integration.switches;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSwitchFallThrough extends IntegrationTest {

	public static class TestCls {
		public int r;

		@SuppressWarnings("fallthrough")
		public void test(int a) {
			int i = 10;
			switch (a) {
				case 1:
					i = 1000;
					// fallthrough
				case 2:
					r = i;
					break;

				default:
					r = -1;
					break;
			}
			r *= 2;
			System.out.println("in: " + a + ", out: " + r);
		}

		public int testWrap(int a) {
			r = 0;
			test(a);
			return r;
		}

		public void check() {
			assertThat(testWrap(1)).isEqualTo(2000);
			assertThat(testWrap(2)).isEqualTo(20);
			assertThat(testWrap(0)).isEqualTo(-2);
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("switch (a) {")
				.containsOne("r = i;")
				.containsOne("r = -1;")
				.countString(2, "break;");
		// code correctness checks done in 'check' method
	}

	public static class TestClsNestedFallThrough {
		@SuppressWarnings("fallthrough")
		public int test(int a, int b, int c) {
			int r = 0;
			switch (a) {
				case 4:
					switch (b) {
						case 10:
							switch (c) {
								case 100:
									return -100;
								case 200:
									r = 200;
									break;
								default:
									r = 300;
									break;
							}
							break;
						case 20:
							return -20;
						default:
							r = 40;
							break;
					}
					// fallthrough
				case 1:
					if (c >= 0) {
						r += 10;
					}
					break;
				case 2:
					r = -10;
					// fallthrough
				case 3:
					if (c >= 0) {
						r += 30;
					}
					break;
				default:
					r = -1;
					break;
			}
			return r;
		}

		public void check() {
			assertThat(test(4, 10, 100)).isEqualTo(-100);
			assertThat(test(4, 10, 200)).isEqualTo(210);
			assertThat(test(4, 10, 0)).isEqualTo(310);
			assertThat(test(4, 20, 0)).isEqualTo(-20);
			assertThat(test(4, 0, 0)).isEqualTo(50);
			assertThat(test(1, 0, 0)).isEqualTo(10);
			assertThat(test(2, 0, 0)).isEqualTo(20);
			assertThat(test(3, 0, 0)).isEqualTo(30);
			assertThat(test(0, 0, 0)).isEqualTo(-1);
		}
	}

	@Test
	public void testNestedFallThrough() {
		assertThat(getClassNode(TestClsNestedFallThrough.class))
				.code()
				.containsOne("switch (a) {")
				.containsOne("switch (b) {")
				.containsOne("switch (c) {")
				.containsOne("r += 10;");
	}

	public static class TestClsConvergingFallThrough {
		@SuppressWarnings("fallthrough")
		public int test(int a, int b) {
			int r = 0;
			switch (a) {
				case 5:
					if (b == 0) {
						r = 500;
					} else {
						switch (b) {
							case 1:
								r = 50;
								break;
							case 2:
								return -5;
							default:
								r = 60;
								break;
						}
					}
					// fallthrough
				case 1:
					if (b >= 0) {
						r += 10;
					}
					break;
				case 2:
					r = 20;
					break;
				case 3:
					r = 30;
					// fallthrough
				case 4:
					if (b >= 0) {
						r += 40;
					}
					break;
				default:
					r = -1;
					break;
			}
			return r;
		}

		public void check() {
			assertThat(test(5, 0)).isEqualTo(510);
			assertThat(test(5, 1)).isEqualTo(60);
			assertThat(test(5, 2)).isEqualTo(-5);
			assertThat(test(5, 9)).isEqualTo(70);
			assertThat(test(1, 0)).isEqualTo(10);
			assertThat(test(2, 0)).isEqualTo(20);
			assertThat(test(3, 0)).isEqualTo(70);
			assertThat(test(4, 0)).isEqualTo(40);
			assertThat(test(0, 0)).isEqualTo(-1);
		}
	}

	@Test
	public void testConvergingFallThrough() {
		allowWarnInCode();
		assertThat(getClassNode(TestClsConvergingFallThrough.class))
				.code()
				.containsOne("switch (a) {")
				.containsOne("r += 40;");
	}
}
