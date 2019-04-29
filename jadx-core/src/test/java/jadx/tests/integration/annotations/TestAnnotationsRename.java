package jadx.tests.integration.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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
			assertThat(annotation.x(), is(5));
		}
	}

	@Test
	public void test() {
		enableDeobfuscation();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("public @interface "));
		assertThat(code, not(containsString("(x = 5)")));
	}
}
