package jadx.tests.integration.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestAnnotationsRenameDef extends IntegrationTest {

	public static class TestCls {

		@Target(ElementType.METHOD)
		@Retention(RetentionPolicy.RUNTIME)
		public @interface A {
			int value();
		}

		@A(5)
		void test() {
		}
	}

	@Test
	public void test() {
		enableDeobfuscation();
		// force rename 'value' method
		args.setDeobfuscationMinLength(20);

		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("public @interface "));
		assertThat(code, not(containsString("int value();")));
		assertThat(code, not(containsString("(5)")));
	}
}
