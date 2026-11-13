package jadx.gui.tree;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatClientProperties;

import hu.akarnokd.rxjava3.swing.SwingSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import jadx.api.ResourceFile;
import jadx.api.gui.tree.BaseTreeNodeEnum;
import jadx.api.gui.tree.IJadxTree;
import jadx.api.gui.tree.ILoadableTreeNode;
import jadx.api.gui.tree.ITreeNode;
import jadx.api.gui.tree.ITreeNodeDetails;
import jadx.gui.jobs.IBackgroundTask;
import jadx.gui.jobs.TaskWithExtraOnFinish;
import jadx.gui.plugins.context.CommonGuiPluginsContext;
import jadx.gui.plugins.context.TreePopupMenuEntry;
import jadx.gui.settings.JadxSettings;
import jadx.gui.treemodel.ApkSignatureNode;
import jadx.gui.treemodel.JLoadableNode;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JPackage;
import jadx.gui.treemodel.JResource;
import jadx.gui.treemodel.JRoot;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.treenodes.SummaryNode;
import jadx.gui.utils.NLS;
import jadx.gui.utils.TextStandardActions;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.rx.RxUtils;
import jadx.gui.utils.ui.FileOpenerHelper;
import jadx.gui.utils.ui.NodeLabel;

public class JadxTree implements IJadxTree {
	private static final Logger LOG = LoggerFactory.getLogger(JadxTree.class);

	private final MainWindow mainWindow;
	private final JTree tree;
	private final FilterableTreeModel treeModel;
	private final JTextField treeFilterField;
	private final Disposable treeFilterDisposable;
	private final Disposable treeScrollDisposable;
	private final TreeExpansionService treeExpansionService;
	private final JPanel filterPanel;
	private final JScrollPane treeScrollPane;

	private @Nullable volatile JRoot treeRoot;
	private volatile boolean treeReloading;

	private final List<Consumer<JRoot>> treeUpdateListener = new ArrayList<>();

	/**
	 * Custom tree nodes stored in ordered map to keep nodes per ID
	 */
	private final Map<String, CustomTreeNode> customTreeNodes = Collections.synchronizedMap(new LinkedHashMap<>());

