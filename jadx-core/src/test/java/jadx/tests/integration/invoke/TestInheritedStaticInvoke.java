package jadx.tests.integration.invoke;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestInheritedStaticInvoke extends IntegrationTest {

	public static class TestCls {
		public static class A {
			public static int a() {
				return 1;
			}
		}

		public static class B extends A {
		}

		public int test() {
			return B.a(); // not A.a()
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("return B.a();"));
	}
}
