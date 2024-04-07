package jadx.tests.integration.code;

import org.junit.jupiter.api.Test;

import jadx.api.data.CommentStyle;
import jadx.api.data.IJavaNodeRef;
import jadx.api.data.impl.JadxCodeComment;
import jadx.api.data.impl.JadxNodeRef;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestCodeCommentStyle extends IntegrationTest {

	@SuppressWarnings("unused")
	public static class TestCls {
		public int aSingleLine;
		public int aMultiLine;

		public int block;
		public int blockMulti;
		public int blockCondensed;
		public int blockCondensedMulti;

		public int javaDoc;
		public int javaDocMulti;
		public int javaDocCondensed;
		public int javaDocCondensedMulti;
	}

	@Test
	public void test() {
		addFldComment("aSingleLine", "Test line comment", CommentStyle.LINE);
		addFldComment("aMultiLine", "Test multi\nline comment", CommentStyle.LINE);

		addFldComment("block", "Test block comment", CommentStyle.BLOCK);
		addFldComment("blockMulti", "Test multi\nline block comment", CommentStyle.BLOCK);
		addFldComment("blockCondensed", "Test condensed block comment", CommentStyle.BLOCK_CONDENSED);
		addFldComment("blockCondensedMulti", "Test condensed multi\nline block comment", CommentStyle.BLOCK_CONDENSED);

		addFldComment("javaDoc", "Test javaDoc comment", CommentStyle.JAVADOC);
		addFldComment("javaDocMulti", "Test multi\nline javaDoc comment", CommentStyle.JAVADOC);
		addFldComment("javaDocCondensed", "Test condensed javaDoc comment", CommentStyle.JAVADOC_CONDENSED);
		addFldComment("javaDocCondensedMulti", "Test condensed multi\nline javaDoc comment", CommentStyle.JAVADOC_CONDENSED);

		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("// Test line comment")
				.containsOne("/* Test condensed block comment */");
	}

	private void addFldComment(String fldName, String comment, CommentStyle style) {
		String clsName = "jadx.tests.integration.code.TestCodeCommentStyle$TestCls";
		JadxNodeRef fldRef = new JadxNodeRef(IJavaNodeRef.RefType.FIELD, clsName, fldName + ":I");
		getCodeData().getComments().add(new JadxCodeComment(fldRef, comment, style));
	}
}
