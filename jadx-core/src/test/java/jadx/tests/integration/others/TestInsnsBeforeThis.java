package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestInsnsBeforeThis extends SmaliTest {
	// @formatter:off
	/*
		public class A {
			public A(String str) {
				checkNull(str);
				this(str.length());
			}

			public A(int i) {
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
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("checkNull(str);");
	}
}
