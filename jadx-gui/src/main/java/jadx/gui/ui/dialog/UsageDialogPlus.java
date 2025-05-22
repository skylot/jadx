package jadx.gui.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeInfo;
import jadx.api.JavaClass;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.api.utils.CodeUtils;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.visitors.prepare.CollectConstValues;
import jadx.gui.JadxWrapper;
import jadx.gui.jobs.TaskStatus;
import jadx.gui.treemodel.CodeNode;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JField;
import jadx.gui.treemodel.JMethod;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.AbstractCodeArea;
import jadx.gui.ui.panel.ProgressPanel;
import jadx.gui.utils.JNodeCache;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

public class UsageDialogPlus extends CommonSearchDialog {
	private static final long serialVersionUID = -5105405789969134107L;

	private final JPanel mainPanel;
	private final JSplitPane splitPane;
	private final CodePanel codePanel;
	private final JTree usageTree;
	private final DefaultTreeModel treeModel;
	private final DefaultMutableTreeNode rootNode;
	private final ProgressPanel localProgressPanel;
	private final JLabel resultsInfoLabel;
	private final JLabel progressInfoLabel;
	private JNode initialNode;

	private static final Logger LOG = LoggerFactory.getLogger(UsageDialogPlus.class);

	public static void open(MainWindow mainWindow, JNode node) {
		UsageDialogPlus usageDialog = new UsageDialogPlus(mainWindow, node);
		mainWindow.addLoadListener(loaded -> {
			if (!loaded) {
				usageDialog.dispose();
				return true;
			}
			return false;
		});
		usageDialog.setVisible(true);

		// Set the position of the split panel after the window is displayed.
		SwingUtilities.invokeLater(() -> {
			int width = usageDialog.splitPane.getWidth();
			if (width > 0) {
				usageDialog.splitPane.setDividerLocation((int) (width * 0.3));
			}
		});
	}

	private UsageDialogPlus(MainWindow mainWindow, JNode node) {
		super(mainWindow, NLS.str("usage_dialog_plus.title"));
		this.initialNode = node;

		// Initialize the progress panel and warning label
		progressPane = new ProgressPanel(mainWindow, false);
		warnLabel = new JLabel();
		warnLabel.setForeground(Color.RED);
		warnLabel.setVisible(false);

		// Initialize result and progress info labels
		resultsInfoLabel = new JLabel("");
		progressInfoLabel = new JLabel("");
		localProgressPanel = new ProgressPanel(mainWindow, false);

		mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());

		// Create the code panel
		codePanel = new CodePanel(mainWindow);

		// Create the tree
		rootNode = new DefaultMutableTreeNode(node);
		treeModel = new DefaultTreeModel(rootNode);
		usageTree = new JTree(treeModel);
		usageTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		usageTree.setRootVisible(true);
		usageTree.setShowsRootHandles(true);
		usageTree.putClientProperty("JTree.lineStyle", "Horizontal");
		usageTree.setRowHeight(22);
		usageTree.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// Use a custom renderer instead of a custom UI
		usageTree.setCellRenderer(new PathHighlightTreeCellRenderer());

