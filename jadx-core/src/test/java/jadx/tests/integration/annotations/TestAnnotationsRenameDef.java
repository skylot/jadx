package jadx.tests.integration.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestAnnotationsRenameDef extends IntegrationTest {

	public static class TestCls {

		@Target(ElementType.METHOD)
		@Retention(RetentionPolicy.RUNTIME)
		public @interface A {
			int value();
		}

		@A(5)
		void test() {
		}
	}

	@Test
	public void test() {
		enableDeobfuscation();
		// force rename 'value' method
		args.setDeobfuscationMinLength(20);

		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("public @interface ")
				.doesNotContain("int value();")
				.doesNotContain("(5)");
	}
}
