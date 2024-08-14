package jadx.tests.api.utils;

import org.junit.jupiter.api.extension.ExtendWith;

import jadx.NotYetImplementedExtension;
import jadx.api.CommentsLevel;
import jadx.api.JadxArgs;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttributeNode;
import jadx.core.dex.attributes.nodes.JadxCommentsAttr;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.Utils;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@ExtendWith(NotYetImplementedExtension.class)
public class TestUtils {

	public static String indent() {
		return JadxArgs.DEFAULT_INDENT_STR;
	}

	public static String indent(int indent) {
		if (indent == 1) {
			return JadxArgs.DEFAULT_INDENT_STR;
		}
		return Utils.strRepeat(JadxArgs.DEFAULT_INDENT_STR, indent);
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
		assertThat(hasErrors(cls, allowWarnInCode)).as("Inconsistent cls: " + cls).isFalse();
		for (MethodNode mthNode : cls.getMethods()) {
			if (hasErrors(mthNode, allowWarnInCode)) {
				fail("Method with problems: " + mthNode
						+ "\n " + Utils.listToString(mthNode.getAttributesStringsList(), "\n "));
			}
		}
		if (!cls.contains(AFlag.DONT_GENERATE)) {
			assertThat(cls)
					.code()
					.doesNotContain("inconsistent")
					.doesNotContain("JADX ERROR");
		}
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
