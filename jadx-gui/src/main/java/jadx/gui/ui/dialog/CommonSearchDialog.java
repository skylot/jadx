package jadx.gui.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

import jadx.api.ResourceType;
import jadx.gui.logs.LogOptions;
import jadx.gui.treemodel.CodeCommentNode;
import jadx.gui.treemodel.CodeNode;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JField;
import jadx.gui.treemodel.JMethod;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JPackage;
import jadx.gui.treemodel.JResSearchNode;
import jadx.gui.treemodel.JResource;
import jadx.gui.treemodel.RefCommentNode;
import jadx.gui.treemodel.SearchResultNode;
import jadx.gui.treemodel.SearchTreeCellRenderer;
import jadx.gui.treemodel.TextNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.panel.ProgressPanel;
import jadx.gui.ui.tab.TabsController;
import jadx.gui.utils.CacheObject;
import jadx.gui.utils.JNodeCache;
import jadx.gui.utils.JumpPosition;
import jadx.gui.utils.NLS;
import jadx.gui.utils.TreeUtils;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.pkgs.PackageHelper;

import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;

public abstract class CommonSearchDialog extends JFrame {
	private static final Logger LOG = LoggerFactory.getLogger(CommonSearchDialog.class);
	private static final long serialVersionUID = 8939332306115370276L;

	protected final transient TabsController tabsController;
	protected final transient CacheObject cache;
	protected final transient MainWindow mainWindow;
	protected final transient Font codeFont;
	protected final transient String windowTitle;

	protected ResultsTree resultsTree;

	protected JLabel resultsInfoLabel;
	protected JLabel progressInfoLabel;
	protected JLabel warnLabel;
	protected ProgressPanel progressPane;

	public CommonSearchDialog(MainWindow mainWindow, String title) {
		this.mainWindow = mainWindow;
		this.tabsController = mainWindow.getTabsController();
		this.cache = mainWindow.getCacheObject();
		this.codeFont = mainWindow.getSettings().getFont();
		this.windowTitle = title;
		UiUtils.setWindowIcons(this);
		updateTitle("");
	}

	protected abstract void openInit();

	protected abstract void loadFinished();

	protected abstract void loadStart();

	public void loadWindowPos() {
		if (!mainWindow.getSettings().loadWindowPos(this)) {
			setSize(800, 500);
		}
	}

	private void updateTitle(String searchText) {
		if (searchText == null || searchText.isEmpty() || searchText.trim().isEmpty()) {
			setTitle(windowTitle);
		} else {
			setTitle(windowTitle + ": " + searchText);
		}
	}

	public void updateHighlightContext(String text) {
		updateTitle(text);
	}

