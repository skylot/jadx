package jadx.tests.integration.others;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import jadx.api.data.ICodeComment;
import jadx.api.data.IJavaNodeRef.RefType;
import jadx.api.data.impl.JadxCodeComment;
import jadx.api.data.impl.JadxCodeData;
import jadx.api.data.impl.JadxNodeRef;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestCodeComments extends IntegrationTest {

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
		ICodeComment innerClsComment = new JadxCodeComment(JadxNodeRef.forCls(baseClsId + ".A"), "inner class comment");
		ICodeComment fldComment = new JadxCodeComment(new JadxNodeRef(RefType.FIELD, baseClsId, "intField:I"), "field comment");
		JadxNodeRef mthRef = new JadxNodeRef(RefType.METHOD, baseClsId, "test()I");
		ICodeComment mthComment = new JadxCodeComment(mthRef, "method comment");
		ICodeComment insnComment = new JadxCodeComment(mthRef, "insn comment", 11);

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

		ICodeComment updInsnComment = new JadxCodeComment(mthRef, "updated insn comment", 11);
		codeData.setComments(Collections.singletonList(updInsnComment));
		assertThat(cls)
				.reloadCode(this)
				.containsOne("System.out.println(\"comment\"); // updated insn comment")
				.doesNotContain("class comment")
				.containsOne(" comment");
	}
}
