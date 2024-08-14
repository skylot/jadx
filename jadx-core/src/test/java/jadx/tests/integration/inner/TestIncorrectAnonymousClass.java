package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestIncorrectAnonymousClass extends SmaliTest {

	// @formatter:off
	/*
		public static class TestCls {
			public final class 1 {
				public void invoke() {
					new 1(); // cause infinite self inline
				}
			}

			public void test() {
				new 1();
			}
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		assertThat(getClassNodeFromSmaliFiles("TestCls"))
				.code()
				.containsOne("public final class AnonymousClass1 {")
				.countString(2, "new AnonymousClass1();");
	}
}
