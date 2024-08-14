package jadx.tests.integration.inner;

import java.util.Timer;
import java.util.TimerTask;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestInnerClass2 extends IntegrationTest {

	public static class TestCls {
		private static class TerminateTask extends TimerTask {
			@Override
			public void run() {
				System.err.println("Test timed out");
			}
		}

		public void test() {
			new Timer().schedule(new TerminateTask(), 1000L);
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("new Timer().schedule(new TerminateTask(), 1000L);")
				.doesNotContain("synthetic")
				.doesNotContain("this")
				.doesNotContain("null")
				.doesNotContain("AnonymousClass");
	}
}
