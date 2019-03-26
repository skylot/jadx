package jadx.tests.integration.invoke;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.countString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestSuperInvoke extends IntegrationTest {

	public class A {
		public int a() {
			return 1;
		}
	}

	public class B extends A {
		@Override
		public int a() {
			return super.a() + 2;
		}

		public int test() {
			return a();
		}
	}

	public void check() {
		assertEquals(3, new B().test());
	}

	@Test
	public void test() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestSuperInvoke.class);
		String code = cls.getCode().toString();

		assertThat(code, countString(2, "return super.a() + 2;"));
	}
}
