package jadx.tests.integration.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestAnnotationsRename extends IntegrationTest {

	public static class TestCls {

		@Target(ElementType.METHOD)
		@Retention(RetentionPolicy.RUNTIME)
		public @interface A {
			int x();
		}

		@A(x = 5)
		void test() {
		}

		public void check() throws NoSuchMethodException {
			Method test = TestCls.class.getDeclaredMethod("test");
			A annotation = test.getAnnotation(A.class);
			assertThat(annotation.x()).isEqualTo(5);
		}
	}

	@Test
	public void test() {
		enableDeobfuscation();
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("public @interface ")
				.doesNotContain("(x = 5)");
	}
}
