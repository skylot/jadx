package jadx.tests.integration.types;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class TestTypeResolver5 extends SmaliTest {
	/*
	  Smali Code equivalent:
		public static class TestCls {
			public int test1(int a) {
				return ~a;
			}

			public long test2(long b) {
				return ~b;
			}
		}
	*/

	@Test
	public void test() {
		disableCompilation();

		ClassNode cls = getClassNodeFromSmaliWithPath("types", "TestTypeResolver5");
		String code = cls.getCode().toString();

//		assertThat(code, containsString("return ~a;"));
//		assertThat(code, containsString("return ~b;"));
		assertThat(code, not(containsString("Object string2")));
	}
}
