package jadx.gui.ui.tab;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.UiUtils;

public class QuickTabsTree extends JTree implements ITabStatesListener, TreeSelectionListener {
	private final MainWindow mainWindow;
	private final DefaultTreeModel treeModel;

	private final QuickTabsParentNode pinParentNode;

	public QuickTabsTree(MainWindow mainWindow) {
		this.mainWindow = mainWindow;

		mainWindow.getTabbedPane().addTabStateListener(this);

		Root root = new Root();
		pinParentNode = new QuickTabsPinParentNode(mainWindow.getTabbedPane());
		fillPinParentNode();
		root.add(pinParentNode);

		treeModel = new DefaultTreeModel(root);
		setModel(treeModel);
		setCellRenderer(new CellRenderer());
		setRootVisible(false);
		setShowsRootHandles(true);

		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				TreeNode pressedNode = UiUtils.getTreeNodeUnderMouse(QuickTabsTree.this, e);
				if (SwingUtilities.isLeftMouseButton(e)) {
					if (nodeClickAction(pressedNode)) {
						setFocusable(true);
						requestFocus();
					}
				}
				if (SwingUtilities.isRightMouseButton(e)) {
					triggerRightClickAction(e);
				}
			}
		});

		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					nodeClickAction(getLastSelectedPathComponent());
				}
			}
		});

		loadSettings();
	}

	private void triggerRightClickAction(MouseEvent e) {
		TreeNode treeNode = UiUtils.getTreeNodeUnderMouse(this, e);
		if (!(treeNode instanceof QuickTabsBaseNode)) {
			return;
		}

		QuickTabsBaseNode quickTabsNode = (QuickTabsBaseNode) treeNode;
		JPopupMenu menu = quickTabsNode.onTreePopupMenu(mainWindow);
		if (menu != null) {
			menu.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	private boolean nodeClickAction(Object pressedNode) {
		if (pressedNode == null) {
			return false;
		}

		if (pressedNode instanceof QuickTabsChildNode) {
			QuickTabsChildNode childNode = (QuickTabsChildNode) pressedNode;
			return mainWindow.getTabbedPane().showNode(childNode.getJNode());
		}

		return false;
	}

	private void fillPinParentNode() {
		mainWindow.getTabbedPane().getPinnedTabs().forEach((contentPanel) -> pinParentNode.addJNode(contentPanel.getNode()));
	}

	@Override
	public void valueChanged(TreeSelectionEvent event) {
		DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) getLastSelectedPathComponent();
		if (selectedNode != null) {
			if (selectedNode instanceof QuickTabsChildNode) {
				QuickTabsChildNode childNode = (QuickTabsChildNode) selectedNode;
				JNode jNode = childNode.getJNode();
				TabbedPane tabbedPane = mainWindow.getTabbedPane();
				tabbedPane.selectTab(tabbedPane.getTabByNode(jNode));
			}
		}
	}

	@Override
	public void onTabPinChange(JNode node, boolean pinned) {
		if (pinned) {
			pinParentNode.addJNode(node);
			treeModel.nodesWereInserted(pinParentNode, new int[] { pinParentNode.getChildCount() - 1 });
		} else {
			QuickTabsChildNode child = pinParentNode.getQuickTabsNode(node);
			int removedIndex = pinParentNode.getIndex(child);
			pinParentNode.removeJNode(node);
			treeModel.nodesWereRemoved(pinParentNode, new int[] { removedIndex }, new Object[] { child });
		}
	}

	public void loadSettings() {
		Font font = mainWindow.getSettings().getFont();
		Font largerFont = font.deriveFont(font.getSize() + 2.f);

		setFont(largerFont);
	}

	public void dispose() {
		mainWindow.getTabbedPane().removeTabStateListener(this);
	}

	private class Root extends DefaultMutableTreeNode {

	}

	private class CellRenderer extends DefaultTreeCellRenderer {
		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row,
				boolean hasFocus) {
			Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
			if (value instanceof QuickTabsBaseNode) {
				QuickTabsBaseNode quickTabsNode = (QuickTabsBaseNode) value;
				setIcon(quickTabsNode.getIcon());
			}
			return c;
		}
	}
}
