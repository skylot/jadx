package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestInsnsBeforeSuper extends SmaliTest {
	// @formatter:off
	/*
		public class A {
			public A(String s) {
			}
		}

		public class B extends A {
			public B(String str) {
				checkNull(str);
				super(str);
			}

			public void checkNull(Object o) {
				if (o == null) {
					throw new NullPointerException();
				}
			}
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		allowWarnInCode();
		assertThat(getClassNodeFromSmaliFiles("B"))
				.code()
				.containsOne("checkNull(str);");
	}
}
