package jadx.tests.integration.synchronize;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSynchronized7 extends IntegrationTest {

	public static class TestCls {
		private int i;

		public synchronized void doSomethingFallthrough() {
			switch (this.i) {
				case 42:
					doSomething2();
					// fallthrough
				default:
					break;
			}
		}

		public synchronized void doSomethingWithBreak() {
			switch (this.i) {
				case 42:
					doSomething2();
					break;
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
		ClassNode cls = getClassNode(TestCls.class);
		assertThat(cls).code()
				.doesNotContain("throw")
				.doesNotContain("throw th;")
				.doesNotContain("try {")
				.doesNotContain("catch")
				.containsOne("public synchronized void doSomethingFallthrough() {")
				.containsOne("public synchronized void doSomethingWithBreak() {")
				.countString(2, "switch (this.i) {");
	}
}
