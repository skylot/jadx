package jadx.tests.integration.debuginfo;

import org.junit.Test;

import jadx.core.codegen.CodeWriter;
import jadx.core.dex.attributes.nodes.LineAttrNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class TestReturnSourceLine extends IntegrationTest {

	public static class TestCls {
		public int test1(boolean v) {
			if (v) {
				f();
				return 1;
			}
			f();
			return 0;
		}

		public int test2(int v) {
			if (v == 0) {
				f();
				return v - 1;
			}
			f();
			return v + 1;
		}

		public int test3(int v) {
			if (v == 0) {
				f();
				return v;
			}
			f();
			return v + 1;
		}

		private void f() {
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		CodeWriter codeWriter = cls.getCode();
		String code = codeWriter.toString();
		String[] lines = code.split(CodeWriter.NL);

		MethodNode test1 = cls.searchMethodByName("test1(Z)I");
		checkLine(lines, codeWriter, test1, 3, "return 1;");

		MethodNode test2 = cls.searchMethodByName("test2(I)I");
		checkLine(lines, codeWriter, test2, 3, "return v - 1;");

//		TODO:
//		MethodNode test3 = cls.searchMethodByName("test3(I)I");
//		checkLine(lines, codeWriter, test3, 3, "return v;");
	}

	private static void checkLine(String[] lines, CodeWriter cw, LineAttrNode node, int offset, String str) {
		int decompiledLine = node.getDecompiledLine() + offset;
		assertThat(lines[decompiledLine - 1], containsOne(str));
		Integer sourceLine = cw.getLineMapping().get(decompiledLine);
		assertNotNull(sourceLine);
		assertEquals(node.getSourceLine() + offset, (int) sourceLine);
	}
}
