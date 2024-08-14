package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestAnonymousClass4 extends IntegrationTest {

	public static class TestCls {
		@SuppressWarnings("unused")
		public static class Inner {
			private int f;
			private double d;

			public void test() {
				new Thread() {
					{
						f = 1;
					}

					@Override
					public void run() {
						d = 7.5;
					}
				}.start();
			}
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne(indent(3) + "new Thread() {")
				.containsOne(indent(4) + '{')
				.containsOne("f = 1;")
				.countString(2, indent(4) + '}')
				.containsOne(indent(4) + "public void run() {")
				.containsOne("d = 7.5")
				.containsOne(indent(3) + "}.start();");
	}
}
