package jadx.tests.api.utils;

import org.junit.jupiter.api.extension.ExtendWith;

import jadx.NotYetImplementedExtension;
import jadx.api.CommentsLevel;
import jadx.api.ICodeWriter;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttributeNode;
import jadx.core.dex.attributes.nodes.JadxCommentsAttr;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.Utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(NotYetImplementedExtension.class)
public class TestUtils {

	public static String indent() {
		return ICodeWriter.INDENT_STR;
	}

	public static String indent(int indent) {
		if (indent == 1) {
			return ICodeWriter.INDENT_STR;
		}
		StringBuilder sb = new StringBuilder(indent * ICodeWriter.INDENT_STR.length());
		for (int i = 0; i < indent; i++) {
			sb.append(ICodeWriter.INDENT_STR);
		}
		return sb.toString();
	}

	public static int count(String string, String substring) {
		if (substring == null || substring.isEmpty()) {
			throw new IllegalArgumentException("Substring can't be null or empty");
		}
		int count = 0;
		int idx = 0;
		while ((idx = string.indexOf(substring, idx)) != -1) {
			idx++;
			count++;
		}
		return count;
	}

	protected static void checkCode(ClassNode cls, boolean allowWarnInCode) {
		assertFalse(hasErrors(cls, allowWarnInCode), "Inconsistent cls: " + cls);
		for (MethodNode mthNode : cls.getMethods()) {
			if (hasErrors(mthNode, allowWarnInCode)) {
				fail("Method with problems: " + mthNode
						+ "\n " + Utils.listToString(mthNode.getAttributesStringsList(), "\n "));
			}
		}

		String code = cls.getCode().getCodeStr();
		assertThat(code, not(containsString("inconsistent")));
		assertThat(code, not(containsString("JADX ERROR")));
	}

	protected static boolean hasErrors(IAttributeNode node, boolean allowWarnInCode) {
		if (node.contains(AFlag.INCONSISTENT_CODE) || node.contains(AType.JADX_ERROR)) {
			return true;
		}
		if (!allowWarnInCode) {
			JadxCommentsAttr commentsAttr = node.get(AType.JADX_COMMENTS);
			if (commentsAttr != null) {
				return commentsAttr.getComments().get(CommentsLevel.WARN) != null;
			}
		}
		return false;
	}
}
