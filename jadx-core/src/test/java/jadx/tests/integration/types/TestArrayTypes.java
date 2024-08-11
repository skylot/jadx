package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestArrayTypes extends IntegrationTest {

	public static class TestCls {

		public void test() {
			Exception e = new Exception();
			System.out.println(e);
			use(new Object[] { e });
		}

		public void use(Object[] arr) {
		}

		public void check() {
			test();
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("use(new Object[]{e});");
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("use(new Object[]{exc});");
	}
}
