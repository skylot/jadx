package jadx.tests.integration.others;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class TestBadMethodAccessModifiers extends SmaliTest {
	/*
		public static class TestCls {

			public abstract class A {
				public abstract void test();
			}

			public class B extends A {
				protected void test() {
				}
			}
		}
	*/
	@Test
	public void test() {
		ClassNode cls = getClassNodeFromSmaliFiles("others", "TestBadMethodAccessModifiers", "TestCls",
				"TestCls$A", "TestCls$B", "TestCls");
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("protected void test() {")));
		assertThat(code, containsOne("public void test() {"));
	}
}
