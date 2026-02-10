package jadx.gui.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

import jadx.gui.logs.LogOptions;
import jadx.gui.strings.SingleStringResult;
import jadx.gui.strings.StringResult;
import jadx.gui.strings.StringResultGrouping;
import jadx.gui.treemodel.CodeNode;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.AbstractCodeArea;
import jadx.gui.ui.panel.ProgressPanel;
import jadx.gui.ui.tab.TabsController;
import jadx.gui.utils.CacheObject;
import jadx.gui.utils.JNodeCache;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.ui.NodeLabel;

public abstract class CommonStringsDialog extends JFrame {
	private static final Logger LOG = LoggerFactory.getLogger(CommonStringsDialog.class);
	private static final long serialVersionUID = 1691435101569167103L;

	protected static final int STRING_NODE_COLUMN_INDEX = 0;
	protected static final int CODE_NODE_COLUMN_INDEX = 1;

	protected final transient TabsController tabsController;
	protected final transient CacheObject cache;
	protected final transient MainWindow mainWindow;
	protected final transient Font codeFont;
	protected final transient String windowTitle;

	private final transient Map<StringResultGrouping, JTable> expandedTables;
	private final transient ChildTableSelection currentChildTableSelection;

	protected ResultsModel resultsModel;
	protected ResultsTable resultsTable;
	protected JLabel resultsInfoLabel;
	protected JLabel progressInfoLabel;
	protected JLabel warnLabel;
	protected ProgressPanel progressPane;

	private StringResultCodeUsageCellRenderer usageRenderer;

	public CommonStringsDialog(MainWindow mainWindow, String title) {
		this.mainWindow = mainWindow;
		this.tabsController = mainWindow.getTabsController();
		this.cache = mainWindow.getCacheObject();
		this.codeFont = mainWindow.getSettings().getCodeFont();
		this.windowTitle = title;
		this.expandedTables = new HashMap<>();
		this.currentChildTableSelection = new ChildTableSelection();
		UiUtils.setWindowIcons(this);
		updateTitle("");
	}

	protected abstract void openInit();

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

