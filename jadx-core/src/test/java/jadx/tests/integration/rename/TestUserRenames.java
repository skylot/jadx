package jadx.tests.integration.rename;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.api.data.CodeRefType;
import jadx.api.data.ICodeRename;
import jadx.api.data.IJavaCodeRef;
import jadx.api.data.IJavaNodeRef.RefType;
import jadx.api.data.impl.JadxCodeData;
import jadx.api.data.impl.JadxCodeRef;
import jadx.api.data.impl.JadxCodeRename;
import jadx.api.data.impl.JadxNodeRef;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestUserRenames extends IntegrationTest {

	@SuppressWarnings({ "FieldCanBeLocal", "FieldMayBeFinal" })
	public static class TestCls {
		private int intField = 5;

		public static class A {
		}

		public int test(int x) {
			int y = x + "test".length();
			System.out.println(y);
			int z = y + 1;
			System.out.println(z);
			return z;
		}
	}

	@Test
	public void test() {
		getArgs().setDeobfuscationOn(false);

		List<ICodeRename> renames = new ArrayList<>();
		String baseClsId = TestCls.class.getName();
		renames.add(new JadxCodeRename(JadxNodeRef.forPkg("jadx.tests"), "renamedPkgTests"));
		renames.add(new JadxCodeRename(JadxNodeRef.forPkg("jadx.tests.integration.rename"), "renamedPkgRename"));
		renames.add(new JadxCodeRename(JadxNodeRef.forCls(baseClsId), "RenamedTestCls"));
		renames.add(new JadxCodeRename(JadxNodeRef.forCls(baseClsId + "$A"), "RenamedInnerCls"));
		renames.add(new JadxCodeRename(new JadxNodeRef(RefType.FIELD, baseClsId, "intField:I"), "renamedField"));
		JadxNodeRef mthRef = new JadxNodeRef(RefType.METHOD, baseClsId, "test(I)I");
		renames.add(new JadxCodeRename(mthRef, "renamedTestMth"));
		renames.add(new JadxCodeRename(mthRef, new JadxCodeRef(CodeRefType.MTH_ARG, 0), "renamedX"));
		JadxCodeRef varDeclareRef = isJavaInput() ? JadxCodeRef.forVar(0, 1) : JadxCodeRef.forVar(0, 0);
		renames.add(new JadxCodeRename(mthRef, varDeclareRef, "renamedY"));
		IJavaCodeRef varUseRef = isJavaInput() ? JadxCodeRef.forVar(0, 4) : JadxCodeRef.forVar(1, 0);
		renames.add(new JadxCodeRename(mthRef, varUseRef, "renamedZ"));

		JadxCodeData codeData = new JadxCodeData();
		codeData.setRenames(renames);
		getArgs().setCodeData(codeData);

		ClassNode cls = getClassNode(TestCls.class);
		assertThat(cls)
				.decompile()
				.checkCodeOffsets()
				.code()
				.containsOne("package jadx.renamedPkgTests.integration.renamedPkgRename;")
				.containsOne("public class RenamedTestCls {")
				.containsOne("private int renamedField")
				.containsOne("public static class RenamedInnerCls {")
				.containsOne("public int renamedTestMth(int renamedX) {")
				.containsOne("int renamedY = renamedX + \"test\".length();")
				.containsOne("int renamedZ = renamedY + 1;")
				.containsOne("return renamedZ;");

		String code = cls.getCode().getCodeStr();
		assertThat(cls)
				.reloadCode(this)
				.isEqualTo(code);

		ICodeRename updVarRename = new JadxCodeRename(mthRef, varUseRef, "anotherZ");
		codeData.setRenames(Collections.singletonList(updVarRename));
		jadxDecompiler.reloadCodeData();
		assertThat(cls)
				.reloadCode(this)
				.containsOne("int anotherZ = y + 1;")
				.doesNotContain("int z")
				.doesNotContain("int renamedZ");
	}
}
