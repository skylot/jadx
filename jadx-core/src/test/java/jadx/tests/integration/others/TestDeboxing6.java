package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestDeboxing6 extends IntegrationTest {

	@SuppressWarnings({ "NumberEquality", "WrapperTypeMayBePrimitive" })
	public static class TestCls {
		public boolean test1() {
			Integer a = 1000;
			Integer b = 1000;
			return a == b;
		}

		public boolean test2() {
			Long a = 5000L;
			Long b = 5000L;
			return a != b;
		}

		public void check() {
			assertThat(test1()).isFalse();
			assertThat(test2()).isTrue();
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("1000 == 1000")
				.doesNotContain("5000L != 5000L");
	}
}
