package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestEndlessLoop extends IntegrationTest {

	public static class TestCls {

		void test1() {
			while (this == this) {
			}
		}

		void test2() {
			do {
			} while (this == this);
		}

		void test3() {
			while (true) {
				if (this != this) {
					return;
				}
			}
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("while (this == this)");
	}
}
