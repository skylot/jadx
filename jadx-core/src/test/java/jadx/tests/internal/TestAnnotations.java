package jadx.tests.internal;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestAnnotations extends InternalJadxTest {

	public static class TestCls {
		private static @interface A {
			int a();
		}

		@A(a = -1)
		public void method1() {
		}

		private static @interface V {
			boolean value();
		}

		@V(false)
		public void method2() {
		}

		private static @interface D {
			float value() default 1.1f;
		}

		@D
		public void method3() {
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("@A(a = 255)")));
		assertThat(code, containsString("@A(a = -1)"));
		assertThat(code, containsString("@V(false)"));
		assertThat(code, not(containsString("@D()")));
	}
}
