package jadx.tests.integration.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class AnnotationsRenaming extends IntegrationTest {

	public static class TestCls {

		@Target({ElementType.METHOD})
		@Retention(RetentionPolicy.RUNTIME)
		public static @interface A {
			int x();
		}

		@A(x = 5)
		void test() {
		}

	}

	@Test
	@NotYetImplemented
	public void test504() {
		enableDeobfuscation();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("public static @interface "));
		assertThat(code, not(containsString("(x = 5)")));
	}
}
