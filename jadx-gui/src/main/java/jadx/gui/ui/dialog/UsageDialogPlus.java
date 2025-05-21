package jadx.gui.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.TableCellRenderer;

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
import jadx.gui.settings.JadxSettings;
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
import jadx.gui.utils.ui.NodeLabel;

public class UsageDialogPlus extends CommonSearchDialog {
	private static final long serialVersionUID = -5105405789969134107L;

	private final List<UsagePanel> usagePanels = new ArrayList<>();
	private final JPanel mainPanel;
	private final JSplitPane splitPane;
	private final CodePanel codePanel;

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
				usageDialog.splitPane.setDividerLocation((int) (width * 0.7));
			}
		});
	}

	private UsageDialogPlus(MainWindow mainWindow, JNode node) {
		super(mainWindow, NLS.str("usage_dialog_plus.title"));

		// Initialize the progress panel and warning label
		progressPane = new ProgressPanel(mainWindow, false);
		warnLabel = new JLabel();
		warnLabel.setForeground(Color.RED);
		warnLabel.setVisible(false);

		mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));

		// Create the code panel
		codePanel = new CodePanel(mainWindow);

		// Create the split panel, allowing horizontal scrolling
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.setOneTouchExpandable(true);
		splitPane.setContinuousLayout(true);
		splitPane.setResizeWeight(0.7); // Left 70%
		splitPane.setDividerSize(10); // Increase the width of the divider for easier dragging

		// Need a wrapper panel to support horizontal scrolling
		JScrollPane scrollPane = new JScrollPane(mainPanel);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

		// Create the first panel but do not initialize it
		UsagePanel panel = new UsagePanel(this, node, usagePanels.size());
		usagePanels.add(panel);
		mainPanel.add(panel);

		initUI();
		registerInitOnOpen();
		loadWindowPos();
	}

	private void addUsagePanel(JNode node) {
		// If there is already a panel, update the panel on the right
		if (!usagePanels.isEmpty()) {
			int lastIndex = usagePanels.size() - 1;
			// If there are multiple panels, remove all panels on the right of the last panel
			while (usagePanels.size() > lastIndex + 1) {
				UsagePanel panelToRemove = usagePanels.get(usagePanels.size() - 1);
				mainPanel.remove(panelToRemove);
				usagePanels.remove(usagePanels.size() - 1);
			}
		}

		// Create a new panel and add it
		UsagePanel panel = new UsagePanel(this, node, usagePanels.size());
		usagePanels.add(panel);
		mainPanel.add(panel);
		mainPanel.revalidate();
		mainPanel.repaint();

		// If a new panel is added, scroll to the right
		scrollToRight();

		// Initialize the new added panel
		panel.openInit();
	}

	private void scrollToRight() {
		// Ensure the layout is updated
		mainPanel.revalidate();

		// Get the scroll panel
		JScrollPane scrollPane = (JScrollPane) splitPane.getLeftComponent();

		// Use SwingUtilities.invokeLater to ensure the scroll is executed after the UI is updated
		SwingUtilities.invokeLater(() -> {
			// Get the horizontal scroll bar
			JScrollBar horizontalScrollBar = scrollPane.getHorizontalScrollBar();
			// Scroll to the right
			horizontalScrollBar.setValue(horizontalScrollBar.getMaximum());
		});
	}

	@Override
	protected void openInit() {
		// Only initialize the first panel, other panels will be initialized automatically when added
		if (!usagePanels.isEmpty()) {
			usagePanels.get(0).openInit();
		}
	}

	@Override
	protected void loadFinished() {
		// This method is handled separately in each UsagePanel
	}

	@Override
	protected void loadStart() {
		// This method is handled separately in each UsagePanel
	}

	private void initUI() {
		JadxSettings settings = mainWindow.getSettings();
		Font codeFont = settings.getFont();

		JScrollPane scrollPane = new JScrollPane(mainPanel);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

		splitPane.setLeftComponent(scrollPane);
		splitPane.setRightComponent(codePanel); // Set the right side to the code panel

		initCommon();
		JPanel buttonPane = initButtonsPanel();

		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BorderLayout(5, 5));
		contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPanel.add(splitPane, BorderLayout.CENTER);
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
					if (ratio < 0.3 || ratio > 0.85) {
						splitPane.setDividerLocation((int) (width * 0.7)); // Use pixel values
					}
				}
			}
		});
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
			setPreferredSize(new Dimension(400, 600));

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

	private class UsagePanel extends JPanel {
		private final UsageDialogPlus parentDialog;
		private final JNode node;
		private final int index;
		private List<CodeNode> usageList;
		private ResultsModel resultsModel;
		private JTable resultsTable;
		private JLabel resultsInfoLabel;
		private JLabel progressInfoLabel;
		private ProgressPanel localProgressPanel; // Add local progress panel

		// Unify the height of all panels.
		private static final int PANEL_HEIGHT = 650;
		private static final int PANEL_WIDTH = 450; // Increase width to display more node content

		public UsagePanel(UsageDialogPlus parentDialog, JNode node, int index) {
			this.parentDialog = parentDialog;
			this.node = node;
			this.index = index;

			setLayout(new BorderLayout(5, 5));
			setBorder(BorderFactory.createTitledBorder("Usage " + (index + 1)));

			// Set the unified panel size
			setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
			setMinimumSize(new Dimension(250, PANEL_HEIGHT)); // Set the minimum width to 250

			initPanelUI();
		}

		private void initPanelUI() {
			// Initialize the local progress panel
			localProgressPanel = new ProgressPanel(mainWindow, false);

			// Search panel - use a text area instead of a label to support automatic line wrapping
			JPanel searchPane = new JPanel(new BorderLayout());
			JLabel lbl = new JLabel(NLS.str("usage_dialog.label"));
			lbl.setFont(codeFont);

			// Use NodeLabel.longName to display node information, including icons
			JLabel nodeLabel = NodeLabel.longName(node);
			nodeLabel.setFont(codeFont);
			lbl.setLabelFor(nodeLabel);

			JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			labelPanel.add(lbl);

			searchPane.add(labelPanel, BorderLayout.NORTH);
			searchPane.add(nodeLabel, BorderLayout.CENTER);
			searchPane.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10)); // Increase the top and bottom margins

			// Result table - initialized exactly as CommonSearchDialog.
			EnhancedCellRenderer renderer = new EnhancedCellRenderer();
			resultsModel = new ResultsModel();
			resultsTable = new JTable(resultsModel);
			resultsTable.setRowHeight(renderer.getMaxRowHeight());
			resultsTable.setShowHorizontalLines(false);
			resultsTable.setDragEnabled(false);
			resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			resultsTable.setColumnSelectionAllowed(false);
			resultsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // Set to not automatically adjust column widths
			resultsTable.setAutoscrolls(true); // Enable automatic scrolling

			resultsTable.setDefaultRenderer(Object.class, renderer);

			// Remove the second column (code column)
			if (resultsTable.getColumnCount() > 1) {
				resultsTable.removeColumn(resultsTable.getColumnModel().getColumn(1));
			}

			// Add a selection listener to display code when a node is selected
			resultsTable.getSelectionModel().addListSelectionListener(e -> {
				if (!e.getValueIsAdjusting()) {
					int selectedRow = resultsTable.getSelectedRow();
					if (selectedRow >= 0 && selectedRow < resultsModel.getRowCount()) {
						JNode selectedNode = (JNode) resultsModel.getValueAt(selectedRow, 0);
						if (selectedNode instanceof CodeNode) {
							CodeNode codeNode = (CodeNode) selectedNode;
							codePanel.showCode(codeNode, codeNode.makeDescString());
						} else {
							codePanel.showCode(selectedNode, selectedNode.makeDescString());
						}
					}
				}
			});

			resultsTable.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() == 2) {
						int row = resultsTable.rowAtPoint(e.getPoint());
						if (row >= 0) {
							JNode selectedNode = (JNode) resultsModel.getValueAt(row, 0);
							if (selectedNode != null) {
								// Get the actual node clicked
								JNode nodeToUse = selectedNode;

								// If it is a CodeNode, we need to get the actual node it references
								if (selectedNode instanceof CodeNode) {
									CodeNode codeNode = (CodeNode) selectedNode;
									// Use JavaNode to determine the correct node type
									JavaNode javaNode = codeNode.getJavaNode();
									// Use NodeCache to convert JavaNode to JNode
									JNodeCache nodeCache = getNodeCache();
									nodeToUse = nodeCache.makeFrom(javaNode);
								}

								// If the current panel is not the last panel, update the right panel
								// Otherwise, add a new panel
								if (index < usagePanels.size() - 1) {
									// Update the right panel
									updateRightPanel(nodeToUse);
								} else {
									// Add a new panel
									parentDialog.addUsagePanel(nodeToUse);
								}
								// Whether updating or adding, scroll to the right
								parentDialog.scrollToRight();
							}
						}
					} else if (e.getButton() == MouseEvent.BUTTON3) {
						int row = resultsTable.rowAtPoint(e.getPoint());
						if (row >= 0) {
							resultsTable.setRowSelectionInterval(row, row);
							JNode selectedNode = (JNode) resultsModel.getValueAt(row, 0);
							if (selectedNode != null) {
								showPopupMenu(e, selectedNode);
							}
						}
					}
				}
			});

			JScrollPane scrollPane = new JScrollPane(resultsTable);
			scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			// Set the preferred scroll size of the table to support displaying longer content
			resultsTable.setPreferredScrollableViewportSize(new Dimension(PANEL_WIDTH - 50, 500));

			// Ensure the table can scroll to display long content
			resultsTable.getTableHeader().setResizingAllowed(true);
			resultsTable.getTableHeader().setReorderingAllowed(false);

			// Status panel
			JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			resultsInfoLabel = new JLabel("");
			progressInfoLabel = new JLabel("");

			statusPanel.add(resultsInfoLabel);
			statusPanel.add(Box.createRigidArea(new Dimension(10, 0)));
			statusPanel.add(progressInfoLabel);
			statusPanel.add(Box.createRigidArea(new Dimension(10, 0)));
			statusPanel.add(localProgressPanel); // Use the local progress panel

			// Main panel layout
			add(searchPane, BorderLayout.NORTH);
			add(scrollPane, BorderLayout.CENTER);
			add(statusPanel, BorderLayout.SOUTH);
		}

		private void showPopupMenu(MouseEvent e, JNode node) {
			JPopupMenu popup = new JPopupMenu();

			JMenuItem jumpToItem = new JMenuItem(NLS.str("usage_dialog_plus.jump_to"));
			jumpToItem.addActionListener(evt -> {
				parentDialog.openItem(node);
			});

			JMenuItem copyPathItem = new JMenuItem(NLS.str("usage_dialog_plus.copy_path"));
			copyPathItem.addActionListener(evt -> {
				copyUsagePath(node);
			});

			popup.add(jumpToItem);
			popup.add(copyPathItem);
			popup.show(e.getComponent(), e.getX(), e.getY());
		}

		private void copyUsagePath(JNode node) {
			if (node != null) {
				// Build the complete path from the leftmost node to the current node
				StringBuilder pathBuilder = new StringBuilder();

				// Get the node path from all panels from left to right
				List<JNode> nodePath = new ArrayList<>();

				// Add the nodes from all panels from left to right
				for (int i = 0; i <= index; i++) {
					UsagePanel panel = usagePanels.get(i);
					nodePath.add(panel.node);
				}

				// Add the currently selected node (if it is different from the current panel node)
				if (!node.equals(nodePath.get(nodePath.size() - 1))) {
					nodePath.add(node);
				}
				Collections.reverse(nodePath);
				// Build the path string in the required format: the leftmost node is at the top, and each level
				// adds indentation and an arrow
				// For example: a\n -> b\n -> c\n -> d

				// Build the path string
				for (int i = 0; i < nodePath.size(); i++) {
					if (i > 0) {
						pathBuilder.append("\n");
						// Add indentation (add one space for each level)
						for (int j = 0; j < i; j++) {
							pathBuilder.append(" ");
						}
						pathBuilder.append("-> ");
					}

					// Add node information
					JNode currentNode = nodePath.get(i);
					pathBuilder.append(currentNode.getJavaNode().getCodeNodeRef().toString());
				}

				// Copy to clipboard
				UiUtils.copyToClipboard(pathBuilder.toString());
			}
		}

		public void openInit() {
			localProgressPanel.setIndeterminate(true);
			localProgressPanel.setVisible(true);
			prepareUsageData();
			mainWindow.getBackgroundExecutor().execute(NLS.str("progress.load"),
					this::collectUsageData,
					(status) -> {
						if (status == TaskStatus.CANCEL_BY_MEMORY) {
							mainWindow.showHeapUsageBar();
							UiUtils.errorMessage(UsageDialogPlus.this, NLS.str("message.memoryLow"));
						}
						localProgressPanel.setVisible(false);
						onLoadFinished();
					});
		}

		private void prepareUsageData() {
			if (mainWindow.getSettings().isReplaceConsts() && node instanceof JField) {
				FieldNode fld = ((JField) node).getJavaField().getFieldNode();
				boolean constField = CollectConstValues.getFieldConstValue(fld) != null;
				if (constField && !fld.getAccessFlags().isPrivate()) {
					// run full decompilation to prepare for full code scan
					mainWindow.requestFullDecompilation();
				}
			}
		}

		private void collectUsageData() {
			usageList = new ArrayList<>();
			buildUsageQuery().forEach(
					(searchNode, useNodes) -> useNodes.stream()
							.map(JavaNode::getTopParentClass)
							.distinct()
							.forEach(u -> processUsage(searchNode, u)));
		}

		private Map<JavaNode, List<? extends JavaNode>> buildUsageQuery() {
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

		private void processUsage(JavaNode searchNode, JavaClass topUseClass) {
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

		private void onLoadFinished() {
			resultsTable.setEnabled(true);
			resultsModel.clear();

			Collections.sort(usageList);
			resultsModel.addAll(usageList);
			updateHighlightContext(node.getName(), true, false, true);
			updateTableUI();

			updateProgressLabel(true);

			// If there are results, select the first one to display code
			if (!usageList.isEmpty()) {
				resultsTable.setRowSelectionInterval(0, 0);
				CodeNode firstNode = usageList.get(0);
				codePanel.showCode(firstNode, firstNode.makeDescString());
			}
		}

		private void updateTableUI() {
			if (resultsTable != null) {
				// Set a sufficiently large column width to display complete content
				int columnCount = resultsTable.getColumnCount();
				int width = Math.max(resultsTable.getParent().getWidth(), PANEL_WIDTH);
				// Use a larger width value to display more content
				int colWidth = width + 500; // Use a sufficiently large width to ensure content can be fully displayed
				resultsTable.getColumnModel().getColumn(0).setPreferredWidth(colWidth);
				for (int col = 1; col < columnCount; col++) {
					resultsTable.getColumnModel().getColumn(col).setPreferredWidth(width);
				}

				// Update the table UI
				resultsTable.updateUI();
			}
		}

		private void updateProgressLabel(boolean complete) {
			if (complete) {
				progressInfoLabel.setText(NLS.str("usage_dialog_plus.search_complete"));
			} else {
				progressInfoLabel.setText(NLS.str("search_dialog.tip_searching"));
			}

			// Update the results information label
			resultsInfoLabel.setText(NLS.str("search_dialog.results_complete", usageList.size()));
		}

		// Update the right panel method
		private void updateRightPanel(JNode selectedNode) {
			// Get the right panel index
			int rightPanelIndex = index + 1;

			// If the right panel does not exist, add a new panel
			if (rightPanelIndex >= usagePanels.size()) {
				parentDialog.addUsagePanel(selectedNode);
				return;
			}

			// Get the right panel
			UsagePanel rightPanel = usagePanels.get(rightPanelIndex);
			// Create a new panel to replace the right panel
			UsagePanel newPanel = new UsagePanel(parentDialog, selectedNode, rightPanelIndex);

			// Replace the panel
			mainPanel.remove(rightPanel);
			mainPanel.add(newPanel, rightPanelIndex);
			usagePanels.set(rightPanelIndex, newPanel);

			// Remove all panels to the right
			while (usagePanels.size() > rightPanelIndex + 1) {
				UsagePanel panelToRemove = usagePanels.get(usagePanels.size() - 1);
				mainPanel.remove(panelToRemove);
				usagePanels.remove(usagePanels.size() - 1);
			}

			// Update UI
			mainPanel.revalidate();
			mainPanel.repaint();

			// Initialize the new panel
			newPanel.openInit();

			// Scroll to the right
			parentDialog.scrollToRight();
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
		// Delete the copy all button

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
		// Do not add the copy all button
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

	@Nullable
	private JNode getSelectedNode() {
		try {
			// Iterate through all panels to find the selected node
			for (UsagePanel panel : usagePanels) {
				int selectedRow = panel.resultsTable.getSelectedRow();
				if (selectedRow != -1 && selectedRow < panel.resultsModel.getRowCount()) {
					return (JNode) panel.resultsModel.getValueAt(selectedRow, 0);
				}
			}
			return null;
		} catch (Exception e) {
			LOG.error("Failed to get results table selected object", e);
			return null;
		}
	}

	// Custom table cell renderer, fully implemented by referring to the ResultsTableCellRenderer in
	// CommonSearchDialog.
	private class EnhancedCellRenderer implements TableCellRenderer {
		private final NodeLabel label;
		private final RSyntaxTextArea codeArea;
		private final NodeLabel emptyLabel;
		private final Color codeSelectedColor;
		private final Color codeBackground;

		public EnhancedCellRenderer() {
			codeArea = AbstractCodeArea.getDefaultArea(mainWindow);
			codeArea.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
			codeArea.setRows(1);
			codeBackground = codeArea.getBackground();
			codeSelectedColor = codeArea.getSelectionColor();
			label = new NodeLabel();
			label.setOpaque(true);
			label.setFont(codeArea.getFont());
			label.setHorizontalAlignment(SwingConstants.LEFT);
			emptyLabel = new NodeLabel();
			emptyLabel.setOpaque(true);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object obj, boolean isSelected, boolean hasFocus,
				int row,
				int column) {
			if (obj == null || table == null) {
				return emptyLabel;
			}
			Component comp = makeCell((JNode) obj, column);
			updateSelection(table, comp, column, isSelected);
			return comp;
		}

		private void updateSelection(JTable table, Component comp, int column, boolean isSelected) {
			if (column == 1) {
				if (isSelected) {
					comp.setBackground(codeSelectedColor);
				} else {
					comp.setBackground(codeBackground);
				}
			} else {
				if (isSelected) {
					comp.setBackground(table.getSelectionBackground());
					comp.setForeground(table.getSelectionForeground());
				} else {
					comp.setBackground(table.getBackground());
					comp.setForeground(table.getForeground());
				}
			}
		}

		private Component makeCell(JNode node, int column) {
			if (column == 0) {
				label.disableHtml(node.disableHtml());
				// Use the complete node information, not truncated
				String nodeText = node.makeLongStringHtml();
				label.setText(nodeText);
				label.setToolTipText(node.getTooltip());
				label.setIcon(node.getIcon());
				return label;
			}
			if (!node.hasDescString()) {
				return emptyLabel;
			}
			codeArea.setSyntaxEditingStyle(node.getSyntaxName());
			String descStr = node.makeDescString();
			codeArea.setText(descStr);
			codeArea.setColumns(descStr.length() + 1);
			return codeArea;
		}

		public int getMaxRowHeight() {
			label.setText("Text");
			codeArea.setText("Text");
			return Math.max(getCompHeight(label), getCompHeight(codeArea));
		}

		private int getCompHeight(Component comp) {
			return Math.max(comp.getHeight(), comp.getPreferredSize().height);
		}
	}
}
