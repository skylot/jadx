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

	private final QuickTabsParentNode openParentNode;
	private final QuickTabsParentNode pinParentNode;
	private final QuickTabsParentNode bookmarkParentNode;

	public QuickTabsTree(MainWindow mainWindow) {
		this.mainWindow = mainWindow;

		mainWindow.getTabsController().addListener(this);

		Root root = new Root();
		pinParentNode = new QuickTabsPinParentNode(mainWindow.getTabsController());
		openParentNode = new QuickTabsOpenParentNode(mainWindow.getTabsController());
		bookmarkParentNode = new QuickTabsBookmarkParentNode(mainWindow.getTabsController());
		root.add(openParentNode);
		root.add(pinParentNode);
		root.add(bookmarkParentNode);

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

		fillOpenParentNode();
		fillPinParentNode();
		fillBookmarkParentNode();
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
			mainWindow.getTabsController().selectTab(childNode.getJNode());
			return true;
		}

		return false;
	}

	private void fillOpenParentNode() {
		mainWindow.getTabsController().getOpenTabs().forEach(this::onTabOpen);
	}

	private void fillPinParentNode() {
		mainWindow.getTabsController().getPinnedTabs().forEach(this::onTabPinChange);
	}

	private void fillBookmarkParentNode() {
		mainWindow.getTabsController().getBookmarkedTabs().forEach(this::onTabBookmarkChange);
	}

	private void clearParentNode(QuickTabsParentNode parentNode) {
		int[] childIndices = new int[parentNode.getChildCount()];
		Object[] objects = new Object[parentNode.getChildCount()];
		for (int i = 0; i < childIndices.length; i++) {
			childIndices[i] = i;
			objects[i] = parentNode.getChildAt(i);
		}
		parentNode.removeAllNodes();
		treeModel.nodesWereRemoved(parentNode, childIndices, objects);
	}

	private void addJNode(QuickTabsParentNode parentNode, JNode node) {
		if (parentNode.addJNode(node)) {
			treeModel.nodesWereInserted(parentNode, new int[] { parentNode.getChildCount() - 1 });
		}
	}

	private void removeJNode(QuickTabsParentNode parentNode, JNode node) {
		QuickTabsChildNode child = parentNode.getQuickTabsNode(node);
		if (child != null) {
			int removedIndex = parentNode.getIndex(child);
			if (parentNode.removeJNode(node)) {
				treeModel.nodesWereRemoved(parentNode, new int[] { removedIndex }, new Object[] { child });
			}
		}
	}

	@Override
	public void valueChanged(TreeSelectionEvent event) {
		DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) getLastSelectedPathComponent();
		if (selectedNode != null) {
			if (selectedNode instanceof QuickTabsChildNode) {
				QuickTabsChildNode childNode = (QuickTabsChildNode) selectedNode;
				JNode jNode = childNode.getJNode();

				TabsController tabsController = mainWindow.getTabsController();
				tabsController.selectTab(jNode);
			}
		}
	}

	public void loadSettings() {
		Font font = mainWindow.getSettings().getFont();
		Font largerFont = font.deriveFont(font.getSize() + 2.f);

		setFont(largerFont);
	}

	public void dispose() {
		mainWindow.getTabsController().removeListener(this);
	}

	@Override
	public void onTabOpen(TabBlueprint blueprint) {
		if (!blueprint.isHidden() && blueprint.getNode().supportsQuickTabs()) {
			addJNode(openParentNode, blueprint.getNode());
		}
	}

	@Override
	public void onTabClose(TabBlueprint blueprint) {
		removeJNode(openParentNode, blueprint.getNode());
		removeJNode(pinParentNode, blueprint.getNode());
		removeJNode(bookmarkParentNode, blueprint.getNode());
	}

	@Override
	public void onTabPinChange(TabBlueprint blueprint) {
		JNode node = blueprint.getNode();
		if (blueprint.isPinned()) {
			addJNode(pinParentNode, node);
		} else {
			removeJNode(pinParentNode, node);
		}
	}

	@Override
	public void onTabBookmarkChange(TabBlueprint blueprint) {
		JNode node = blueprint.getNode();
		if (blueprint.isBookmarked()) {
			addJNode(bookmarkParentNode, node);
		} else {
			removeJNode(bookmarkParentNode, node);
		}
	}

	@Override
	public void onTabVisibilityChange(TabBlueprint blueprint) {
		JNode node = blueprint.getNode();
		if (!blueprint.isHidden()) {
			addJNode(openParentNode, node);
		} else {
			removeJNode(openParentNode, node);
		}
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
