package jadx.gui.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JResSearchNode;
import jadx.gui.ui.codearea.AbstractCodeArea;
import jadx.gui.utils.CacheObject;
import jadx.gui.utils.JumpPosition;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.search.TextSearchIndex;

public abstract class CommonSearchDialog extends JDialog {
	private static final long serialVersionUID = 8939332306115370276L;

	private static final Logger LOG = LoggerFactory.getLogger(CommonSearchDialog.class);

	public static final int RESULTS_PER_PAGE = 100;

	protected final transient TabbedPane tabbedPane;
	protected final transient CacheObject cache;
	protected final transient MainWindow mainWindow;
	protected final transient Font codeFont;

	protected ResultsModel resultsModel;
	protected ResultsTable resultsTable;
	protected JLabel resultsInfoLabel;
	protected JLabel warnLabel;
	protected ProgressPanel progressPane;

	protected String highlightText;
	protected boolean highlightTextCaseInsensitive = false;
	protected boolean highlightTextUseRegex = false;

	public CommonSearchDialog(MainWindow mainWindow) {
		super(mainWindow);
		this.mainWindow = mainWindow;
		this.tabbedPane = mainWindow.getTabbedPane();
		this.cache = mainWindow.getCacheObject();
		this.codeFont = mainWindow.getSettings().getFont();
	}

	protected abstract void openInit();

	protected abstract void loadFinished();

	protected abstract void loadStart();

	public void loadWindowPos() {
		if (!mainWindow.getSettings().loadWindowPos(this)) {
			setSize(800, 500);
		}
	}

	public void prepare() {
		if (cache.getIndexService().isComplete()) {
			loadFinishedCommon();
			loadFinished();
			return;
		}
		LoadTask task = new LoadTask();
		task.addPropertyChangeListener(progressPane);
		task.execute();
	}

