package jadx.tests.integration.generics;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestGeneric8 extends IntegrationTest {

	public static class TestCls {
		@SuppressWarnings("InnerClassMayBeStatic")
		public class TestNumber<T extends Integer> {
			private final T n;

			public TestNumber(T n) {
				this.n = n;
			}

			public boolean isEven() {
				return n.intValue() % 2 == 0;
			}
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("public TestNumber(T n");
	}
}
