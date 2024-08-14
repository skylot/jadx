package jadx.tests.integration.synchronize;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSynchronized3 extends IntegrationTest {

	public static class TestCls {
		private int x;

		public void f() {
		}

		public void test() {
			while (true) {
				synchronized (this) {
					if (x == 0) {
						throw new IllegalStateException();
					}
					x++;
					if (x == 10) {
						break;
					}
				}
				this.x++;
				f();
			}
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsLines(3, "}", "this.x++;", "f();");
	}
}
