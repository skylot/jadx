package jadx.samples;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;

public class TestAnnotations extends AbstractTest {

	@Deprecated
	public int a;

	public void error() throws Exception {
		throw new Exception("error");
	}

	@Deprecated
	public static Object depr(String[] a) {
		return Arrays.asList(a);
	}

	public @interface SimpleAnnotation {
		boolean value();
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

	@MyAnnotation(name = "b",
			num = 7,
			cls = Exception.class,
			doubles = {0.0, 1.1},
			value = 9.87f,
			simple = @SimpleAnnotation(false))
	public static Object test(String[] a) {
		return Arrays.asList(a);
	}

	public static Object test2(@Deprecated String a, @SimpleAnnotation(value = false) Object b) {
		@Deprecated
		Object c = a;
		return c;
	}

	public @interface ClassesAnnotation {
		Class<?>[] value();
	}

	@ClassesAnnotation({
			int.class, int[].class, int[][][].class,
			String.class, String[].class, String[][].class
	})
	public static Object test3(Object b) {
		return b.toString();
	}

	@Override
	public boolean testRun() throws Exception {
		Class<?> cls = TestAnnotations.class;
		new Thread();

		Method err = cls.getMethod("error");
		assertTrue(err.getExceptionTypes().length > 0);
		assertTrue(err.getExceptionTypes()[0] == Exception.class);

		Method d = cls.getMethod("depr", String[].class);
		assertTrue(d.getAnnotations().length > 0);
		assertTrue(d.getAnnotations()[0].annotationType() == Deprecated.class);

		Method ma = cls.getMethod("test", String[].class);
		assertTrue(ma.getAnnotations().length > 0);
		MyAnnotation a = (MyAnnotation) ma.getAnnotations()[0];
		assertTrue(a.num() == 7);
		assertTrue(a.state() == Thread.State.TERMINATED);
		return true;
	}

	public static void main(String[] args) throws Exception {
		new TestAnnotations().testRun();
	}
}
