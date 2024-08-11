package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

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
		assertThat(getClassNodeFromSmaliFiles("others", "TestBadMethodAccessModifiers", "TestCls"))
				.code()
				.doesNotContain("protected void test() {")
				.containsOne("public void test() {");
	}
}
