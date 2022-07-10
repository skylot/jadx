package jadx.tests.integration.debuginfo;

import org.junit.jupiter.api.Test;

import jadx.api.ICodeInfo;
import jadx.api.ICodeWriter;
import jadx.api.utils.CodeUtils;
import jadx.core.dex.attributes.nodes.LineAttrNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
		printLineNumbers();

		ClassNode cls = getClassNode(TestCls.class);
		ICodeInfo codeInfo = cls.getCode();
		String[] lines = codeInfo.getCodeStr().split(ICodeWriter.NL);

		MethodNode test1 = cls.searchMethodByShortId("test1(Z)I");
		checkLine(lines, codeInfo, test1, 3, "return 1;");

		MethodNode test2 = cls.searchMethodByShortId("test2(I)I");
		checkLine(lines, codeInfo, test2, 3, "return v - 1;");
		checkLine(lines, codeInfo, test2, 6, "return v + 1;");

		MethodNode test3 = cls.searchMethodByShortId("test3(I)I");
		if (isJavaInput()) { // dx lost line number for this return
			checkLine(lines, codeInfo, test3, 3, "return v;");
		}
		checkLine(lines, codeInfo, test3, 6, "return v + 1;");
	}

	private static void checkLine(String[] lines, ICodeInfo cw, LineAttrNode node, int offset, String str) {
		int nodeDefLine = CodeUtils.getLineNumForPos(cw.getCodeStr(), node.getDefPosition());
		int decompiledLine = nodeDefLine + offset;
		assertThat(lines[decompiledLine - 1], containsOne(str));
		Integer sourceLine = cw.getCodeMetadata().getLineMapping().get(decompiledLine);
		assertNotNull(sourceLine);
		assertEquals(node.getSourceLine() + offset, (int) sourceLine);
	}
}
