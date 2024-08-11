package jadx.tests.integration.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;
import static java.lang.Thread.State.TERMINATED;
import static org.assertj.core.api.Assertions.assertThat;

public class TestAnnotationsMix extends IntegrationTest {

	public static class TestCls {

		public boolean test() throws Exception {
			Class<?> cls = TestCls.class;
			new Thread();

			Method err = cls.getMethod("error");
			assertThat(err.getExceptionTypes().length > 0).isTrue();
			assertThat(err.getExceptionTypes()[0]).isSameAs(Exception.class);

			Method d = cls.getMethod("depr", String[].class);
			assertThat(d.getAnnotations().length > 0).isTrue();
			assertThat(d.getAnnotations()[0].annotationType()).isSameAs(Deprecated.class);

			Method ma = cls.getMethod("test", String[].class);
			assertThat(ma.getAnnotations().length > 0).isTrue();
			MyAnnotation a = (MyAnnotation) ma.getAnnotations()[0];
			assertThat(a.num()).isEqualTo(7);
			assertThat(a.state()).isSameAs(TERMINATED);
			return true;
		}

		@Deprecated
		public int a;

		public void error() throws Exception {
			throw new Exception("error");
		}

		@Deprecated
		public static Object depr(String[] a) {
			return Arrays.asList(a);
		}

		@MyAnnotation(
				name = "b",
				num = 7,
				cls = Exception.class,
				doubles = { 0.0, 1.1 },
				value = 9.87f,
				simple = @SimpleAnnotation(false)
		)
		public static Object test(String[] a) {
			return Arrays.asList(a);
		}

		@Documented
		@Retention(RetentionPolicy.RUNTIME)
		@Target(ElementType.METHOD)
		public @interface MyAnnotation {
			String name() default "a";

			String str() default "str";

			int num();

			float value();

			double[] doubles();

			Class<?> cls();

			SimpleAnnotation simple();

			Thread.State state() default Thread.State.TERMINATED;
		}

		public @interface SimpleAnnotation {
			boolean value();
		}

		public void check() throws Exception {
			test();
		}
	}

	@Test
	public void test() {
		// useDexInput();
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("int i = false;");
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		getClassNode(TestCls.class);
	}

	@Test
	public void testDeclaration() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("Thread thread = new Thread();")
				.contains("new Thread();");
	}
}
