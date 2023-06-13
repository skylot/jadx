package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * Don't inline const string into anonymous class constructor
 */
public class TestAnonymousClass21 extends IntegrationTest {

	@SuppressWarnings("Convert2Lambda")
	public static class TestCls {
		public void test() {
			String str = "str";
			new Thread(new Runnable() {
				@Override
				public void run() {
					System.out.println(str);
				}
			}).start();
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("String str = \"str\";")
				.containsOne("System.out.println(str);");
	}
}
