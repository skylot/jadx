package jadx.tests.integration.inner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestReplaceConstsInAnnotations2 extends IntegrationTest {

	public static class TestCls {
		@Target(ElementType.TYPE)
		@Retention(RetentionPolicy.RUNTIME)
		public @interface A {
			int[] value();
		}

		@A(C.INT_CONST)
		public static class C {
			public static final int INT_CONST = 23412342;
		}

		@A({ C.INT_CONST, C2.INT_CONST })
		public static class C2 {
			public static final int INT_CONST = 34563456;
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				// .containsOne("@A(C.INT_CONST)") // TODO: remove brackets for single element
				.containsOne("@A({C.INT_CONST}")
				.containsOne("@A({C.INT_CONST, C2.INT_CONST})")
				.containsOne("23412342")
				.containsOne("34563456");
	}
}
