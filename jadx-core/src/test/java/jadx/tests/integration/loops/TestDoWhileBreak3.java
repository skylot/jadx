package jadx.tests.integration.loops;

import java.util.Iterator;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestDoWhileBreak3 extends IntegrationTest {

	public static class TestCls {
		Iterator<String> it;

		public void test() {
			do {
				if (!it.hasNext()) {
					break;
				}
			} while (it.next() != null);
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("while")
				.containsLines(2, "while (this.it.hasNext() && this.it.next() != null) {", "}");
	}
}
