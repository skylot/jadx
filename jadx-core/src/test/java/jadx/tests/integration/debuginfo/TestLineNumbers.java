package jadx.tests.integration.debuginfo;

import org.junit.jupiter.api.Test;

import jadx.api.utils.CodeUtils;
import jadx.core.dex.attributes.nodes.LineAttrNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
		printLineNumbers();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		FieldNode field = cls.searchFieldByName("field");
		MethodNode func = cls.searchMethodByShortId("func()V");
		ClassNode inner = cls.getInnerClasses().get(0);
		MethodNode innerFunc = inner.searchMethodByShortId("innerFunc()V");
		MethodNode innerFunc2 = inner.searchMethodByShortId("innerFunc2()V");
		MethodNode innerFunc3 = inner.searchMethodByShortId("innerFunc3()V");
		FieldNode innerField = inner.searchFieldByName("innerField");

		// check source lines (available only for instructions and methods)
		int testClassLine = 18;
		assertEquals(testClassLine + 3, func.getSourceLine());
		assertEquals(testClassLine + 9, innerFunc.getSourceLine());
		assertEquals(testClassLine + 12, innerFunc2.getSourceLine());
		assertEquals(testClassLine + 20, innerFunc3.getSourceLine());

		// check decompiled lines
		checkLine(code, field, "int field;");
		checkLine(code, func, "public void func() {");
		checkLine(code, inner, "public static class Inner {");
		checkLine(code, innerField, "int innerField;");
		checkLine(code, innerFunc, "public void innerFunc() {");
		checkLine(code, innerFunc2, "public void innerFunc2() {");
		checkLine(code, innerFunc3, "public void innerFunc3() {");
	}

	private static void checkLine(String code, LineAttrNode node, String str) {
		String line = CodeUtils.getLineForPos(code, node.getDefPosition());
		assertThat(line, containsString(str));
	}
}
