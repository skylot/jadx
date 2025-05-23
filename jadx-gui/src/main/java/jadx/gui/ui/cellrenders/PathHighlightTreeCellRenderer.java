package jadx.gui.ui.cellrenders;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import jadx.gui.treemodel.JNode;
import jadx.gui.utils.UiUtils;

public class PathHighlightTreeCellRenderer extends DefaultTreeCellRenderer {

	private final boolean isDarkTheme;

	public PathHighlightTreeCellRenderer() {
		super();
		Color themeBackground = UIManager.getColor("Panel.background");
		isDarkTheme = UiUtils.isDarkTheme(themeBackground);
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
			boolean expanded, boolean leaf, int row, boolean hasFocus) {

		Component comp = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

		DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
		Object userObject = node.getUserObject();

		// Calculate the node level
		int level = node.getLevel();
		// Set different colors according to the level
		float hue = (level * 0.1f) % 1.0f; // Hue cycle
		Color levelColor = Color.getHSBColor(hue, 0.1f, 0.95f);

		// Check if it is on the selected path
		boolean onSelectionPath = false;
		TreePath selectionPath = tree.getSelectionPath();
		if (selectionPath != null) {
			// Check if the current node is on the selected path (whether it is part of the selected path)
			Object[] selectedPathNodes = selectionPath.getPath();
			for (Object pathNode : selectedPathNodes) {
				if (pathNode == node) {
					onSelectionPath = true;
					break;
				}
			}
		}

		if (onSelectionPath && !selected) {
			// If it is on the selected path but not the selected node, use a special foreground
			setForeground(isDarkTheme ? Color.decode("#70AEFF") : Color.decode("#0033B3"));
		} else if (!selected) {
			// Only apply the background color when it is not selected
			setBackground(levelColor);
			// Normal border
			setBorder(BorderFactory.createEmptyBorder(2, level * 2 + 1, 2, 1));
		} else {
			// The selected node also adds a border
			setBorder(BorderFactory.createEmptyBorder(2, level * 2 + 1, 2, 1));
		}

		if (userObject instanceof JNode) {
			JNode jNode = (JNode) userObject;
			setText(jNode.makeLongString());
			setIcon(jNode.getIcon());
			setToolTipText(jNode.getTooltip());
		}
		return comp;
	}

}