		// Add tree listeners
		usageTree.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) usageTree.getLastSelectedPathComponent();
				if (node == null) {
					return;
				}

				Object nodeInfo = node.getUserObject();
				if (nodeInfo instanceof CodeNode) {
					CodeNode codeNode = (CodeNode) nodeInfo;
					codePanel.showCode(codeNode, codeNode.makeDescString());
				} else if (nodeInfo instanceof JNode) {
					JNode jNode = (JNode) nodeInfo;
					codePanel.showCode(jNode, jNode.makeDescString());
				}

				// Update the result information to display the number of child nodes of the currently selected node
				updateResultsInfo(node);
			}
		});

		usageTree.addTreeExpansionListener(new TreeExpansionListener() {
			@Override
			public void treeExpanded(TreeExpansionEvent event) {
				TreePath path = event.getPath();
				DefaultMutableTreeNode expandedNode = (DefaultMutableTreeNode) path.getLastPathComponent();

				// Load only when the node has no child nodes.
				if (expandedNode.getChildCount() == 0) {
					Object userObject = expandedNode.getUserObject();
					if (userObject instanceof JNode) {
						JNode nodeToUse = (JNode) userObject;
						// If it is a CodeNode, first convert it to an actual JNode and then search for its usage.
						if (nodeToUse.getClass() == CodeNode.class) {
							nodeToUse = getNodeFromCodeNode((CodeNode) nodeToUse);
						}
						if (nodeToUse != null) {
							loadNodeUsages(nodeToUse, expandedNode);
						}
					}
				}
			}

			@Override
			public void treeCollapsed(TreeExpansionEvent event) {
				// No need to process
			}
		});

		usageTree.addMouseListener(new MouseAdapter() {
			private long lastClickTime = 0;
			private TreePath lastClickPath = null;

			@Override
			public void mouseClicked(MouseEvent e) {
				TreePath path = usageTree.getPathForLocation(e.getX(), e.getY());
				if (path == null) {
					return;
				}

				// Set the selected path
				usageTree.setSelectionPath(path);

				// Handle the right-click menu
				if (e.getButton() == MouseEvent.BUTTON3) {
					DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
					Object userObject = selectedNode.getUserObject();

					if (userObject instanceof JNode || userObject instanceof CodeNode) {
						JNode nodeForMenu = (userObject instanceof JNode) ? (JNode) userObject : getNodeFromCodeNode((CodeNode) userObject);
						showPopupMenu(e, nodeForMenu, path);
					}
					return;
				}

				// Handle left-click single/double click
				if (e.getButton() == MouseEvent.BUTTON1) {
					long clickTime = System.currentTimeMillis();
					// Double-click interval
					long doubleClickInterval = 300;

					DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();

					// If the time interval between two clicks is less than the threshold and the same node is clicked,
					// it is considered a double-click
					if ((clickTime - lastClickTime) < doubleClickInterval && path.equals(lastClickPath)) {
						// Double-click processing - switch between expanded/collapsed states
						if (usageTree.isExpanded(path)) {
							usageTree.collapsePath(path);
						} else {
							usageTree.expandPath(path);
							// If the node has no child nodes and is a JNode, load its usage
							if (selectedNode.getChildCount() == 0 && selectedNode.getUserObject() instanceof JNode) {
								JNode nodeToUse = (JNode) selectedNode.getUserObject();
								// If it is a CodeNode, first convert it to an actual JNode and then search for its usage
								if (nodeToUse.getClass() == CodeNode.class) {
									nodeToUse = getNodeFromCodeNode((CodeNode) nodeToUse);
								}
								if (nodeToUse != null) {
									loadNodeUsages(nodeToUse, selectedNode);
								}
							}
						}
						// Update the result information to display the number of child nodes of the currently selected node
						updateResultsInfo(selectedNode);
					} else {
						// Single-click processing - if the node is not expanded, expand it
						if (!usageTree.isExpanded(path)) {
							usageTree.expandPath(path);
							// If the node has no child nodes and is a JNode, then load its usage.
							if (selectedNode.getChildCount() == 0 && selectedNode.getUserObject() instanceof JNode) {
								JNode nodeToUse = (JNode) selectedNode.getUserObject();
								// If it is a CodeNode, first convert it to an actual JNode and then search for its usage
								if (nodeToUse.getClass() == CodeNode.class) {
									nodeToUse = getNodeFromCodeNode((CodeNode) nodeToUse);
								}
								if (nodeToUse != null) {
									loadNodeUsages(nodeToUse, selectedNode);
								}
							}
						}
						// Update the result information to display the number of child nodes of the currently selected node
						updateResultsInfo(selectedNode);
					}

					lastClickTime = clickTime;
					lastClickPath = path;
				}
			}
		});

		// Create the split panel
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.setOneTouchExpandable(true);
		splitPane.setContinuousLayout(true);
		splitPane.setResizeWeight(0.3); // Left 30%
		splitPane.setDividerSize(10); // Increase the width of the divider for easier dragging

		// Add tree to the scroll pane
		JScrollPane treeScrollPane = new JScrollPane(usageTree);
		treeScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		treeScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

		// Create status panel
		JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		statusPanel.add(resultsInfoLabel);
		statusPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		statusPanel.add(progressInfoLabel);
		statusPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		statusPanel.add(localProgressPanel);

		// Add components to the main panel
		JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.add(treeScrollPane, BorderLayout.CENTER);
		leftPanel.add(statusPanel, BorderLayout.SOUTH);

		splitPane.setLeftComponent(leftPanel);
		splitPane.setRightComponent(codePanel);

		mainPanel.add(splitPane, BorderLayout.CENTER);

		initUI();
		registerInitOnOpen();
		loadWindowPos();
	}

	private void initUI() {
		initCommon();
		JPanel buttonPane = initButtonsPanel();

		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BorderLayout(5, 5));
		contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPanel.add(mainPanel, BorderLayout.CENTER);
		contentPanel.add(buttonPane, BorderLayout.PAGE_END);
		getContentPane().add(contentPanel);

		pack();
		setSize(1300, 700);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		// Add a window size change listener to ensure the divider position adapts to window size changes
		addComponentListener(new java.awt.event.ComponentAdapter() {
			@Override
			public void componentResized(java.awt.event.ComponentEvent e) {
				// Keep the left-right ratio
				int width = splitPane.getWidth();
				if (width > 0) {
					int currentDividerLocation = splitPane.getDividerLocation();
					double ratio = (double) currentDividerLocation / width;

					// If the ratio is too small or too large, reset to a reasonable value
					if (ratio < 0.2 || ratio > 0.5) {
						splitPane.setDividerLocation((int) (width * 0.3)); // Use pixel values
					}
				}
			}
		});
	}

	@Override
	protected void openInit() {
		prepareUsageData(initialNode);

		localProgressPanel.setIndeterminate(true);
		localProgressPanel.setVisible(true);
		progressInfoLabel.setText(NLS.str("search_dialog.tip_searching"));

		// Load the usage of the root node
		loadNodeUsages(initialNode, rootNode);
	}

	private void loadNodeUsages(JNode node, DefaultMutableTreeNode treeNode) {
		// When loading starts, display the searching state
		localProgressPanel.setIndeterminate(true);
		localProgressPanel.setVisible(true);
		progressInfoLabel.setText(NLS.str("search_dialog.tip_searching"));

		mainWindow.getBackgroundExecutor().execute(NLS.str("progress.load"),
				() -> collectUsageData(node, treeNode),
				(status) -> {
					if (status == TaskStatus.CANCEL_BY_MEMORY) {
						mainWindow.showHeapUsageBar();
						UiUtils.errorMessage(UsageDialogPlus.this, NLS.str("message.memoryLow"));
					}
					localProgressPanel.setVisible(false);
					progressInfoLabel.setText(NLS.str("usage_dialog_plus.search_complete"));
					// Update the result information - always display the number of child nodes of the currently
					// selected node
					updateResultsInfo(treeNode);

					// Expand the root node
					if (treeNode == rootNode) {
						usageTree.expandPath(new TreePath(rootNode.getPath()));
					}
				});
	}

	private void updateResultsInfo(DefaultMutableTreeNode node) {
		if (node != null) {
			int childCount = node.getChildCount();
			resultsInfoLabel.setText(NLS.str("search_dialog.results_complete", childCount));
		}
	}

	private int getTotalChildCount(DefaultMutableTreeNode node) {
		int count = node.getChildCount();
		for (Enumeration<TreeNode> e = node.children(); e.hasMoreElements();) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) e.nextElement();
			count += getTotalChildCount(child);
		}
		return count;
	}

	private void prepareUsageData(JNode node) {
		if (mainWindow.getSettings().isReplaceConsts() && node instanceof JField) {
			FieldNode fld = ((JField) node).getJavaField().getFieldNode();
			boolean constField = CollectConstValues.getFieldConstValue(fld) != null;
			if (constField && !fld.getAccessFlags().isPrivate()) {
				// run full decompilation to prepare for full code scan
				mainWindow.requestFullDecompilation();
			}
		}
	}

	private void collectUsageData(JNode node, DefaultMutableTreeNode treeNode) {
		List<CodeNode> usageList = new ArrayList<>();
		buildUsageQuery(node).forEach(
				(searchNode, useNodes) -> useNodes.stream()
						.map(JavaNode::getTopParentClass)
						.distinct()
						.forEach(u -> processUsage(searchNode, u, usageList)));

		// Sort and add to the tree node
		Collections.sort(usageList);
		SwingUtilities.invokeLater(() -> {
			for (CodeNode codeNode : usageList) {
				DefaultMutableTreeNode usageTreeNode = new DefaultMutableTreeNode(codeNode);
				treeModel.insertNodeInto(usageTreeNode, treeNode, treeNode.getChildCount());
			}
			treeModel.nodeStructureChanged(treeNode);
		});
	}

	private Map<JavaNode, List<? extends JavaNode>> buildUsageQuery(JNode node) {
		Map<JavaNode, List<? extends JavaNode>> map = new HashMap<>();
		if (node instanceof JMethod) {
			JavaMethod javaMethod = ((JMethod) node).getJavaMethod();
			for (JavaMethod mth : getMethodWithOverrides(javaMethod)) {
				map.put(mth, mth.getUseIn());
			}
			return map;
		}
		if (node instanceof JClass) {
			JavaClass javaCls = ((JClass) node).getCls();
			map.put(javaCls, javaCls.getUseIn());
			// add constructors usage into class usage
			for (JavaMethod javaMth : javaCls.getMethods()) {
				if (javaMth.isConstructor()) {
					map.put(javaMth, javaMth.getUseIn());
				}
			}
			return map;
		}
		if (node instanceof JField && mainWindow.getSettings().isReplaceConsts()) {
			FieldNode fld = ((JField) node).getJavaField().getFieldNode();
			boolean constField = CollectConstValues.getFieldConstValue(fld) != null;
			if (constField && !fld.getAccessFlags().isPrivate()) {
				// search all classes to collect usage of replaced constants
				map.put(fld.getJavaNode(), mainWindow.getWrapper().getIncludedClasses());
				return map;
			}
		}
		JavaNode javaNode = node.getJavaNode();
		map.put(javaNode, javaNode.getUseIn());
		return map;
	}

	private List<JavaMethod> getMethodWithOverrides(JavaMethod javaMethod) {
		List<JavaMethod> relatedMethods = javaMethod.getOverrideRelatedMethods();
		if (!relatedMethods.isEmpty()) {
			return relatedMethods;
		}
		return Collections.singletonList(javaMethod);
	}

	private void processUsage(JavaNode searchNode, JavaClass topUseClass, List<CodeNode> usageList) {
		ICodeInfo codeInfo = topUseClass.getCodeInfo();
		List<Integer> usePositions = topUseClass.getUsePlacesFor(codeInfo, searchNode);
		if (usePositions.isEmpty()) {
			return;
		}
		String code = codeInfo.getCodeStr();
		JadxWrapper wrapper = mainWindow.getWrapper();
		for (int pos : usePositions) {
			String line = CodeUtils.getLineForPos(code, pos);
			if (line.startsWith("import ")) {
				continue;
			}
			JNodeCache nodeCache = getNodeCache();
			JavaNode enclosingNode = wrapper.getEnclosingNode(codeInfo, pos);
			JClass rootJCls = nodeCache.makeFrom(topUseClass);
			JNode usageJNode = enclosingNode == null ? rootJCls : nodeCache.makeFrom(enclosingNode);

			// Create CodeNode and add to list
			CodeNode codeNode = new CodeNode(rootJCls, usageJNode, line.trim(), pos);
			usageList.add(codeNode);
		}
	}

	private JNode getNodeFromCodeNode(CodeNode codeNode) {
		if (codeNode != null) {
			try {
				// Try to get the actual node referenced by the CodeNode
				JavaNode javaNode = codeNode.getJavaNode();
				JNodeCache nodeCache = getNodeCache();
				JNode node = nodeCache.makeFrom(javaNode);

				// If it cannot be obtained directly, try to get it from jParent
				if (node == null) {
					node = codeNode.getJParent();
				}

				// Record the log for debugging
				if (node != null) {
					LOG.debug("Converted CodeNode to {} of type {}", node.getName(), node.getClass().getSimpleName());
				} else {
					LOG.debug("Failed to convert CodeNode: {}", codeNode.getName());
				}

				return node;
			} catch (Exception e) {
				LOG.error("Error converting CodeNode to JNode", e);
			}
		}
		return null;
	}

	private void showPopupMenu(MouseEvent e, JNode node, TreePath path) {
		if (node == null) {
			return;
		}

		JPopupMenu popup = new JPopupMenu();

		// Add the expand/load usage menu item
		DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path.getLastPathComponent();
		JMenuItem expandItem = new JMenuItem(NLS.str("usage_dialog_plus.expand_usages"));
		expandItem.addActionListener(evt -> {
			// Expand the node and load the usage
			if (treeNode.getChildCount() == 0) {
				JNode nodeToUse = node;
				// If it is a CodeNode, first convert it to an actual JNode and then search for its usage
				if (node.getClass() == CodeNode.class) {
					nodeToUse = getNodeFromCodeNode((CodeNode) node);
				}
				if (nodeToUse != null) {
					loadNodeUsages(nodeToUse, treeNode);
				}
			}
			usageTree.expandPath(path);
		});

		JMenuItem jumpToItem = new JMenuItem(NLS.str("usage_dialog_plus.jump_to"));
		jumpToItem.addActionListener(evt -> {
			openItem(node);
		});

		JMenuItem copyPathItem = new JMenuItem(NLS.str("usage_dialog_plus.copy_path"));
		copyPathItem.addActionListener(evt -> {
			copyUsagePath(path);
		});

		popup.add(expandItem);
		popup.addSeparator();
		popup.add(jumpToItem);
		popup.add(copyPathItem);
		popup.show(e.getComponent(), e.getX(), e.getY());
	}

	private void copyUsagePath(TreePath path) {
		if (path != null) {
			StringBuilder pathBuilder = new StringBuilder();
			Object[] nodes = path.getPath();

			// Actually reverse the node order, from the leaf node (currently selected node) to the root node
			for (int i = nodes.length - 1; i >= 0; i--) {
				DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) nodes[i];
				Object userObject = treeNode.getUserObject();

				if (i < nodes.length - 1) {
					pathBuilder.append("\n");
					// Add indentation - reverse calculate the indentation level
					int indentLevel = nodes.length - 1 - i;
					for (int j = 0; j < indentLevel; j++) {
						pathBuilder.append(" ");
					}
					pathBuilder.append("-> ");
				}

				// Add node information
				if (userObject instanceof JNode) {
					pathBuilder.append(((JNode) userObject).getJavaNode().getCodeNodeRef().toString());
				} else if (userObject instanceof CodeNode) {
					CodeNode codeNode = (CodeNode) userObject;
					pathBuilder.append(codeNode.getJavaNode().getCodeNodeRef().toString());
				}
			}

			// Copy to clipboard
			UiUtils.copyToClipboard(pathBuilder.toString());
		}
	}

	// Delete the PathHighlightTreeUI class, use the enhanced CellRenderer instead
	private class PathHighlightTreeCellRenderer extends DefaultTreeCellRenderer {
		private static final int INDENT_PER_LEVEL = 18; // Each level of indentation in pixels
		private final Color selectedPathColor = new Color(220, 240, 255); // Light blue background
		private final Color selectedPathTextColor = new Color(0, 0, 150); // Dark blue text

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
				// If it is on the selected path but not the selected node, use a special background
				setBackground(selectedPathColor);
				setForeground(selectedPathTextColor);

				// Set the highlighted border
				setBorder(BorderFactory.createCompoundBorder(
						BorderFactory.createLineBorder(selectedPathTextColor, 1),
						BorderFactory.createEmptyBorder(1, level * 2, 1, 1)));
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
			} else if (userObject instanceof CodeNode) {
				CodeNode codeNode = (CodeNode) userObject;
				setText(codeNode.makeLongString());
				setIcon(codeNode.getIcon());
				setToolTipText(codeNode.getTooltip());
			}

			return comp;
		}

		// Add custom icons to enhance visual distinction
		@Override
		public Icon getLeafIcon() {
			return super.getLeafIcon();
		}

		@Override
		public Icon getOpenIcon() {
			return super.getOpenIcon();
		}

		@Override
		public Icon getClosedIcon() {
			return super.getClosedIcon();
		}
	}

	// The code panel class is used to display the code of the selected node.
	private class CodePanel extends JPanel {
		private final RSyntaxTextArea codeArea;
		private final JLabel titleLabel;

		public CodePanel(MainWindow mainWindow) {
			setLayout(new BorderLayout(5, 5));
			setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

			// Set the minimum size to ensure the panel is not completely minimized
			setMinimumSize(new Dimension(300, 400));
			setPreferredSize(new Dimension(800, 600));

			// The title label
			titleLabel = new JLabel(NLS.str("usage_dialog_plus.code_view"));
			titleLabel.setFont(codeFont);
			titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));

			// The code area
			codeArea = AbstractCodeArea.getDefaultArea(mainWindow);
			codeArea.setEditable(false);
			codeArea.setText("// " + NLS.str("usage_dialog_plus.select_node"));

			JScrollPane scrollPane = new JScrollPane(codeArea);
			scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

			add(titleLabel, BorderLayout.NORTH);
			add(scrollPane, BorderLayout.CENTER);
		}

		public void showCode(JNode node, String codeLine) {
			if (node != null) {
				titleLabel.setText(NLS.str("usage_dialog_plus.code_for") + " " + node.makeLongString());
				codeArea.setSyntaxEditingStyle(node.getSyntaxName());

				// Get the complete code
				String contextCode = getContextCode(node, codeLine);
				codeArea.setText(contextCode);

				// Highlight the key line and scroll to that position
				scrollToCodeLine(codeArea, codeLine);

				// If it is a CodeNode, we can get a more precise position
				if (node instanceof CodeNode) {
					CodeNode codeNode = (CodeNode) node;
					int pos = codeNode.getPos();
					if (pos > 0) {
						// Try to use the position information to more accurately locate
						try {
							String text = codeArea.getText();
							int lineNum = 0;
							int curPos = 0;
							// Calculate the line number corresponding to the position
							for (int i = 0; i < text.length() && curPos <= pos; i++) {
								if (text.charAt(i) == '\n') {
									lineNum++;
								}
								curPos++;
							}

							if (lineNum > 0) {
								// Scroll to the calculated line number
								int finalLineNum = lineNum;
								SwingUtilities.invokeLater(() -> {
									try {
										Rectangle lineRect = codeArea
												.modelToView(codeArea.getLineStartOffset(finalLineNum));
										if (lineRect != null) {
											JScrollPane scrollPane = (JScrollPane) codeArea.getParent().getParent();
											Rectangle viewRect = scrollPane.getViewport().getViewRect();
											int y = lineRect.y - (viewRect.height - lineRect.height) / 2;
											if (y < 0) {
												y = 0;
											}
											scrollPane.getViewport().setViewPosition(new Point(0, y));
										}
									} catch (Exception e) {
										// Fall back to using string matching
										scrollToCodeLine(codeArea, codeLine);
									}
								});
							}
						} catch (Exception e) {
							// Fall back to using string matching
							scrollToCodeLine(codeArea, codeLine);
						}
					} else {
						// If there is no position information, use string matching
						scrollToCodeLine(codeArea, codeLine);
					}
				} else {
					// Not a CodeNode, use string matching
					scrollToCodeLine(codeArea, codeLine);
				}
			} else {
				titleLabel.setText(NLS.str("usage_dialog_plus.code_view"));
				codeArea.setText("// " + NLS.str("usage_dialog_plus.select_node"));
			}
		}

		private String getContextCode(JNode node, String codeLine) {
			// Always try to get the complete code
			if (node instanceof CodeNode) {
				CodeNode codeNode = (CodeNode) node;
				JNode usageJNode = codeNode.getJParent();
				if (usageJNode != null) {
					// Try to get the complete code of the method or class
					String fullCode = getFullNodeCode(usageJNode);
					if (fullCode != null && !fullCode.isEmpty()) {
						return fullCode;
					}
				}
			}

			// If you cannot get more context, at least add some empty lines and comments
			return "// Unable to get complete context, only display related lines\n\n" + codeLine;
		}

		private String getFullNodeCode(JNode node) {
			if (node != null) {
				// Get the code information of the node
				ICodeInfo codeInfo = node.getCodeInfo();
				if (codeInfo != null && !codeInfo.equals(ICodeInfo.EMPTY)) {
					return codeInfo.getCodeStr();
				}

				// If it is a class node, try to get the class code
				if (node instanceof JClass) {
					JClass jClass = (JClass) node;
					return jClass.getCodeInfo().getCodeStr();
				}
			}
			return null;
		}

		private void scrollToCodeLine(RSyntaxTextArea textArea, String lineToHighlight) {
			// Try to find and highlight a specific line in the code and scroll to that position
			try {
				String fullText = textArea.getText();
				int lineIndex = fullText.indexOf(lineToHighlight);
				if (lineIndex >= 0) {
					// Ensure the text area has updated the layout
					textArea.revalidate();

					// Highlight the code line
					textArea.setCaretPosition(lineIndex);
					int endIndex = lineIndex + lineToHighlight.length();
					textArea.select(lineIndex, endIndex);
					textArea.getCaret().setSelectionVisible(true);

					// Use SwingUtilities.invokeLater to ensure the scroll is executed after the UI is updated
					SwingUtilities.invokeLater(() -> {
						try {
							// Get the line number
							int lineNum = textArea.getLineOfOffset(lineIndex);
							// Ensure the line is centered in the view
							Rectangle lineRect = textArea.modelToView(textArea.getLineStartOffset(lineNum));
							if (lineRect != null) {
								// Calculate the center point of the view
								JScrollPane scrollPane = (JScrollPane) textArea.getParent().getParent();
								Rectangle viewRect = scrollPane.getViewport().getViewRect();
								int y = lineRect.y - (viewRect.height - lineRect.height) / 2;
								if (y < 0) {
									y = 0;
								}
								// Scroll to the calculated position
								scrollPane.getViewport().setViewPosition(new Point(0, y));
							}
						} catch (Exception e) {
							LOG.debug("Error scrolling to line: {}", e.getMessage());
						}
					});
				} else {
					LOG.debug("Could not find line to highlight: {}", lineToHighlight);
				}
			} catch (Exception e) {
				LOG.debug("Error highlighting line: {}", e.getMessage());
			}
		}
	}

	@NotNull
	@Override
	protected JPanel initButtonsPanel() {
		progressPane = new ProgressPanel(mainWindow, false);

		JButton cancelButton = new JButton(NLS.str("search_dialog.cancel"));
		cancelButton.addActionListener(event -> dispose());
		JButton openBtn = new JButton(NLS.str("search_dialog.open"));
		openBtn.addActionListener(event -> openSelectedItem());
		getRootPane().setDefaultButton(openBtn);

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
		buttonPane.add(openBtn);
		buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPane.add(cancelButton);
		return buttonPane;
	}

	@Override
	protected void openSelectedItem() {
		// Get the currently selected node
		JNode node = getSelectedNode();
		if (node == null) {
			return;
		}
		openItem(node);
	}

	@Override
	protected void loadFinished() {
		// The tree loading is already handled
	}

	@Override
	protected void loadStart() {
		// The tree loading is already handled
	}

	@Nullable
	private JNode getSelectedNode() {
		try {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) usageTree.getLastSelectedPathComponent();
			if (node == null) {
				return null;
			}

			Object userObject = node.getUserObject();
			if (userObject instanceof JNode) {
				return (JNode) userObject;
			} else if (userObject instanceof CodeNode) {
				CodeNode codeNode = (CodeNode) userObject;
				return getNodeFromCodeNode(codeNode);
			}
			return null;
		} catch (Exception e) {
			LOG.error("Failed to get selected node", e);
			return null;
		}
	}
}
