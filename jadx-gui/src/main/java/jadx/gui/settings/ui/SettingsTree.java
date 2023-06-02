package jadx.gui.settings.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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

import jadx.api.plugins.gui.ISettingsGroup;
import jadx.gui.utils.NLS;

public class SettingsTree extends JTree {

	public void init(JPanel groupPanel, List<ISettingsGroup> groups) {
		DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode(NLS.str("preferences.title"));
		addGroups(treeRoot, groups);
		setModel(new DefaultTreeModel(treeRoot));
		getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		setFocusable(false);
		addTreeSelectionListener(e -> switchGroup(groupPanel));
		// expand all nodes and disallow collapsing
		setNodeExpandedState(this, treeRoot, true);
		addTreeWillExpandListener(new DisableRootCollapseListener(treeRoot));
		addSelectionRow(1);
	}

	private static void addGroups(DefaultMutableTreeNode base, List<ISettingsGroup> groups) {
		for (ISettingsGroup group : groups) {
			SettingsTreeNode node = new SettingsTreeNode(group);
			base.add(node);
			addGroups(node, group.getSubGroups());
		}
	}

	private void switchGroup(JPanel groupPanel) {
		Object selected = getLastSelectedPathComponent();
		groupPanel.removeAll();
		if (selected instanceof SettingsTreeNode) {
			groupPanel.add(((SettingsTreeNode) selected).getGroup().buildComponent());
		}
		groupPanel.updateUI();
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

	private static class DisableRootCollapseListener implements TreeWillExpandListener {
		private final DefaultMutableTreeNode treeRoot;

		public DisableRootCollapseListener(DefaultMutableTreeNode treeRoot) {
			this.treeRoot = treeRoot;
		}

		@Override
		public void treeWillExpand(TreeExpansionEvent event) {
		}

		@Override
		public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
			Object current = event.getPath().getLastPathComponent();
			if (Objects.equals(current, treeRoot)) {
				throw new ExpandVetoException(event, "Root collapsing not allowed");
			}
		}
	}
}
