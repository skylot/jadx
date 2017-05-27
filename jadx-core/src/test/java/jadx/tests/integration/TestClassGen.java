package jadx.tests.integration;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestClassGen extends IntegrationTest {

	public static class TestCls {
		public interface I {
			int test();

			public int test3();
		}

		public static abstract class A {
			public abstract int test2();
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("public interface I {"));
		assertThat(code, containsString(indent(2) + "int test();"));
		assertThat(code, not(containsString("public int test();")));
		assertThat(code, containsString(indent(2) + "int test3();"));

		assertThat(code, containsString("public static abstract class A {"));
		assertThat(code, containsString(indent(2) + "public abstract int test2();"));
	}
}
