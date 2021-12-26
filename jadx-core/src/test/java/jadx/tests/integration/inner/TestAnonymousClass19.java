package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestAnonymousClass19 extends SmaliTest {

	@SuppressWarnings({ "Convert2Lambda", "unused" })
	public static class TestCls {

		public void test(boolean a, boolean b) {
			boolean c = a && b;
			use(new Runnable() {
				@Override
				public void run() {
					System.out.println(a + " && " + b + " = " + c);
				}
			});
		}

		public void use(Runnable r) {
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("System.out.println(a + \" && \" + b + \" = \" + c);");
	}

	@Test
	public void testSmali() {
		assertThat(getClassNodeFromSmaliFiles("ATestCls"))
				.code()
				.containsOne("System.out.println(a + \" && \" + b + \" = \" + c);");
	}
}
