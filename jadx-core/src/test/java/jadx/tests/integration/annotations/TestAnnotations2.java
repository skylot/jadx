package jadx.tests.integration.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestAnnotations2 extends IntegrationTest {

	public static class TestCls {

		@Target(ElementType.TYPE)
		@Retention(RetentionPolicy.RUNTIME)
		public @interface A {
			int i();

			float f();
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("@Target({ElementType.TYPE})")
				.contains("@Retention(RetentionPolicy.RUNTIME)")
				.contains("public @interface A {")
				.contains("float f();")
				.contains("int i();");
	}
}
