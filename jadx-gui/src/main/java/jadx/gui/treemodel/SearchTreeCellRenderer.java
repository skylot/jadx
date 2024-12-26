package jadx.gui.treemodel;

import java.awt.*;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;

public class SearchTreeCellRenderer extends DefaultTreeCellRenderer {

	@Override
	public Component getTreeCellRendererComponent(JTree tree,
			Object value, boolean selected, boolean expanded,
			boolean isLeaf, int row, boolean focused) {
		Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, isLeaf, row, focused);
		if (value instanceof JNode) {
			JNode jNode = (JNode) value;
			setText(jNode.isHasHighlight() ? jNode.makeHighlightHtml() : jNode.makeLongStringHtml());
			setIcon(jNode.getIcon());
			setToolTipText(jNode.getTooltip());
		} else {
			setToolTipText(null);
		}
		if (value instanceof JPackage) {
			setEnabled(((JPackage) value).isEnabled());
		}
		return c;
	}
}
