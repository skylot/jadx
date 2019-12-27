package jadx.tests.integration.loops;

import java.util.Arrays;
import java.util.Iterator;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestDoWhileBreak2 extends IntegrationTest {

	public static class TestCls {
		Iterator<String> it;

		@SuppressWarnings("ConstantConditions")
		public Object test() {
			String obj;
			do {
				obj = this.it.next();
				if (obj == null) {
					return obj; // 'return null' works
				}
			} while (this.it.hasNext());
			return obj;
		}

		public void check() {
			this.it = Arrays.asList("a", "b").iterator();
			assertThat(test()).isEqualTo("b");

			this.it = Arrays.asList("a", "b", null).iterator();
			assertThat(test()).isEqualTo(null);
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsLine(2, "do {")
				.containsLine(3, "obj = this.it.next();")
				.containsLine(3, "if (obj == null) {")
				.containsLine(2, "} while (this.it.hasNext());");
	}
}
