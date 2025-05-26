package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestPrimitivesInIf extends IntegrationTest {

	public static class TestCls {

		public boolean test(String str) {
			short sh = Short.parseShort(str);
			int i = Integer.parseInt(str);
			System.out.println(sh + " vs " + i);
			return sh == i;
		}

		public void check() {
			assertThat(test("1")).isTrue();
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("short sh = Short.parseShort(str);")
				.containsOne("int i = Integer.parseInt(str);")
				.containsOne("return sh == i;");
	}

	@Test
	public void test2() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("short s = Short.parseShort(str);");
	}
}
