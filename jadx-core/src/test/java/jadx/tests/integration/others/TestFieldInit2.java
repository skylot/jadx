package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestFieldInit2 extends IntegrationTest {

	public static class TestCls {

		public interface BasicAbstract {
			void doSomething();
		}

		public BasicAbstract x = new BasicAbstract() {
			@Override
			public void doSomething() {
				y = 1;
			}
		};
		public int y = 0;

		public TestCls() {
		}

		public TestCls(int z) {
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("x = new BasicAbstract() {")
				.containsOne("y = 0;")
				.containsLines(1, "public TestFieldInit2$TestCls(int z) {", "}");
	}
}
