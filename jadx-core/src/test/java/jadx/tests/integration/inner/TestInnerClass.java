package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestInnerClass extends IntegrationTest {

	public static class TestCls {
		public class Inner {
			public class Inner2 extends Thread {
			}
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("Inner {")
				.contains("Inner2 extends Thread {")
				.doesNotContain("super();")
				.doesNotContain("this$")
				.doesNotContain("/* synthetic */");
	}
}
