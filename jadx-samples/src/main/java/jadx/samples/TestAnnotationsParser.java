package jadx.samples;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class TestAnnotationsParser extends AbstractTest {

	@Target({ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface A {
		int i();

		float f();
	}

	@A(i = -1, f = C1.FLOAT_CONST)
	public static class C1 {
		public static final float FLOAT_CONST = 3.14f;
	}

	@A(i = -1025, f = C2.FLOAT_CONST)
	public static class C2 {
		public static final float FLOAT_CONST = 0xFF0000;
	}

	public boolean testRun() {
		A c1 = C1.class.getAnnotation(A.class);
		assertEquals(c1.i(), -1);
		assertEquals(c1.f(), C1.FLOAT_CONST);

		A c2 = C2.class.getAnnotation(A.class);
		assertEquals(c2.i(), -1025);
		assertEquals(c2.f(), C2.FLOAT_CONST);
		return true;
	}

	public static void main(String[] args) throws Exception {
		new TestAnnotationsParser().testRun();
	}
}
