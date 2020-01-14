package jadx.tests.integration.inner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestReplaceConstsInAnnotations extends IntegrationTest {

	public static class TestCls {
		@Target(ElementType.TYPE)
		@Retention(RetentionPolicy.RUNTIME)
		public @interface A {
			int i();

			float f();
		}

		@A(i = -1, f = C.FLOAT_CONST)
		public static class C {
			public static final float FLOAT_CONST = 3.14f;
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOnlyOnce("f = C.FLOAT_CONST");
	}
}
