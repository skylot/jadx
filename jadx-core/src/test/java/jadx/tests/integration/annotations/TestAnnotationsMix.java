package jadx.tests.integration.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestAnnotationsMix extends IntegrationTest {

	public static class TestCls {

		public boolean test() throws Exception {
			Class<?> cls = TestCls.class;
			new Thread();

			Method err = cls.getMethod("error");
			assertTrue(err.getExceptionTypes().length > 0);
			assertSame(err.getExceptionTypes()[0], Exception.class);

			Method d = cls.getMethod("depr", String[].class);
			assertTrue(d.getAnnotations().length > 0);
			assertSame(d.getAnnotations()[0].annotationType(), Deprecated.class);

			Method ma = cls.getMethod("test", String[].class);
			assertTrue(ma.getAnnotations().length > 0);
			MyAnnotation a = (MyAnnotation) ma.getAnnotations()[0];
			assertEquals(7, a.num());
			assertSame(Thread.State.TERMINATED, a.state());
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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("int i = false;")));
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		getClassNode(TestCls.class);
	}

	@Test
	public void testDeclaration() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("Thread thread = new Thread();")));
		assertThat(code, containsString("new Thread();"));
	}
}
