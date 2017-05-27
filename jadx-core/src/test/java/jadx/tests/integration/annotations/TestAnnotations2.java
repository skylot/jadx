package jadx.tests.integration.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class TestAnnotations2 extends IntegrationTest {

	public static class TestCls {

		@Target({ElementType.TYPE})
		@Retention(RetentionPolicy.RUNTIME)
		public @interface A {
			int i();

			float f();
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("@Target({ElementType.TYPE})"));
		assertThat(code, containsString("@Retention(RetentionPolicy.RUNTIME)"));
		assertThat(code, containsString("public @interface A {"));
		assertThat(code, containsString("float f();"));
		assertThat(code, containsString("int i();"));
	}
}
