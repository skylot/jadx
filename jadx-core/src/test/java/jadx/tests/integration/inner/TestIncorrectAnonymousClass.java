package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static jadx.tests.api.utils.JadxMatchers.countString;
import static org.hamcrest.MatcherAssert.assertThat;

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
		ClassNode cls = getClassNodeFromSmaliFiles("TestCls");
		String code = cls.getCode().toString();

		assertThat(code, containsOne("public final class AnonymousClass1 {"));
		assertThat(code, countString(2, "new AnonymousClass1();"));
	}
}