	public JadxTree(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
		DefaultMutableTreeNode defaultNode = new DefaultMutableTreeNode(NLS.str("msg.open_file"));
		treeModel = new FilterableTreeModel(mainWindow, defaultNode);
		tree = new JTree(treeModel);
		tree.setLargeModel(true);
		ToolTipManager.sharedInstance().registerComponent(tree);
		treeExpansionService = new TreeExpansionService(mainWindow, tree);

		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setFocusable(false);
		tree.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				tree.setFocusable(false);
			}
		});
		tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e)) {
					if (!nodeClickAction(getNodeUnderMouse(e))) {
						// click ignored -> switch to focusable mode
						tree.setFocusable(true);
						tree.requestFocus();
					}
				} else if (SwingUtilities.isRightMouseButton(e)) {
					treeRightClickAction(e);
				}
			}
		});
		tree.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					nodeClickAction(tree.getLastSelectedPathComponent());
				}
			}
		});
		tree.setCellRenderer(new DefaultTreeCellRenderer() {
			@Override
			public Component getTreeCellRendererComponent(JTree tree,
					Object value, boolean selected, boolean expanded,
					boolean isLeaf, int row, boolean focused) {
				Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, isLeaf, row, focused);
				if (value instanceof ITreeNode) {
					ITreeNode treeNode = (ITreeNode) value;
					if (treeNode instanceof ITreeNodeDetails) {
						ITreeNodeDetails nodeDetails = (ITreeNodeDetails) treeNode;
						NodeLabel.disableHtml(this, nodeDetails.disableHtml());
						setText(nodeDetails.makeStringHtml());
					} else {
						NodeLabel.disableHtml(this, true);
						setText(treeNode.getName());
					}
					setIcon(treeNode.getIcon());
					setToolTipText(treeNode.getTooltip());
				} else {
					setToolTipText(null);
				}
				if (value instanceof JPackage) {
					setEnabled(((JPackage) value).isEnabled());
				}
				return c;
			}
		});
		tree.addTreeWillExpandListener(new TreeWillExpandListener() {
			@Override
			public void treeWillExpand(TreeExpansionEvent event) {
				TreePath path = event.getPath();
				Object node = path.getLastPathComponent();
				if (node instanceof JLoadableNode) {
					JLoadableNode treeNode = (JLoadableNode) node;
					IBackgroundTask loadTask = treeNode.getLoadTask();
					if (loadTask != null) {
						mainWindow.getBackgroundExecutor().execute(new TaskWithExtraOnFinish(loadTask,
								status -> {
									if (!treeReloading) {
										treeModel.nodeStructureChanged(treeNode);
									}
								}));
					}
				} else if (node instanceof ILoadableTreeNode) {
					ILoadableTreeNode loadableNode = (ILoadableTreeNode) node;
					mainWindow.getBackgroundExecutor().execute("Loading node", loadableNode::loadNode,
							status -> {
								if (!treeReloading && node instanceof TreeNode) {
									treeModel.nodeStructureChanged((TreeNode) node);
								}
							});
				}
			}

			@Override
			public void treeWillCollapse(TreeExpansionEvent event) {
				if (!treeReloading) {
					mainWindow.update();
				}
			}
		});

		treeFilterField = new JTextField();
		TextStandardActions.attach(treeFilterField);
		treeFilterField.setToolTipText(NLS.str("tree.filter"));
		treeFilterField.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
		treeFilterField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, NLS.str("tree.filter"));
		treeFilterField.registerKeyboardAction(ev -> treeFilterField.setText(""),
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_FOCUSED);

		treeFilterDisposable = RxUtils.textFieldChanges(treeFilterField)
				.debounce(300, TimeUnit.MILLISECONDS)
				.observeOn(Schedulers.newThread())
				.subscribe(t -> treeModel.setFilter(treeFilterField.getText()));

		filterPanel = new JPanel(new BorderLayout());
		filterPanel.setBorder(BorderFactory.createEmptyBorder(5, 2, 2, 2));
		filterPanel.add(treeFilterField, BorderLayout.CENTER);

		treeScrollPane = new JScrollPane(tree);
		treeScrollPane.setMinimumSize(new Dimension(100, 150));
		treeScrollDisposable = RxUtils.scrollBarEvents(treeScrollPane.getVerticalScrollBar())
				.observeOn(SwingSchedulers.edt())
				.subscribe(v -> treeModel.expandVisibleFilteredNodes());
	}

	private void registerCustomNodes() {
		addNode(BaseTreeNodeEnum.ROOT, ApkSignatureNode.getApkSignature(mainWindow.getWrapper()));
		addNode(BaseTreeNodeEnum.ROOT, new SummaryNode(mainWindow));
	}

	public void initTree() {
		treeReloading = true;
		try {
			treeRoot = new JRoot(mainWindow);
			treeRoot.setFlatPackages(mainWindow.getSettings().isFlattenPackage());
			treeModel.setRoot(treeRoot);
			treeFilterField.setText("");
			registerCustomNodes();
			treeRoot.update();
		} finally {
			treeReloading = false;
		}
		reloadTree();
	}

	public void clearTree() {
		treeRoot = null;
		treeModel.setRoot(null);
		treeModel.reload();
		customTreeNodes.clear();
	}

	public void reloadTree() {
		treeReloading = true;
		treeUpdateListener.forEach(listener -> listener.accept(treeRoot));
		treeModel.reload();
		treeReloading = false;
	}

	public void rebuildPackagesTree() {
		if (treeRoot != null) {
			treeRoot.update();
		}
	}

	public void loadSettings() {
		JadxSettings settings = mainWindow.getSettings();
		tree.setFont(settings.getCodeFont());
		tree.setRowHeight(-1);
	}

	private boolean nodeClickAction(@Nullable Object obj) {
		if (obj == null) {
			return false;
		}
		try {
			if (obj instanceof JResource) {
				JResource res = (JResource) obj;
				ResourceFile resFile = res.getResFile();
				if (resFile != null) {
					if (JResource.isOpenInExternalTool(resFile.getType())) {
						FileOpenerHelper.openFile(mainWindow, res);
						return true;
					}
					if (JResource.isSupportedForView(resFile.getType())) {
						mainWindow.getTabsController().selectTab(res, true);
						return true;
					}
				}
			} else if (obj instanceof JNode) {
				JNode treeNode = (JNode) obj;
				if (treeNode.hasContent() || treeNode.getJParent() != null) {
					mainWindow.getTabsController().codeJump(treeNode, true);
					return true;
				}
			} else if (obj instanceof ITreeNode) {
				ITreeNode treeNode = (ITreeNode) obj;
				if (treeNode.hasContent()) {
					mainWindow.getTabsController().selectTab(treeNode, true);
					return true;
				}
			}
		} catch (Exception e) {
			LOG.error("Content loading error", e);
		}
		return false;
	}

	private void treeRightClickAction(MouseEvent e) {
		ITreeNode node = getNodeUnderMouse(e);
		if (node == null) {
			return;
		}
		JPopupMenu menu = node.onTreePopupMenu(mainWindow);
		CommonGuiPluginsContext pluginsContext = mainWindow.getGuiPluginsManager().getPluginsContext();
		for (TreePopupMenuEntry entry : pluginsContext.getTreePopupMenuEntries()) {
			JMenuItem menuItem = entry.buildEntry(node);
			if (menuItem != null) {
				if (menu == null) {
					menu = new JPopupMenu();
				}
				menu.add(menuItem);
			}
		}
		if (menu != null) {
			menu.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	private @Nullable ITreeNode getNodeUnderMouse(MouseEvent mouseEvent) {
		TreeNode treeNode = UiUtils.getTreeNodeUnderMouse(tree, mouseEvent);
		if (treeNode instanceof ITreeNode) {
			return (ITreeNode) treeNode;
		}
		return null;
	}

	public void selectNodeInTree(ITreeNode node) {
		if (treeRoot == null) {
			return;
		}
		if (node.getParent() == null) {
			// node not registered in tree
			ITreeNode treeNode = treeRoot.searchNode(node);
			if (treeNode == null) {
				LOG.error("Node not found in tree: {}", node);
				return;
			}
			node = treeNode;
		}
		TreeNode[] pathNodes = treeModel.getPathToRoot(node);
		if (pathNodes == null) {
			return;
		}
		TreePath path = new TreePath(pathNodes);
		tree.setSelectionPath(path);
		tree.makeVisible(path);
		tree.scrollPathToVisible(path);
		tree.requestFocus();
	}

	public void reloadTreePreservingState() {
		List<String> treePath = getTreeState();
		reloadTree();
		treeExpansionService.load(treePath);
	}

	public List<String> getTreeState() {
		return treeExpansionService.save();
	}

	public void loadTreeState(List<String> state) {
		treeExpansionService.load(state);
	}

	public void setFlattenPackage(boolean isFlattenPackage) {
		Object root = treeModel.getRoot();
		if (root instanceof JRoot) {
			JRoot treeRoot = (JRoot) root;
			treeRoot.setFlatPackages(isFlattenPackage);
			reloadTree();
		}
	}

	public void addTreeUpdateListener(Consumer<JRoot> listener) {
		treeUpdateListener.add(listener);
	}

	public void addTreeCustomNodes() {
		synchronized (customTreeNodes) {
			customTreeNodes.forEach((id, node) -> insertCustomTreeNode(node));
		}
	}

	@Override
	public void addNode(BaseTreeNodeEnum parent, @Nullable ITreeNode treeNode) {
		if (treeNode == null) {
			return;
		}
		CustomTreeNode customTreeNode = new CustomTreeNode(parent, treeNode);
		CustomTreeNode prev = customTreeNodes.put(treeNode.getID(), customTreeNode);
		if (!treeReloading && treeRoot != null) {
			UiUtils.uiRunAndWait(() -> {
				if (prev != null) {
					removeCustomTreeNode(prev);
				}
				insertCustomTreeNode(customTreeNode, true);
			});
		}
	}

	private void removeCustomTreeNode(CustomTreeNode customTreeNode) {
		ITreeNode node = customTreeNode.getNode();
		TreeNode parent = node.getParent();
		if (parent != null) {
			node.removeFromParent();
			treeModel.nodeStructureChanged(parent);
		}
	}

	private void insertCustomTreeNode(CustomTreeNode customTreeNode) {
		insertCustomTreeNode(customTreeNode, false);
	}

	private void insertCustomTreeNode(CustomTreeNode customTreeNode, boolean notifyModel) {
		JNode parent = resolveParent(customTreeNode.getParent());
		if (parent != null) {
			parent.add(customTreeNode.getNode());
			if (notifyModel) {
				treeModel.nodeStructureChanged(parent);
			}
		} else if (treeRoot != null) {
			LOG.debug("Can't find parent for custom tree node: {}", customTreeNode);
		}
	}

	private @Nullable JNode resolveParent(BaseTreeNodeEnum parent) {
		JRoot root = treeRoot;
		if (root != null) {
			switch (parent) {
				case ROOT:
					return root;
				case INPUTS:
					return root.followStaticPath("JInputs");
				case SOURCES:
					return root.followStaticPath("JSources");
				case RESOURCES:
					return root.followStaticPath("JResource");
			}
		}
		return null;
	}

	@Override
	public void openTreeNodeContent(ITreeNode node) {
		if (SwingUtilities.isEventDispatchThread()) {
			nodeClickAction(node);
		} else {
			UiUtils.uiRun(() -> nodeClickAction(node));
		}
	}

	public JTree getTreeComponent() {
		return tree;
	}

	public @Nullable JRoot getTreeRoot() {
		return treeRoot;
	}

	public JScrollPane getTreeScrollPane() {
		return treeScrollPane;
	}

	public JPanel getFilterPanel() {
		return filterPanel;
	}

	public JTextField getTreeFilterField() {
		return treeFilterField;
	}

	public void unload() {
		treeRoot = null;
		treeUpdateListener.clear();
		treeFilterDisposable.dispose();
		treeScrollDisposable.dispose();
	}
}
