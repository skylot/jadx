package jadx.gui.settings.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import jadx.gui.utils.NLS;

public class SettingsTree extends JTree {

	public void init(JPanel groupPanel, List<SettingsGroupPanel> groups) {
		DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode(NLS.str("preferences.title"));
		for (SettingsGroupPanel group : groups) {
			treeRoot.add(new DefaultMutableTreeNode(group));
		}
		setModel(new DefaultTreeModel(treeRoot));
		getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		setFocusable(false);
		addTreeSelectionListener(e -> {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) getLastSelectedPathComponent();
			Object obj = node.getUserObject();
			groupPanel.removeAll();
			if (obj instanceof SettingsGroupPanel) {
				SettingsGroupPanel panel = (SettingsGroupPanel) obj;
				groupPanel.add(panel);
			}
			groupPanel.updateUI();
		});
		// expand all nodes and disallow collapsing
		setNodeExpandedState(this, treeRoot, true);
		addTreeWillExpandListener(new TreeWillExpandListener() {
			@Override
			public void treeWillExpand(TreeExpansionEvent event) {
			}

			@Override
			public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
				throw new ExpandVetoException(event, "Collapsing tree not allowed");
			}
		});
		addSelectionRow(1);
	}

	private static void setNodeExpandedState(JTree tree, TreeNode node, boolean expanded) {
		ArrayList<? extends TreeNode> list = Collections.list(node.children());
		for (TreeNode treeNode : list) {
			setNodeExpandedState(tree, treeNode, expanded);
		}
		DefaultMutableTreeNode mutableTreeNode = (DefaultMutableTreeNode) node;
		if (!expanded && mutableTreeNode.isRoot()) {
			return;
		}
		TreePath path = new TreePath(mutableTreeNode.getPath());
		if (expanded) {
			tree.expandPath(path);
		} else {
			tree.collapsePath(path);
		}
	}
}
