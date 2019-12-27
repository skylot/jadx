package jadx.tests.integration.loops;

import java.util.Iterator;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
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

	@NotYetImplemented
	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOnlyOnce("while");
	}
}
