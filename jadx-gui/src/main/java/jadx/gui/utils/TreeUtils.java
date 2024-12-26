package jadx.gui.utils;

import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JTree;
import javax.swing.tree.TreeNode;

import org.jetbrains.annotations.Nullable;

import jadx.gui.treemodel.JNode;

public class TreeUtils {

	@Nullable
	public static JNode getJNodeUnderMouse(JTree tree, MouseEvent mouseEvent) {
		TreeNode treeNode = UiUtils.getTreeNodeUnderMouse(tree, mouseEvent);
		if (treeNode instanceof JNode) {
			return (JNode) treeNode;
		}

		return null;
	}

	public static void expandAllNodes(JTree tree) {
		int j = tree.getRowCount();
		int i = 0;
		while (i < j) {
			tree.expandRow(i);
			i += 1;
			j = tree.getRowCount();
		}
	}

	public static String highlightString(String s, String styleTag, List<Integer> ranges) {
		StringBuilder sb = new StringBuilder("<html><style>.highlight {");
		sb.append(styleTag);
		sb.append("}</style><body>");
		int lastIndex = 0;
		for (int i = 0; i < ranges.size(); i += 2) {
			int rangeStart = ranges.get(i);
			int rangeEnd = ranges.get(i + 1);
			appendString2html(sb, s.substring(lastIndex, rangeStart));
			sb.append("<span class=\"highlight\">");
			appendString2html(sb, s.substring(rangeStart, rangeEnd));
			sb.append("</span>");
			lastIndex = rangeEnd;
		}
		appendString2html(sb, s.substring(lastIndex));
		sb.append("</body></html>");
		return sb.toString();
	}

	public static void appendString2html(StringBuilder sb, String s) {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			String r;
			switch (c) {
				case '"':
					r = "&quot;";
					break;
				// case '\'': r = "&apos;"; break;
				case '&':
					r = "&amp;";
					break;
				case '<':
					r = "&lt;";
					break;
				case '>':
					r = "&gt;";
					break;
				case ' ':
					r = "&nbsp;"; // Maintain amount of whitespace in line
					break;
				default:
					r = String.valueOf(c);
					break;
			}
			sb.append(r);
		}
	}

}
