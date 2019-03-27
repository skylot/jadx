package jadx.tests.integration.annotations;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestAnnotations extends IntegrationTest {

	public static class TestCls {
		private @interface A {
			int a();
		}

		@A(a = -1)
		public void methodA1() {
		}

		@A(a = -253)
		public void methodA2() {
		}

		@A(a = -11253)
		public void methodA3() {
		}

		private @interface V {
			boolean value();
		}

		@V(false)
		public void methodV() {
		}

		private @interface D {
			float value() default 1.1f;
		}

		@D
		public void methodD() {
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("@A(a = 255)")));
		assertThat(code, containsOne("@A(a = -1)"));
		assertThat(code, containsOne("@A(a = -253)"));
		assertThat(code, containsOne("@A(a = -11253)"));
		assertThat(code, containsOne("@V(false)"));
		assertThat(code, not(containsString("@D()")));
		assertThat(code, containsOne("@D"));

		assertThat(code, containsOne("int a();"));
		assertThat(code, containsOne("float value() default 1.1f;"));
	}
}
