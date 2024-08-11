package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestInnerClass4 extends IntegrationTest {

	public static class TestCls {
		public class C {
			public String c;

			private C() {
				this.c = "c";
			}
		}

		public String test() {
			return new C().c;
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return new C().c;");
	}
}
