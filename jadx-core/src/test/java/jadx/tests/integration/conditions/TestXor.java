package jadx.tests.integration.conditions;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

public class TestXor extends SmaliTest {

	public static class TestCls {
		public boolean test1() {
			return test() ^ true;
		}

		public boolean test2(boolean v) {
			return v ^ true;
		}

		public boolean test() {
			return true;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("return !test();"));
		assertThat(code, containsOne("return !v;"));
	}

	@Test
	public void smali() {
		/*
    		public boolean test1() {
				return test() ^ true;
			}

			public boolean test2() {
				return test() ^ false;
			}

			public boolean test() {
				return true;
			}
		 */
		ClassNode cls = getClassNodeFromSmaliWithPath("conditions", "TestXor");
		String code = cls.getCode().toString();

		assertThat(code, containsOne("return !test();"));
		assertThat(code, containsOne("return test();"));
	}

}
