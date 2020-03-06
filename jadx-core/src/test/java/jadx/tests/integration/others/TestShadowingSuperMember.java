package jadx.tests.integration.others;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;
import org.junit.jupiter.api.Test;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestShadowingSuperMember extends SmaliTest {
	// @formatter:off
	/*

		public class C {
			public C(String s) {
			}
		}

		public class A {
			public int a;
			public A(String s) {
			}
		}

		public class B extends A {
			public C a;
			public B(String str) {
				super(str);
			}

			public int add(int b) {
				return super.a + b;
			}
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		allowWarnInCode();
		ClassNode cls = getClassNodeFromSmaliFiles("B");
		String code = cls.getCode().toString();

		assertThat(code, containsOne("return super.A00 + "));
	}
}