	protected void registerInitOnOpen() {
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(WindowEvent e) {
				SwingUtilities.invokeLater(CommonSearchDialog.this::openInit);
			}
		});
	}

	protected void openSelectedItem(MouseEvent event) {
		JNode node = getSelectedNode(event);
		if (node instanceof SearchResultNode) {
			SearchResultNode node1 = (SearchResultNode) node;
			openItem(node1.getRealNode());
		} else {
			openItem(node);
		}
	}

	protected void openSelectedItem(@Nullable Object node) {
		if (node instanceof SearchResultNode) {
			SearchResultNode node1 = (SearchResultNode) node;
			openItem(node1.getRealNode());
		} else {
			openItem((JNode) node);
		}
	}

	protected void openItem(JNode node) {
		if (node instanceof JResSearchNode) {
			JumpPosition jmpPos = new JumpPosition(((JResSearchNode) node).getResNode(), node.getPos());
			tabsController.codeJump(jmpPos);
		} else {
			tabsController.codeJump(node);
		}
		if (!mainWindow.getSettings().getKeepCommonDialogOpen()) {
			dispose();
		}
	}

	@Nullable
	private JNode getSelectedNode(MouseEvent event) {
		return TreeUtils.getJNodeUnderMouse(resultsTree, event);
	}

	@Override
	public void dispose() {
		mainWindow.getSettings().saveWindowPos(this);
		super.dispose();
	}

	protected void initCommon() {
		UiUtils.addEscapeShortCutToDispose(this);
	}

	@NotNull
	protected JPanel initButtonsPanel() {
		progressPane = new ProgressPanel(mainWindow, false);

		JButton cancelButton = new JButton(NLS.str("search_dialog.cancel"));
		cancelButton.addActionListener(event -> dispose());

		JCheckBox cbKeepOpen = new JCheckBox(NLS.str("search_dialog.keep_open"));
		cbKeepOpen.setSelected(mainWindow.getSettings().getKeepCommonDialogOpen());
		cbKeepOpen.addActionListener(e -> {
			mainWindow.getSettings().setKeepCommonDialogOpen(cbKeepOpen.isSelected());
			mainWindow.getSettings().sync();
		});
		cbKeepOpen.setAlignmentY(Component.CENTER_ALIGNMENT);

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.add(cbKeepOpen);
		buttonPane.add(Box.createRigidArea(new Dimension(15, 0)));
		buttonPane.add(progressPane);
		buttonPane.add(Box.createRigidArea(new Dimension(5, 0)));
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(cancelButton);
		return buttonPane;
	}

	protected JPanel initResultsTree() {
		resultsTree = new ResultsTree();
		resultsTree.setCellRenderer(new SearchTreeCellRenderer());
		resultsTree.setDragEnabled(false);
		resultsTree.setAutoscrolls(false);
		resultsTree.setRootVisible(false);

		resultsTree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(evt)) {
					openSelectedItem(evt);
				}
			}
		});
		resultsTree.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					openSelectedItem(resultsTree.getLeadSelectionPath());
				}
			}
		});

		warnLabel = new JLabel();
		warnLabel.setForeground(Color.RED);
		warnLabel.setVisible(false);

		JScrollPane scroll = new JScrollPane(resultsTree, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);

		resultsInfoLabel = new JLabel("");
		resultsInfoLabel.setFont(mainWindow.getSettings().getFont());

		progressInfoLabel = new JLabel("");
		progressInfoLabel.setFont(mainWindow.getSettings().getFont());
		progressInfoLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				mainWindow.showLogViewer(LogOptions.allWithLevel(Level.INFO));
			}
		});

		JPanel resultsActionsPanel = new JPanel();
		resultsActionsPanel.setLayout(new BoxLayout(resultsActionsPanel, BoxLayout.LINE_AXIS));
		resultsActionsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
		addResultsActions(resultsActionsPanel);

		JPanel resultsPanel = new JPanel();
		resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.PAGE_AXIS));
		resultsPanel.add(warnLabel, BorderLayout.PAGE_START);
		resultsPanel.add(scroll, BorderLayout.CENTER);
		resultsPanel.add(resultsActionsPanel, BorderLayout.PAGE_END);
		return resultsPanel;
	}

	protected void addResultsActions(JPanel resultsActionsPanel) {
		resultsActionsPanel.add(Box.createRigidArea(new Dimension(20, 0)));
		resultsActionsPanel.add(resultsInfoLabel);
		resultsActionsPanel.add(Box.createRigidArea(new Dimension(20, 0)));
		resultsActionsPanel.add(progressInfoLabel);
		resultsActionsPanel.add(Box.createHorizontalGlue());
	}

	protected void updateProgressLabel(boolean complete) {
		int count = resultsTree.getFoundCount();
		String statusText;
		if (complete) {
			statusText = NLS.str("search_dialog.results_complete", count);
		} else {
			statusText = NLS.str("search_dialog.results_incomplete", count);
		}
		resultsInfoLabel.setText(statusText);
	}

	protected void showSearchState() {
		resultsInfoLabel.setText(NLS.str("search_dialog.tip_searching") + "...");
	}

	protected static final class ResultsTree extends JTree {
		private final transient List<JNode> rows = new ArrayList<>();
		private final JNode treeRoot;
		private final DefaultTreeModel treeModel;
		private transient int foundCount;

		public ResultsTree() {
			setShowsRootHandles(true);
			treeRoot = new TextNode("");
			treeModel = new DefaultTreeModel(treeRoot);
			setModel(treeModel);
		}

		public void updateTree() {
			addNodes();
			treeModel.reload();
			TreeUtils.expandAllNodes(this);
		}

		public void clear() {
			foundCount = 0;
			rows.clear();
			treeRoot.removeAllChildren();
			treeModel.reload();
		}

		public void sort() {
			Collections.sort(rows);
			updateTree();
		}

		private void addNodes() {
			if (rows.isEmpty()) {
				return;
			}
			for (JNode row : rows) {
				if (row instanceof JMethod) {
					treeRoot.add(row);
				}
				if (row instanceof JField) {
					treeRoot.add(row);
				}
				if (row instanceof JClass) {
					JClass jClass = (JClass) row;
					JPackage jPackage = findPackage(jClass);

					// create new objects
					JPackage existingPackage = findOrCreatePackage(jPackage);
					JClass existingClass = findOrCreateClass(existingPackage, jClass);

					existingPackage.add(existingClass);
					treeRoot.add(existingPackage);
				}
				if (row instanceof CodeNode) {
					CodeNode node = (CodeNode) row;
					processCodeNode(node, null);
				}
				if (row instanceof JResSearchNode) {
					JResSearchNode jResSearchNode = (JResSearchNode) row;
					JResource jResource = jResSearchNode.getResNode();
					// check if file node from resource table
					JResource tableRes = isTableResource(jResource);
					// file node
					JResource newJResource = findOrCreateJResource(jResource);
					SearchResultNode textNode =
							new SearchResultNode(jResSearchNode.makeDescString(), row, jResSearchNode.getStart(), jResSearchNode.getEnd());

					if (tableRes != null) {
						newJResource = findOrCreateJResource(tableRes);
						JResource subRes = findOrCreateSubJResource(jResource, newJResource);
						subRes.add(textNode);
						newJResource.add(subRes);
					} else {
						newJResource.add(textNode);
					}
					treeRoot.add(newJResource);
				}
				if (row instanceof CodeCommentNode | row instanceof RefCommentNode) {
					processCodeNode(null, (RefCommentNode) row);
				}
			}
		}

		public int getFoundCount() {
			return foundCount;
		}

		public void addAll(Collection<? extends JNode> nodes) {
			foundCount += nodes.size();
			rows.clear();
			rows.addAll(nodes);
		}

		private void processCodeNode(CodeNode codeNode, RefCommentNode codeCommentNode) {
			JClass jClass = null;
			JPackage jPackage = null;
			JNode jMethod = null;
			JNode addedNode = null;
			if (codeNode != null) {
				jClass = codeNode.getRootClass();
				jPackage = (JPackage) jClass.getParent();
				jMethod = codeNode.getjNode();
				addedNode = new SearchResultNode(codeNode.makeDescString(), codeNode, codeNode.getStart(), codeNode.getEnd());
			} else if (codeCommentNode != null) {
				addedNode = new SearchResultNode(codeCommentNode.makeDescString(), codeCommentNode, 0, 0);
				jMethod = codeCommentNode.getNode();
				if (jMethod instanceof JClass) {
					jClass = (JClass) jMethod;
				} else {
					jClass = jMethod.getJParent();
				}
				jPackage = PackageHelper.buildJPackage(codeCommentNode.getRootClass().getCls().getJavaPackage(), false);
			}
			JNode parentClass = jMethod.getJParent();
			// create new objects
			JPackage existingPackage = findOrCreatePackage(jPackage);
			JClass existingClass = findOrCreateClass(existingPackage, jClass);

			Stack<JNode> stack = new Stack<>();
			if (jMethod instanceof JClass) {
				stack.push(jMethod);
			}
			while (parentClass != null) {
				stack.push(parentClass);
				parentClass = parentClass.getJParent();
			}

			while (!stack.isEmpty()) {
				JNode currentClass = stack.pop();
				JClass newParentClass = findOrCreateSubClass((JClass) currentClass, existingClass);

				if (jMethod instanceof JMethod) {
					if (currentClass == jMethod.getJParent()) {
						JMethod existingMethod = findOrCreateMethod((JMethod) jMethod, newParentClass);
						existingMethod.add(addedNode);
					}
				} else if (jMethod instanceof JField) {
					JField jField = (JField) jMethod;
					jField.add(addedNode);
					JClass jClassField = jField.getRootClass();
					newParentClass = findOrCreateSubClass(jClassField, newParentClass);
					newParentClass.add(jField);
				} else {
					if (newParentClass.makeLongString().equals(jMethod.makeLongString())) {
						newParentClass.add(addedNode);
					}
				}
				existingClass = newParentClass;
			}
			treeRoot.add(existingPackage);
		}

		private JPackage findPackage(JNode jNode) {
			JNode parentNode = (JNode) jNode.getParent();
			JNode jNodeJParent = jNode.getJParent();
			if (parentNode != null) {
				return (JPackage) parentNode;
			} else {
				return findPackage(jNodeJParent);
			}
		}

		private JResource isTableResource(JNode jResource) {
			if (jResource.getParent() instanceof JResource) {
				JResource parent = (JResource) jResource.getParent();
				if (parent != null) {
					if (parent.getResFile() != null && parent.getResFile().getType() == ResourceType.ARSC) {
						return parent;
					} else {
						return isTableResource((JNode) parent.getParent());
					}
				}
			}
			return null;
		}

		private JPackage findOrCreatePackage(JPackage jPackage) {
			for (int i = 0; i < treeRoot.getChildCount(); i++) {
				if (treeRoot.getChildAt(i) instanceof JPackage) {
					JPackage existingPackage = (JPackage) treeRoot.getChildAt(i);
					if (existingPackage.getPkg().getFullName().equals(jPackage.getPkg().getFullName())) {
						return existingPackage;
					}
				}
			}

			JPackage newPackage = new JPackage(
					jPackage.getPkg(),
					jPackage.isEnabled(),
					jPackage.getClasses(),
					jPackage.getSubPackages(),
					jPackage.isSynthetic());
			newPackage.setName(jPackage.getPkg().getFullName());
			return newPackage;
		}

		private JResource findOrCreateJResource(JResource jResource) {
			for (int i = 0; i < treeRoot.getChildCount(); i++) {
				if (treeRoot.getChildAt(i) instanceof JResource) {
					JResource subChildren = (JResource) treeRoot.getChildAt(i);
					if (subChildren.makeLongString().equals(jResource.makeLongString())) {
						return subChildren;
					}
				}
			}
			return new JResource(jResource.getResFile(), jResource.getName(), jResource.getType());
		}

		private JResource findOrCreateSubJResource(JResource jResource, JResource jParent) {
			if (jParent.makeLongString().equals(jResource.makeLongString())) {
				return jParent;
			}
			for (Iterator<TreeNode> iterator = jParent.children().asIterator(); iterator.hasNext();) {
				JNode subChildren = (JNode) iterator.next();
				if (subChildren instanceof JResource && subChildren.makeLongString().equals(jResource.makeLongString())) {
					return (JResource) subChildren;
				}
			}
			return new JResource(jResource.getResFile(), jResource.getName(), jResource.getType());
		}

		private JClass findOrCreateSubClass(JClass jClass, JClass jParent) {
			if (jParent.makeLongString().equals(jClass.makeLongString())) {
				return jParent;
			}
			for (Iterator<TreeNode> iterator = jParent.children().asIterator(); iterator.hasNext();) {
				JNode subChildren = (JNode) iterator.next();
				if (subChildren instanceof JClass && subChildren.makeLongString().equals(jClass.makeLongString())) {
					return (JClass) subChildren;
				}
			}

			JClass newClass = new JClass(jClass.getCls(), jClass.getJParent());
			newClass.setStart(jClass.getStart());
			newClass.setEnd(jClass.getEnd());
			newClass.setHasHighlight(jClass.isHasHighlight());
			jParent.add(newClass);
			return newClass;
		}

		private JClass findOrCreateClass(JPackage jPackage, JClass jClass) {
			for (Iterator<TreeNode> it = jPackage.children().asIterator(); it.hasNext();) {
				JNode child = (JNode) it.next();
				if (child instanceof JClass) {
					if (child.makeLongString().equals(jClass.makeLongString())) {
						return (JClass) child;
					}
				}
			}

			JClass newClass = new JClass(jClass.getCls(), jClass.getJParent());
			newClass.setStart(jClass.getStart());
			newClass.setEnd(jClass.getEnd());
			newClass.setHasHighlight(jClass.isHasHighlight());
			jPackage.add(newClass);
			return newClass;
		}

		private JMethod findOrCreateMethod(JMethod jMethod, JClass jClass) {
			for (Iterator<TreeNode> it = jClass.children().asIterator(); it.hasNext();) {
				JNode child = (JNode) it.next();
				if (child instanceof JMethod && child.makeLongString().equals(jMethod.makeLongString())) {
					return (JMethod) child;
				}
			}

			JMethod newMethod = new JMethod(jMethod.getJavaMethod(), jMethod.getJParent());
			jClass.add(newMethod);
			return newMethod;
		}
	}

	void progressStartCommon() {
		progressPane.setIndeterminate(true);
		progressPane.setVisible(true);
		warnLabel.setVisible(false);
	}

	void progressFinishedCommon() {
		progressPane.setVisible(false);
	}

	protected JNodeCache getNodeCache() {
		return mainWindow.getCacheObject().getNodeCache();
	}
}
