package jadx.tests.integration.others;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import jadx.api.data.ICodeComment;
import jadx.api.data.IJavaCodeRef;
import jadx.api.data.IJavaNodeRef.RefType;
import jadx.api.data.impl.JadxCodeComment;
import jadx.api.data.impl.JadxCodeData;
import jadx.api.data.impl.JadxCodeRef;
import jadx.api.data.impl.JadxNodeRef;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestCodeComments extends IntegrationTest {

	@SuppressWarnings("FieldCanBeLocal")
	public static class TestCls {
		private int intField = 5;

		public static class A {
		}

		public int test() {
			System.out.println("Hello");
			System.out.println("comment");
			return intField;
		}
	}

	@Test
	public void test() {
		String baseClsId = TestCls.class.getName();
		ICodeComment clsComment = new JadxCodeComment(JadxNodeRef.forCls(baseClsId), "class comment");
		ICodeComment innerClsComment = new JadxCodeComment(JadxNodeRef.forCls(baseClsId + "$A"), "inner class comment");
		ICodeComment fldComment = new JadxCodeComment(new JadxNodeRef(RefType.FIELD, baseClsId, "intField:I"), "field comment");
		JadxNodeRef mthRef = new JadxNodeRef(RefType.METHOD, baseClsId, "test()I");
		ICodeComment mthComment = new JadxCodeComment(mthRef, "method comment");
		IJavaCodeRef insnRef = JadxCodeRef.forInsn(isJavaInput() ? 13 : 11);
		ICodeComment insnComment = new JadxCodeComment(mthRef, insnRef, "insn comment");

		JadxCodeData codeData = new JadxCodeData();
		getArgs().setCodeData(codeData);
		codeData.setComments(Arrays.asList(clsComment, innerClsComment, fldComment, mthComment, insnComment));

		ClassNode cls = getClassNode(TestCls.class);
		assertThat(cls)
				.decompile()
				.checkCodeOffsets()
				.code()
				.containsOne("// class comment")
				.containsOne("// inner class comment")
				.containsOne("// field comment")
				.containsOne("// method comment")
				.containsOne("System.out.println(\"comment\"); // insn comment");

		String code = cls.getCode().getCodeStr();
		assertThat(cls)
				.reloadCode(this)
				.isEqualTo(code);

		ICodeComment updInsnComment = new JadxCodeComment(mthRef, insnRef, "updated insn comment");
		codeData.setComments(Collections.singletonList(updInsnComment));
		jadxDecompiler.reloadCodeData();
		assertThat(cls)
				.reloadCode(this)
				.containsOne("System.out.println(\"comment\"); // updated insn comment")
				.doesNotContain("class comment")
				.containsOne(" comment");
	}
}
