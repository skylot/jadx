package jadx.tests.integration.switches;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSwitch3 extends IntegrationTest {

	public static class TestCls {
		private int i;

		void test(int a) {
			switch (a) {
				case 1:
					i = 1;
					return;
				case 2:
				case 3:
					i = 2;
					return;
				default:
					i = 4;
					break;
			}
			i = 5;
		}

		public void check() {
			test(1);
			assertThat(i).isEqualTo(1);
			test(2);
			assertThat(i).isEqualTo(2);
			test(3);
			assertThat(i).isEqualTo(2);
			test(4);
			assertThat(i).isEqualTo(5);
			test(10);
			assertThat(i).isEqualTo(5);
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.countString(3, "break;")
				.countString(0, "return;");
	}
}
