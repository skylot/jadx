package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestArrayTypes extends IntegrationTest {

	@SuppressWarnings({ "ThrowablePrintedToSystemOut", "unused" })
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
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("use(new Object[]{e});");
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("use(new Object[]{exc});");
	}
}
