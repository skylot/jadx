package jadx.tests.integration.others;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestDefConstructorWithAnnotation extends IntegrationTest {

	public static class TestCls {
		@AnnotationTest
		public TestCls() {
		}

		@Target(ElementType.CONSTRUCTOR)
		@Retention(RetentionPolicy.RUNTIME)
		public @interface AnnotationTest {
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("@AnnotationTest");
	}
}
