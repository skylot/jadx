package jadx.tests.integration.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestParamAnnotations extends IntegrationTest {

	public static class TestCls {

		@Target(ElementType.PARAMETER)
		@Retention(RetentionPolicy.RUNTIME)
		public static @interface A {
			int i() default 7;
		}

		void test1(@A int i) {
		}

		void test2(int i, @A int j) {
		}

		void test3(@A(i = 5) int i) {
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("void test1(@A int i) {"));
		assertThat(code, containsString("void test2(int i, @A int j) {"));
		assertThat(code, containsString("void test3(@A(i = 5) int i) {"));
	}
}
