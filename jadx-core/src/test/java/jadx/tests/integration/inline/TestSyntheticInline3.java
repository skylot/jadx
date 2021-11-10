package jadx.tests.integration.inline;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSyntheticInline3 extends SmaliTest {

	@SuppressWarnings({ "Convert2Lambda", "TrivialFunctionalExpressionUsage" })
	public static class TestCls {
		private String strField;

		private String str() {
			return "a";
		}

		private void test() {
			new Function<String, Void>() {
				@Override
				public Void apply(String s) {
					System.out.println(s + strField + str());
					return null;
				}
			}.apply("c");
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code();
	}

	@Test
	public void testSmali() {
		allowWarnInCode();
		disableCompilation();
		assertThat(getClassNodeFromSmaliFiles())
				.code()
				.doesNotContain(".access$getDialog$p(")
				.doesNotContain(".access$getChooserIntent(")
				.doesNotContain("= r1;")
				.doesNotContain("this$0");
	}
}
