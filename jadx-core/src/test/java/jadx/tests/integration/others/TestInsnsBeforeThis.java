package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

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
		ClassNode cls = getClassNodeFromSmali();
		String code = cls.getCode().toString();

		assertThat(code, containsOne("checkNull(str);"));
	}
}
