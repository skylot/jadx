package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestFieldInitOrder2 extends SmaliTest {

	@SuppressWarnings({ "SpellCheckingInspection", "StaticVariableName" })
	public static class TestCls {
		static String ZPREFIX = "SOME_";
		private static final String VALUE = ZPREFIX + "VALUE";

		public void check() {
			assertThat(VALUE).isEqualTo("SOME_VALUE");
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("private static final String VALUE = ZPREFIX + \"VALUE\";");
	}

	@Test
	public void testSmali() {
		assertThat(getClassNodeFromSmali())
				.runDecompiledAutoCheck(this)
				.code()
				.containsOne("private static final String VALUE = ZPREFIX + \"VALUE\";");
	}
}
