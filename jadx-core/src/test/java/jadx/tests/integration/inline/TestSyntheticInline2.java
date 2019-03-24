package jadx.tests.integration.inline;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestSyntheticInline2 extends IntegrationTest {

	public static class Base {
		protected void call() {
			System.out.println("base call");
		}
	}

	public static class TestCls extends Base {
		public class A {
			public void invokeCall() {
				TestCls.this.call();
			}

			public void invokeSuperCall() {
				TestCls.super.call();
			}
		}

		@Override
		public void call() {
			System.out.println("TestCls call");
		}

		public void check() {
			A a = new A();
			a.invokeSuperCall();
			a.invokeCall();
		}
	}

	@Test
	public void test() {
		disableCompilation(); // strange java compiler bug
		ClassNode cls = getClassNode(TestCls.class); // Base class in unknown
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("synthetic")));
		assertThat(code, not(containsString("access$")));
		assertThat(code, containsString("TestSyntheticInline2$TestCls.this.call();"));
		assertThat(code, containsString("TestSyntheticInline2$TestCls.super.call();"));
	}

	@Test
	public void testTopClass() {
		ClassNode cls = getClassNode(TestSyntheticInline2.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString(indent(1) + "TestCls.super.call();"));
	}
}
