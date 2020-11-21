package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestInterfaceDefaultMethod extends IntegrationTest {

	public static class TestCls {

		@SuppressWarnings("UnnecessaryInterfaceModifier")
		public interface ITest {
			void test1();

			default void test2() {
			}

			static void test3() {
			}

			abstract void test4();
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("static default")
				.doesNotContain("abstract")
				.containsOne("void test1();")
				.containsOne("default void test2() {")
				.containsOne("static void test3() {");
	}
}