	protected void registerInitOnOpen() {
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(WindowEvent e) {
				SwingUtilities.invokeLater(CommonSearchDialog.this::openInit);
			}
		});
	}

	protected synchronized void performSearch() {
		resultsTable.updateTable();
		updateProgressLabel();
	}

	protected void openSelectedItem() {
		int selectedId = resultsTable.getSelectedRow();
		if (selectedId == -1) {
			return;
		}
		JumpPosition jmpPos;
		JNode node = (JNode) resultsModel.getValueAt(selectedId, 0);
		if (node instanceof JResSearchNode) {
			jmpPos = new JumpPosition(((JResSearchNode) node).getResNode(), node.getLine(), node.getPos());
		} else {
			jmpPos = new JumpPosition(node.getRootClass(), node.getLine(), node.getPos());
		}
		tabbedPane.codeJump(jmpPos);
		if (!mainWindow.getSettings().getKeepCommonDialogOpen()) {
			dispose();
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

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

		JCheckBox cbKeepOpen = new JCheckBox(NLS.str("search_dialog.keep_open"));
		cbKeepOpen.setSelected(mainWindow.getSettings().getKeepCommonDialogOpen());
		cbKeepOpen.addActionListener(e -> {
			mainWindow.getSettings().setKeepCommonDialogOpen(cbKeepOpen.isSelected());
			mainWindow.getSettings().sync();
		});
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

	protected JPanel initResultsTable() {
		ResultsTableCellRenderer renderer = new ResultsTableCellRenderer();
		resultsModel = new ResultsModel(renderer);
		resultsModel.addTableModelListener(e -> updateProgressLabel());

		resultsTable = new ResultsTable(resultsModel, renderer);
		resultsTable.setShowHorizontalLines(false);
		resultsTable.setDragEnabled(false);
		resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		// resultsTable.setBackground(CodeArea.CODE_BACKGROUND);
		resultsTable.setColumnSelectionAllowed(false);
		resultsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		resultsTable.setAutoscrolls(false);

		resultsTable.setDefaultRenderer(Object.class, renderer);
		Enumeration<TableColumn> columns = resultsTable.getColumnModel().getColumns();
		while (columns.hasMoreElements()) {
			TableColumn column = columns.nextElement();
			column.setCellRenderer(renderer);
		}

		resultsTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() == 2) {
					openSelectedItem();
				}
			}
		});
		resultsTable.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					openSelectedItem();
				}
			}
		});

		warnLabel = new JLabel();
		warnLabel.setForeground(Color.RED);
		warnLabel.setVisible(false);

		JPanel resultsPanel = new JPanel();
		resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.PAGE_AXIS));
		resultsPanel.add(warnLabel);
		resultsPanel.add(new JScrollPane(resultsTable,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED));

		JPanel paginationPanel = new JPanel();
		paginationPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		paginationPanel.setLayout(new BoxLayout(paginationPanel, BoxLayout.X_AXIS));
		resultsInfoLabel = new JLabel("");

		JButton nextPageButton = new JButton("->");
		nextPageButton.setToolTipText(NLS.str("search_dialog.next_page"));
		nextPageButton.addActionListener(e -> {
			if (resultsModel.nextPage()) {
				switchPage(renderer);
			}
		});

		JButton prevPageButton = new JButton("<-");
		prevPageButton.setToolTipText(NLS.str("search_dialog.prev_page"));
		prevPageButton.addActionListener(e -> {
			if (resultsModel.prevPage()) {
				switchPage(renderer);
			}
		});

		paginationPanel.add(prevPageButton);
		paginationPanel.add(nextPageButton);
		paginationPanel.add(resultsInfoLabel);

		resultsPanel.add(paginationPanel);
		resultsPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		return resultsPanel;
	}

	private void switchPage(ResultsTableCellRenderer renderer) {
		renderer.clear();
		resultsTable.updateTable();
		updateProgressLabel();
		resultsTable.scrollRectToVisible(new Rectangle(0, 0, 1, 1));
	}

	protected void updateProgressLabel() {
		String statusText = NLS.str("search_dialog.info_label",
				resultsModel.getDisplayedResultsStart(),
				resultsModel.getDisplayedResultsEnd(),
				resultsModel.getResultCount());
		resultsInfoLabel.setText(statusText);
	}

	protected void showSearchState() {
		resultsInfoLabel.setText(NLS.str("search_dialog.tip_searching"));
	}

	protected static class ResultsTable extends JTable {
		private static final long serialVersionUID = 3901184054736618969L;
		private final transient ResultsTableCellRenderer renderer;

		public ResultsTable(ResultsModel resultsModel, ResultsTableCellRenderer renderer) {
			super(resultsModel);
			this.renderer = renderer;
		}

		public void updateTable() {
			ResultsModel model = (ResultsModel) getModel();
			TableColumnModel columnModel = getColumnModel();

			int width = getParent().getWidth();
			int firstColMaxWidth = (int) (width * 0.5);
			int rowCount = getRowCount();
			int columnCount = getColumnCount();
			boolean addDescColumn = model.isAddDescColumn();
			if (!addDescColumn) {
				firstColMaxWidth = width;
			}
			setRowHeight(10); // reset all rows height
			for (int col = 0; col < columnCount; col++) {
				int colWidth = 50;
				for (int row = 0; row < rowCount; row++) {
					Component comp = prepareRenderer(renderer, row, col);
					if (comp == null) {
						continue;
					}
					colWidth = Math.max(comp.getPreferredSize().width, colWidth);
					int h = Math.max(getRowHeight(row), getHeight(comp));
					if (h > 1) {
						setRowHeight(row, h);
					}
				}
				colWidth += 10;
				if (col == 0) {
					colWidth = Math.min(colWidth, firstColMaxWidth);
				} else {
					colWidth = Math.max(colWidth, width - columnModel.getColumn(0).getPreferredWidth());
				}
				TableColumn column = columnModel.getColumn(col);
				column.setPreferredWidth(colWidth);
			}
			updateUI();
		}

		private int getHeight(@Nullable Component nodeComp) {
			if (nodeComp != null) {
				return Math.max(nodeComp.getHeight(), nodeComp.getPreferredSize().height);
			}
			return 0;
		}
	}

	protected static class ResultsModel extends AbstractTableModel {
		private static final long serialVersionUID = -7821286846923903208L;
		private static final String[] COLUMN_NAMES = {
				NLS.str("search_dialog.col_node"),
				NLS.str("search_dialog.col_code")
		};

		private final transient ArrayList<JNode> rows = new ArrayList<>();
		private final transient ResultsTableCellRenderer renderer;
		private transient boolean addDescColumn;
		private transient int start = 0;

		public ResultsModel(ResultsTableCellRenderer renderer) {
			this.renderer = renderer;
		}

		protected void addAll(Collection<? extends JNode> nodes) {
			rows.ensureCapacity(rows.size() + nodes.size());
			rows.addAll(nodes);
			if (!addDescColumn) {
				for (JNode row : rows) {
					if (row.hasDescString()) {
						addDescColumn = true;
						break;
					}
				}
			}
		}

		public void clear() {
			start = 0;
			addDescColumn = false;
			rows.clear();
			renderer.clear();
		}

		public boolean isAddDescColumn() {
			return addDescColumn;
		}

		public int getResultCount() {
			return rows.size();
		}

		public int getDisplayedResultsStart() {
			if (rows.isEmpty()) {
				return 0;
			}
			return start + 1;
		}

		public int getDisplayedResultsEnd() {
			return Math.min(rows.size(), start + RESULTS_PER_PAGE);
		}

		public boolean nextPage() {
			if (start + RESULTS_PER_PAGE < rows.size()) {
				start += RESULTS_PER_PAGE;
				return true;
			}
			return false;
		}

		public boolean prevPage() {
			if (start - RESULTS_PER_PAGE >= 0) {
				start -= RESULTS_PER_PAGE;
				return true;
			}
			return false;
		}

		@Override
		public int getRowCount() {
			if (rows.isEmpty()) {
				return 0;
			}
			return getDisplayedResultsEnd() - start;
		}

		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public String getColumnName(int index) {
			return COLUMN_NAMES[index];
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			return rows.get(rowIndex + start);
		}
	}

	protected class ResultsTableCellRenderer implements TableCellRenderer {
		private final JLabel emptyLabel = new JLabel();
		private final Font font;
		private final Color codeSelectedColor;
		private final Color codeBackground;
		private final Map<Integer, Component> componentCache = new HashMap<>();

		public ResultsTableCellRenderer() {
			RSyntaxTextArea area = AbstractCodeArea.getDefaultArea(mainWindow);
			this.font = area.getFont();
			this.codeSelectedColor = area.getSelectionColor();
			this.codeBackground = area.getBackground();
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object obj, boolean isSelected,
				boolean hasFocus, int row, int column) {
			int id = makeID(row, column);
			Component comp = componentCache.get(id);
			if (comp == null) {
				if (obj instanceof JNode) {
					comp = makeCell((JNode) obj, column);
					componentCache.put(id, comp);
				} else {
					comp = emptyLabel;
				}
			}
			updateSelection(table, comp, isSelected);
			return comp;
		}

		private int makeID(int row, int col) {
			return row << 2 | col;
		}

		private void updateSelection(JTable table, Component comp, boolean isSelected) {
			if (comp instanceof RSyntaxTextArea) {
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
				JLabel label = new JLabel(node.makeLongStringHtml() + "  ", node.getIcon(), SwingConstants.LEFT);
				label.setFont(font);
				label.setOpaque(true);
				label.setToolTipText(label.getText());
				return label;
			}
			if (!node.hasDescString()) {
				return emptyLabel;
			}

			RSyntaxTextArea textArea = AbstractCodeArea.getDefaultArea(mainWindow);
			textArea.setSyntaxEditingStyle(node.getSyntaxName());
			String descStr = node.makeDescString();
			textArea.setText(descStr);
			if (descStr.contains("\n")) {
				textArea.setRows(textArea.getLineCount());
			} else {
				textArea.setRows(1);
				textArea.setColumns(descStr.length() + 1);
			}
			if (highlightText != null) {
				SearchContext searchContext = new SearchContext(highlightText);
				searchContext.setMatchCase(!highlightTextCaseInsensitive);
				searchContext.setRegularExpression(highlightTextUseRegex);
				searchContext.setMarkAll(true);
				SearchEngine.markAll(textArea, searchContext);
			}
			textArea.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
			return textArea;
		}

		public void clear() {
			componentCache.clear();
		}
	}

	private class LoadTask extends SwingWorker<Void, Void> {
		public LoadTask() {
			loadStartCommon();
			loadStart();
		}

		@Override
		public Void doInBackground() {
			try {
				progressPane.changeLabel(this, NLS.str("progress.decompile") + ": ");
				mainWindow.waitDecompileTask();
			} catch (Exception e) {
				LOG.error("Waiting background tasks failed", e);
			}
			return null;
		}

		@Override
		public void done() {
			loadFinishedCommon();
			loadFinished();
		}
	}

	private void loadStartCommon() {
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		progressPane.setIndeterminate(true);
		progressPane.setVisible(true);
		warnLabel.setVisible(false);
	}

	private void loadFinishedCommon() {
		setCursor(null);
		progressPane.setVisible(false);

		TextSearchIndex textIndex = cache.getTextIndex();
		if (textIndex == null) {
			warnLabel.setText(NLS.str("msg.index_not_initialized"));
			warnLabel.setVisible(true);
		}
	}
}