	protected void registerInitOnOpen() {
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(WindowEvent e) {
				SwingUtilities.invokeLater(CommonStringsDialog.this::openInit);
			}
		});
	}

	protected void openSelectedItem() {
		final StringResult node = getSelectedNode();
		if (node == null) {
			return;
		}

		openItem(node);
	}

	protected void openItem(final StringResult node) {
		if (node instanceof StringResultGrouping) {
			final StringResultGrouping grouping = (StringResultGrouping) node;
			final boolean currentlyExpanded = grouping.isExpanded();
			if (currentlyExpanded) {
				this.expandedTables.remove(grouping);
			} else {
				final JTable childTable = new StringsReferencesTable(CommonStringsDialog.this, CommonStringsDialog.this.usageRenderer,
						grouping.getResults(StringsPackageFilterDialog.STRINGS_PACKAGE_FILTERS),
						CommonStringsDialog.this.usageRenderer.getMaxRowHeight(), CommonStringsDialog.this::openItem);
				childTable.setSelectionBackground(resultsTable.getSelectionBackground());
				childTable.setSelectionForeground(resultsTable.getSelectionForeground());
				this.expandedTables.put(grouping, childTable);
			}
			grouping.setExpanded(!currentlyExpanded);
			return;
		} else if (node instanceof SingleStringResult) {
			final SingleStringResult singleResult = (SingleStringResult) node;
			openJNode(singleResult.getUnderlyingNode());
		}
	}

	protected void openJNode(final JNode node) {
		tabsController.codeJump(node);
		if (!mainWindow.getSettings().isKeepCommonDialogOpen()) {
			dispose();
		}
	}

	@Nullable
	private StringResult getSelectedNode() {
		final int selectedId = resultsTable.getSelectedRow();
		if (selectedId == -1 || selectedId >= resultsTable.getRowCount()) {
			return null;
		}
		return getNodeAtRow(selectedId);
	}

	@Nullable
	private StringResult getNodeAtRow(final int row) {
		try {
			return (StringResult) resultsModel.getValueAt(resultsTable.convertRowIndexToModel(row), STRING_NODE_COLUMN_INDEX);
		} catch (final Exception e) {
			LOG.error("Failed to get results table selected object", e);
			return null;
		}
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
		JButton openBtn = new JButton(NLS.str("search_dialog.open"));
		openBtn.addActionListener(event -> openSelectedItem());
		getRootPane().setDefaultButton(openBtn);

		JCheckBox cbKeepOpen = new JCheckBox(NLS.str("search_dialog.keep_open"));
		cbKeepOpen.setSelected(mainWindow.getSettings().isKeepCommonDialogOpen());
		cbKeepOpen.addActionListener(e -> {
			mainWindow.getSettings().saveKeepCommonDialogOpen(cbKeepOpen.isSelected());
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

	protected ResultsModel getResultsModel() {
		return new ResultsModel();
	}

	protected JPanel initResultsTable() {
		this.usageRenderer = new StringResultCodeUsageCellRenderer();
		StringResultIdentifierCellRenderer stringRenderer = new StringResultIdentifierCellRenderer(usageRenderer.getMaxRowHeight());
		resultsModel = getResultsModel();
		resultsModel.addTableModelListener(e -> updateProgressLabel(false));

		resultsTable = new ResultsTable(resultsModel, usageRenderer);
		resultsTable.setShowHorizontalLines(false);
		resultsTable.setDragEnabled(false);
		resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		resultsTable.setColumnSelectionAllowed(true);
		resultsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		resultsTable.setAutoscrolls(false);

		resultsTable.getColumnModel().getColumn(STRING_NODE_COLUMN_INDEX).setCellRenderer(stringRenderer);
		resultsTable.getColumnModel().getColumn(CODE_NODE_COLUMN_INDEX).setCellRenderer(usageRenderer);

		resultsTable.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					openSelectedItem();
				}
			}
		});
		// override copy action to copy long string of node column
		resultsTable.getActionMap().put("copy", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				StringResult selectedNode = getSelectedNode();
				if (selectedNode != null) {
					UiUtils.copyToClipboard(selectedNode.makeLongString());
				}
			}
		});

		resultsTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent ev) {
				final Point mouseLocation = ev.getPoint();

				final JTable childTable = getChildTableAtPoint(mouseLocation);
				if (childTable == null) {
					if (ev.getClickCount() == 2) {
						openSelectedItem();
					}
					return;
				}

				final int parentRow = resultsTable.rowAtPoint(mouseLocation);
				if (parentRow == -1) {
					return;
				}

				final int childRow = getChildTableRowAtPoint(childTable, mouseLocation);
				if (childRow == -1) {
					return;
				}

				final int parentRowForModel = resultsTable.convertRowIndexToModel(parentRow);
				currentChildTableSelection.createSelection(childTable, childRow, parentRowForModel);

				if (ev.getClickCount() == 2) {
					final Object cellObject = childTable.getValueAt(childRow, 0);
					if (!(cellObject instanceof JNode)) {
						throw new ClassCastException("All instances placed into the child table cells must be JNodes, not "
								+ cellObject.getClass().getSimpleName());
					}
					openJNode((JNode) cellObject);
				}
			}
		});

		resultsTable.getSelectionModel().addListSelectionListener((x) -> {
			if (this.currentChildTableSelection.hasSelection()) {
				this.currentChildTableSelection.removeSelection();
			}
		});

		warnLabel = new JLabel();
		warnLabel.setForeground(Color.RED);
		warnLabel.setVisible(false);

		JScrollPane scroll = new JScrollPane(resultsTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		resultsInfoLabel = new JLabel("");
		resultsInfoLabel.setFont(mainWindow.getSettings().getUiFont());

		progressInfoLabel = new JLabel("");
		progressInfoLabel.setFont(mainWindow.getSettings().getUiFont());
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

	@Nullable
	protected JTable getChildTableAtPoint(final Point mouseLocation) {
		final Point parentLocation = resultsTable.getLocationOnScreen();
		final Point mouseToParentLocation = (Point) mouseLocation.clone();
		mouseToParentLocation.x -= parentLocation.x;
		mouseToParentLocation.y -= parentLocation.y;

		final int parentColumn = resultsTable.columnAtPoint(mouseLocation);
		if (parentColumn != CODE_NODE_COLUMN_INDEX) {
			return null;
		}

		final int parentRow = resultsTable.rowAtPoint(mouseLocation);
		if (parentRow == -1) {
			return null;
		}

		final StringResult selectedNode = getNodeAtRow(parentRow);
		if (selectedNode == null) {
			return null;
		}
		if (!(selectedNode instanceof StringResultGrouping)) {
			return null;
		}

		final StringResultGrouping grouping = (StringResultGrouping) selectedNode;
		if (!grouping.isExpanded()) {
			return null;
		}

		return expandedTables.get(grouping);
	}

	protected int getChildTableRowAtPoint(final JTable childTable, final Point mouseLocation) {
		final int parentRow = resultsTable.rowAtPoint(mouseLocation);
		final int parentColumn = resultsTable.columnAtPoint(mouseLocation);
		final Point childLocation = resultsTable.getCellRect(parentRow, parentColumn, false).getLocation();
		final int dy = mouseLocation.y - childLocation.y;
		final int rowHeight = childTable.getRowHeight();

		return dy / rowHeight;
	}

	protected void updateProgressLabel(final boolean complete) {
		final int count = resultsModel.getRowCount();
		final String statusText;
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

	protected static final class ResultsTable extends JTable {
		private static final long serialVersionUID = 1310059573061176979L;

		private final transient ResultsModel model;

		public ResultsTable(ResultsModel resultsModel, StringResultCodeUsageCellRenderer renderer) {
			super(resultsModel);
			this.model = resultsModel;
			setRowHeight(renderer.getMaxRowHeight());
		}

		public void initColumnWidth() {
			int columnCount = getColumnCount();
			int width = getParent().getWidth() / columnCount;
			columnModel.getColumn(STRING_NODE_COLUMN_INDEX).setPreferredWidth(width);
			for (int col = 1; col < columnCount; col++) {
				columnModel.getColumn(col).setPreferredWidth(width);
			}
		}

		public void updateTable() {
			UiUtils.uiThreadGuard();
			int rowCount = getRowCount();
			if (rowCount == 0) {
				updateUI();
				return;
			}
			long start = System.currentTimeMillis();
			updateUI();
			if (LOG.isDebugEnabled()) {
				LOG.debug("Update results table in {}ms, count: {}", System.currentTimeMillis() - start, rowCount);
			}
		}

		@Override
		public Object getValueAt(int row, int column) {
			return model.getValueAt(convertRowIndexToModel(row), column);
		}

		@Override
		public Class<?> getColumnClass(int column) {
			final Class<?> cls;
			switch (column) {
				case STRING_NODE_COLUMN_INDEX:
					cls = StringResult.class;
					break;
				case CODE_NODE_COLUMN_INDEX:
					cls = JNode.class;
					break;
				default:
					cls = Object.class;
					break;
			}
			return cls;
		}
	}

	protected static final class ResultsModel extends AbstractTableModel {

		protected static final String[] DEFAULT_COLUMN_NAMES = { NLS.str("strings.string"), NLS.str("search_dialog.col_code") };
		private static final long serialVersionUID = -6828283192920568599L;

		private final String[] columnNames;

		private final transient List<StringResult> rows = new ArrayList<>();

		public ResultsModel() {
			this(DEFAULT_COLUMN_NAMES);
		}

		public ResultsModel(final String[] columnNames) {
			this.columnNames = columnNames;
		}

		public void addAll(final Collection<StringResult> nodes) {
			synchronized (rows) {
				addAndMergeAllResults(nodes);
			}
		}

		public void clear() {
			synchronized (rows) {
				rows.clear();
			}
		}

		public void sort() {
			synchronized (rows) {
				Collections.sort(rows);
			}
			fireTableDataChanged();
		}

		@Override
		public int getRowCount() {
			synchronized (rows) {
				return rows.size();
			}
		}

		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public String getColumnName(int index) {
			return columnNames[index];
		}

		@Override
		public @Nullable Object getValueAt(int rowIndex, int columnIndex) {
			final StringResult row;
			synchronized (rows) {
				if (rowIndex < rows.size()) {
					row = rows.get(rowIndex);
				} else {
					row = null;
				}
			}
			final Object value;
			switch (columnIndex) {
				case STRING_NODE_COLUMN_INDEX:
					value = row;
					break;
				case CODE_NODE_COLUMN_INDEX:
					value = getDisplayForResult(row);
					break;
				default:
					value = null;
					break;
			}
			return value;
		}

		private void addAndMergeAllResults(Collection<StringResult> nodes) {
			synchronized (rows) {
				for (final StringResult result : nodes) {
					final int resultIndex = rows.indexOf(result);
					if (resultIndex == -1) {
						rows.add(result);
					} else {
						final StringResult otherResult = rows.get(resultIndex);
						StringResult.mergeStringResults(result, otherResult, resultIndex, rows);
					}
				}
			}
			fireTableDataChanged();
		}

		@Nullable
		private Object getDisplayForResult(final StringResult result) {
			if (result == null) {
				return null;
			}

			final Class<? extends StringResult> resultClass = result.getClass();

			final Object display;
			if (resultClass.equals(SingleStringResult.class)) {
				final SingleStringResult singleResult = (SingleStringResult) result;
				display = singleResult.getUnderlyingNode();
			} else if (resultClass.equals(StringResultGrouping.class)) {
				display = result;
			} else {
				throw new ClassCastException("No display has been set for result type of " + result.getClass().getSimpleName());
			}
			return display;
		}

	}

	protected final class StringResultIdentifierCellRenderer implements TableCellRenderer {

		private final int rowHeight;

		public StringResultIdentifierCellRenderer(final int rowHeight) {
			this.rowHeight = rowHeight;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			if (value == null) {
				return new JLabel("");
			}

			final Component component;
			final Icon collapsedIcon = UIManager.getIcon("Tree.collapsedIcon");
			final Icon expandedIcon = UIManager.getIcon("Tree.expandedIcon");
			final int maxWidth = Math.max(collapsedIcon.getIconWidth(), expandedIcon.getIconWidth());

			if (!(value instanceof StringResultGrouping)) {
				Component buttonBox = Box.createRigidArea(new Dimension(maxWidth, rowHeight));
				JLabel label = new JLabel(value.toString());
				JPanel contentPanel = new JPanel();
				contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.X_AXIS));
				contentPanel.add(buttonBox);
				contentPanel.add(label);
				component = contentPanel;
			} else {
				final StringResultGrouping grouping = (StringResultGrouping) value;
				JLabel button = new JLabel();
				button.setSize(this.rowHeight, this.rowHeight);
				final Icon buttonIcon;
				if (grouping.isExpanded()) {
					buttonIcon = expandedIcon;
				} else {
					buttonIcon = collapsedIcon;
				}
				button.setIcon(buttonIcon);
				JPanel contentPanel = new JPanel();
				contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.X_AXIS));
				contentPanel.add(button);
				final int marginWidth = maxWidth - buttonIcon.getIconWidth();
				if (marginWidth != 0) {
					final Component margin = Box.createRigidArea(new Dimension(marginWidth, rowHeight));
					contentPanel.add(margin);
				}
				JLabel label = new JLabel(value.toString());
				contentPanel.add(label);
				component = contentPanel;
			}

			if (isSelected) {
				component.setBackground(table.getSelectionBackground());
				component.setForeground(table.getSelectionForeground());
			} else {
				component.setBackground(table.getBackground());
				component.setForeground(table.getForeground());
			}

			return component;
		}

	}

	protected final class StringResultCodeUsageCellRenderer implements TableCellRenderer {
		private final NodeLabel label;
		private final RSyntaxTextArea codeArea;
		private final NodeLabel emptyLabel;

		public StringResultCodeUsageCellRenderer() {
			codeArea = AbstractCodeArea.getDefaultArea(mainWindow);
			codeArea.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
			codeArea.setRows(1);
			label = new NodeLabel();
			label.setOpaque(true);
			label.setFont(codeArea.getFont());
			label.setHorizontalAlignment(SwingConstants.LEFT);
			emptyLabel = new NodeLabel();
			emptyLabel.setOpaque(true);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object obj, boolean isSelected,
				boolean hasFocus, int row, int column) {
			if (obj == null || table == null) {
				return emptyLabel;
			}

			if (!(obj instanceof JNode)) {
				return new JLabel(obj.toString());
			}

			final JNode node = (JNode) obj;
			final Component comp;
			if (node instanceof StringResultGrouping) {
				final StringResultGrouping grouping = (StringResultGrouping) node;
				comp = makeGroupingCell(grouping, table, row);
			} else {
				comp = makeCell(node, column, isSelected);
			}

			updateSelection(table, comp, isSelected);
			return comp;
		}

		private void updateSelection(JTable table, Component comp, boolean isSelected) {
			if (isSelected && !(comp instanceof JTable)) {
				comp.setBackground(table.getSelectionBackground());
				comp.setForeground(table.getSelectionForeground());
			} else {
				comp.setBackground(table.getBackground());
				comp.setForeground(table.getForeground());
			}
		}

		private Component makeGroupingCell(final StringResultGrouping grouping, final JTable table, final int row) {
			final Component component;
			if (grouping.isExpanded()) {
				component = expandedTables.get(grouping);
				table.setRowHeight(row, getCompHeight(component));
			} else {
				final JPanel panel = new JPanel();
				panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
				final StringBuilder sb = new StringBuilder("<...> (");
				final int count = grouping.getCount(StringsPackageFilterDialog.STRINGS_PACKAGE_FILTERS);
				sb.append(NLS.str("strings.references_count", count));
				sb.append(")");
				final JLabel label = new JLabel(sb.toString());
				panel.add(label);
				component = panel;
				table.setRowHeight(row, getMaxRowHeight());
			}

			return component;
		}

		private Component makeCell(JNode node, int column, boolean isSelected) {
			if (!(node instanceof CodeNode)) {
				JPanel container = new JPanel();
				container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));

				JLabel warningIcon = new JLabel();
				warningIcon.setIcon(UiUtils.openSvgIcon("ui/warning"));
				label.disableHtml(node.disableHtml());
				label.setText(node.makeLongStringHtml());
				label.setToolTipText(node.getTooltip());
				label.setIcon(node.getIcon());

				container.add(warningIcon);
				container.add(label);
				container.setToolTipText(NLS.str("strings.no_code"));

				updateSelection(resultsTable, label, isSelected);

				return container;
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

	protected void progressStartCommon() {
		progressPane.setIndeterminate(true);
		progressPane.setVisible(true);
		warnLabel.setVisible(false);
	}

	protected void progressFinishedCommon() {
		progressPane.setVisible(false);
	}

	protected JNodeCache getNodeCache() {
		return mainWindow.getCacheObject().getNodeCache();
	}

	private final class ChildTableSelection {
		private JTable childTable;
		private int parentRow;

		public boolean hasSelection() {
			return this.childTable != null;
		}

		public void createSelection(final JTable childTable, final int childRow, final int parentRow) {
			resultsTable.clearSelection();
			childTable.setRowSelectionInterval(childRow, childRow);
			resultsModel.fireTableRowsUpdated(parentRow, parentRow);

			this.parentRow = parentRow;
			this.childTable = childTable;
		}

		public void removeSelection() {
			this.childTable.clearSelection();
			resultsModel.fireTableRowsUpdated(this.parentRow, this.parentRow);

			this.childTable = null;
		}
	}
}
