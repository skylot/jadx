package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestAnonymousClass17 extends IntegrationTest {

	public static class TestCls {

		@SuppressWarnings({ "checkstyle:InnerAssignment", "Convert2Lambda" })
		public void test(boolean a, boolean b) {
			String v;
			if (a && (v = get(b)) != null) {
				use(new Runnable() {
					@Override
					public void run() {
						System.out.println(v);
					}
				});
			}
		}

		public String get(boolean a) {
			return a ? "str" : null;
		}

		public void use(Runnable r) {
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("if (a && (v = get(b)) != null) {");
	}
}
