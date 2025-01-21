package jadx.gui.treemodel;

import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import jadx.gui.utils.ui.NodeLabel;

public class TreeCellRenderer extends DefaultTreeCellRenderer {

	@Override
	public Component getTreeCellRendererComponent(JTree tree,
			Object value, boolean selected, boolean expanded,
			boolean isLeaf, int row, boolean focused) {
		Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, isLeaf, row, focused);
		if (value instanceof JNode) {
			JNode jNode = (JNode) value;
			NodeLabel.disableHtml(this, jNode.disableHtml());
			setText(jNode.makeStringHtml());
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
