package jadx.tests.integration.synchronize;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

@SuppressWarnings("SwitchStatementWithTooFewBranches")
public class TestSynchronized7 extends IntegrationTest {

	public static class TestCls {
		private int i;

		public synchronized void test() {
			switch (this.i) {
				case 42:
					doSomething2();
				default:
					break;
			}
		}

		private void doSomething2() {
		}
	}

	@Test
	public void test() {
		useDexInput();
		assertThat(getClassNode(TestCls.class)).code()
				.doesNotContain("throw ")
				.doesNotContain("try {")
				.doesNotContain("catch")
				.containsOne("public synchronized void test() {")
				.containsOne("switch (this.i) {");
	}
}
