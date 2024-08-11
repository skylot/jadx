package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestLoopDetection3 extends IntegrationTest {

	public static class TestCls {

		public void test(TestCls parent, int pos) {
			Object item;
			while (--pos >= 0) {
				item = parent.get(pos);
				if (item instanceof String) {
					func((String) item);
					return;
				}
			}
		}

		private Object get(int pos) {
			return null;
		}

		private void func(String item) {
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("while");
	}

	@Test
	@NotYetImplemented
	public void test2() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("while (--pos >= 0) {");
	}
}
