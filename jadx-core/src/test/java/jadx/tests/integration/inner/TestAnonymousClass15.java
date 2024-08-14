package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestAnonymousClass15 extends IntegrationTest {

	public static class TestCls {

		public Thread test(Runnable run) {
			return new Thread(run) {
				@Override
				public void run() {
					System.out.println("run");
					super.run();
				}
			};
		}

		public Thread test2(Runnable run) {
			return new Thread(run) {
				{
					setName("run");
				}

				@Override
				public void run() {
				}
			};
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.countString(2, "return new Thread(run) {")
				.containsOne("setName(\"run\");");
	}
}
