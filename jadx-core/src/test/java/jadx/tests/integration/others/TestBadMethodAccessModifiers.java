package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

public class TestBadMethodAccessModifiers extends SmaliTest {
	// @formatter:off
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
	// @formatter:on

	@Test
	public void test() {
		ClassNode cls = getClassNodeFromSmaliFiles("others", "TestBadMethodAccessModifiers", "TestCls");
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("protected void test() {")));
		assertThat(code, containsOne("public void test() {"));
	}
}
