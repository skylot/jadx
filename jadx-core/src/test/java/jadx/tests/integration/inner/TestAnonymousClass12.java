package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestAnonymousClass12 extends IntegrationTest {

	public static class TestCls {

		public abstract static class BasicAbstract {
			public abstract void doSomething();
		}

		public BasicAbstract outer;
		public BasicAbstract inner;

		public void test() {
			outer = new BasicAbstract() {
				@Override
				public void doSomething() {
					inner = new BasicAbstract() {
						@Override
						public void doSomething() {
							inner = null;
						}
					};
				}
			};
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("outer = new BasicAbstract() {")
				.containsOne("inner = new BasicAbstract() {")
				.containsOne("inner = null;");
	}
}
