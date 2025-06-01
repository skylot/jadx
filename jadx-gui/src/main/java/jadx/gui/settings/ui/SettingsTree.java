package jadx.gui.settings.ui;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.gui.ISettingsGroup;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.utils.NLS;

public class SettingsTree extends JTree {
	private final JadxSettingsWindow settingsWindow;

	public SettingsTree(JadxSettingsWindow settingsWindow) {
		this.settingsWindow = settingsWindow;
	}

	public void init(List<ISettingsGroup> groups) {
		DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode(NLS.str("preferences.title"));
		addGroups(treeRoot, groups);
		setModel(new DefaultTreeModel(treeRoot));
		getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		setFocusable(false);
		addTreeSelectionListener(ev -> switchGroup());
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

	public void selectGroup(ISettingsGroup group) {
		SettingsTreeNode node = searchTreeNode(group);
		if (node == null) {
			throw new JadxRuntimeException("Settings group not found: " + group);
		}
		setSelectionPath(new TreePath(node.getPath()));
	}

	private @Nullable SettingsTreeNode searchTreeNode(ISettingsGroup group) {
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) getModel().getRoot();
		Enumeration<TreeNode> enumeration = root.children();
		while (enumeration.hasMoreElements()) {
			SettingsTreeNode node = (SettingsTreeNode) enumeration.nextElement();
			if (node.getGroup() == group) {
				return node;
			}
		}
		return null;
	}

	private void switchGroup() {
		Object selected = getLastSelectedPathComponent();
		if (selected instanceof SettingsTreeNode) {
			ISettingsGroup group = ((SettingsTreeNode) selected).getGroup();
			settingsWindow.activateGroup(group);
		} else {
			settingsWindow.activateGroup(null);
		}
	}

	private static void setNodeExpandedState(JTree tree, TreeNode node, boolean expanded) {
		List<? extends TreeNode> list = Collections.list(node.children());
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
