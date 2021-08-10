package jadx.tests.integration.synchronize;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSynchronized6 extends SmaliTest {

	public static class TestCls {
		private final Object lock = new Object();

		private boolean test(Object obj) {
			synchronized (this.lock) {
				return isA(obj) || isB(obj);
			}
		}

		private boolean isA(Object obj) {
			return false;
		}

		private boolean isB(Object obj) {
			return false;
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("synchronized (this.lock) {")
				.containsOne("isA(obj) || isB(obj);"); // TODO: "return isA(obj) || isB(obj);"
	}

	@Test
	public void testSmali() {
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("synchronized (this.lock) {");
		// TODO: .containsOne("return isA(obj) || isB(obj);");
	}
}
