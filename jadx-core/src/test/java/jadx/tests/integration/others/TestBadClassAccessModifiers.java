package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestBadClassAccessModifiers extends SmaliTest {
	// @formatter:off
	/*
		// class others.A
		public class A {
			public void call() {
				B.BB.BBB.test();
			}
		}

		// class others.B
		public class B {
			private static class BB {
				public static class BBB {
					public static void test() {
					}
				}
			}
		}
	*/

	@Test
	public void test() {
		assertThat(getClassNodeFromSmaliFiles("A"))
				.code();
	}
}
