package jadx.tests.integration.debuginfo;

import org.junit.Test;

import jadx.core.codegen.CodeWriter;
import jadx.core.dex.attributes.nodes.LineAttrNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class TestLineNumbers extends IntegrationTest {

	public static class TestCls {
		int field;

		public void func() {
		}

		public static class Inner {
			int innerField;

			public void innerFunc() {
			}

			public void innerFunc2() {
				new Runnable() {
					@Override
					public void run() {
					}
				}.run();
			}

			public void innerFunc3() {
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		FieldNode field = cls.searchFieldByName("field");
		MethodNode func = cls.searchMethodByName("func()V");
		ClassNode inner = cls.getInnerClasses().get(0);
		MethodNode innerFunc = inner.searchMethodByName("innerFunc()V");
		MethodNode innerFunc2 = inner.searchMethodByName("innerFunc2()V");
		MethodNode innerFunc3 = inner.searchMethodByName("innerFunc3()V");
		FieldNode innerField = inner.searchFieldByName("innerField");

		// check source lines (available only for instructions and methods)
		int testClassLine = 18;
		assertEquals(testClassLine + 3, func.getSourceLine());
		assertEquals(testClassLine + 9, innerFunc.getSourceLine());
		assertEquals(testClassLine + 12, innerFunc2.getSourceLine());
		assertEquals(testClassLine + 20, innerFunc3.getSourceLine());

		// check decompiled lines
		String[] lines = code.split(CodeWriter.NL);
		checkLine(lines, field, "int field;");
		checkLine(lines, func, "public void func() {");
		checkLine(lines, inner, "public static class Inner {");
		checkLine(lines, innerField, "int innerField;");
		checkLine(lines, innerFunc, "public void innerFunc() {");
		checkLine(lines, innerFunc2, "public void innerFunc2() {");
		checkLine(lines, innerFunc3, "public void innerFunc3() {");
	}

	private static void checkLine(String[] lines, LineAttrNode node, String str) {
		int lineNumber = node.getDecompiledLine();
		String line = lines[lineNumber - 1];
		assertThat(line, containsString(str));
	}
}
