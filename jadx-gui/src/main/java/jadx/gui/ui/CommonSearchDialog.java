package jadx.gui.ui;

import jadx.gui.jobs.BackgroundJob;
import jadx.gui.jobs.BackgroundWorker;
import jadx.gui.jobs.DecompileJob;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.TextNode;
import jadx.gui.utils.CacheObject;
import jadx.gui.utils.NLS;
import jadx.gui.utils.Position;
import jadx.gui.utils.search.TextSearchIndex;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CommonSearchDialog extends JDialog {

	private static final Logger LOG = LoggerFactory.getLogger(CommonSearchDialog.class);
	private static final long serialVersionUID = 8939332306115370276L;

	public static final int MAX_RESULTS_COUNT = 100;

	protected final TabbedPane tabbedPane;
	protected final CacheObject cache;
	protected final MainWindow mainWindow;
	protected final Font codeFont;

	protected ResultsModel resultsModel;
	protected ResultsTable resultsTable;
	protected JLabel warnLabel;
	protected ProgressPanel progressPane;

	protected String highlightText;

	public CommonSearchDialog(MainWindow mainWindow) {
		super(mainWindow);
		this.mainWindow = mainWindow;
		this.tabbedPane = mainWindow.getTabbedPane();
		this.cache = mainWindow.getCacheObject();
		this.codeFont = mainWindow.getSettings().getFont();
	}

	public void loadWindowPos() {
		mainWindow.getSettings().loadWindowPos(this);
	}

	public void prepare() {
		if (cache.getIndexJob().isComplete()) {
			loadFinishedCommon();
			loadFinished();
			return;
		}
		LoadTask task = new LoadTask();
		task.addPropertyChangeListener(progressPane);
		task.execute();
	}

	protected void openSelectedItem() {
		int selectedId = resultsTable.getSelectedRow();
		if (selectedId == -1) {
			return;
		}
		JNode node = (JNode) resultsModel.getValueAt(selectedId, 0);
		tabbedPane.codeJump(new Position(node.getRootClass(), node.getLine()));

		dispose();
	}

	@Override
	public void dispose() {
		mainWindow.getSettings().saveWindowPos(this);
		super.dispose();
	}

	protected void initCommon() {
		KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		getRootPane().registerKeyboardAction(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		}, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	@NotNull
	protected JPanel initButtonsPanel() {
		progressPane = new ProgressPanel(mainWindow, false);

		JButton cancelButton = new JButton(NLS.str("search_dialog.cancel"));
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				dispose();
			}
		});
		JButton openBtn = new JButton(NLS.str("search_dialog.open"));
		openBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				openSelectedItem();
			}
		});
		getRootPane().setDefaultButton(openBtn);

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
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
		resultsTable = new ResultsTable(resultsModel);
		resultsTable.setShowHorizontalLines(false);
		resultsTable.setDragEnabled(false);
		resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		resultsTable.setBackground(CodeArea.BACKGROUND);
		resultsTable.setColumnSelectionAllowed(false);
		resultsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		resultsTable.setAutoscrolls(false);

		Enumeration<TableColumn> columns = resultsTable.getColumnModel().getColumns();
		while (columns.hasMoreElements()) {
			TableColumn column = columns.nextElement();
			column.setCellRenderer(renderer);
		}

		resultsTable.addMouseListener(new MouseAdapter() {
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
		resultsPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		return resultsPanel;
	}

	protected static class ResultsTable extends JTable {
		private static final long serialVersionUID = 3901184054736618969L;

		public ResultsTable(ResultsModel resultsModel) {
			super(resultsModel);
		}

		public void updateTable() {
			ResultsModel model = (ResultsModel) getModel();
			TableColumnModel columnModel = getColumnModel();

			int width = getParent().getWidth();
			int firstColMaxWidth = (int) (width * 0.5);
			int rowCount = getRowCount();
			int columnCount = getColumnCount();
			if (!model.isAddDescColumn()) {
				firstColMaxWidth = width;
			}
			Component codeComp = null;
			for (int col = 0; col < columnCount; col++) {
				int colWidth = 50;
				for (int row = 0; row < rowCount; row++) {
					TableCellRenderer renderer = getCellRenderer(row, col);
					Component comp = prepareRenderer(renderer, row, col);
					if (comp == null) {
						continue;
					}
					colWidth = Math.max(comp.getPreferredSize().width, colWidth);
					if (codeComp == null && col == 1) {
						codeComp = comp;
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
			if (codeComp != null) {
				setRowHeight(Math.max(20, codeComp.getPreferredSize().height + 4));
			}
			updateUI();
		}
	}

	protected static class ResultsModel extends AbstractTableModel {
		private static final long serialVersionUID = -7821286846923903208L;
		private static final String[] COLUMN_NAMES = {"Node", "Code"};

		private final List<JNode> rows = new ArrayList<JNode>();
		private final ResultsTableCellRenderer renderer;
		private boolean addDescColumn;

		public ResultsModel(ResultsTableCellRenderer renderer) {
			this.renderer = renderer;
		}

		protected void addAll(Iterable<? extends JNode> nodes) {
			for (JNode node : nodes) {
				int size = getRowCount();
				if (size >= MAX_RESULTS_COUNT) {
					if (size == MAX_RESULTS_COUNT) {
						add(new TextNode("Search results truncated (limit: " + MAX_RESULTS_COUNT + ")"));
					}
					return;
				}
				add(node);
			}
		}

		private void add(JNode node) {
			if (node.hasDescString()) {
				addDescColumn = true;
			}
			rows.add(node);
		}

		public void clear() {
			addDescColumn = false;
			rows.clear();
			renderer.clear();
		}

		public boolean isAddDescColumn() {
			return addDescColumn;
		}

		@Override
		public int getRowCount() {
			return rows.size();
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
			return rows.get(rowIndex);
		}
	}

	protected class ResultsTableCellRenderer implements TableCellRenderer {
		private final Color selectedBackground;
		private final Color selectedForeground;
		private final Color foreground;

		private final JLabel emptyLabel = new JLabel();

		private Map<Integer, Component> componentCache = new HashMap<Integer, Component>();

		public ResultsTableCellRenderer() {
			UIDefaults defaults = UIManager.getDefaults();
			foreground = defaults.getColor("List.foreground");
			selectedBackground = defaults.getColor("List.selectionBackground");
			selectedForeground = defaults.getColor("List.selectionForeground");
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object obj, boolean isSelected,
				boolean hasFocus, int row, int column) {
			int id = row << 2 | column;
			Component comp = componentCache.get(id);
			if (comp == null) {
				if (obj instanceof JNode) {
					comp = makeCell((JNode) obj, column);
					componentCache.put(id, comp);
				} else {
					comp = emptyLabel;
				}
			}
			updateSelection(comp, isSelected);
			return comp;
		}

		private void updateSelection(Component comp, boolean isSelected) {
			if (isSelected) {
				comp.setBackground(selectedBackground);
				comp.setForeground(selectedForeground);
			} else {
				comp.setBackground(CodeArea.BACKGROUND);
				comp.setForeground(foreground);
			}
		}

		private Component makeCell(JNode node, int column) {
			if (column == 0) {
				JLabel label = new JLabel(node.makeLongString() + "  ", node.getIcon(), SwingConstants.LEFT);
				label.setOpaque(true);
				label.setToolTipText(label.getText());
				return label;
			}
			if (!node.hasDescString()) {
				return emptyLabel;
			}
			RSyntaxTextArea textArea = new RSyntaxTextArea();
			textArea.setFont(codeFont);
			textArea.setEditable(false);
			textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
			textArea.setText("  " + node.makeDescString());
			textArea.setRows(1);
			textArea.setColumns(textArea.getText().length());
			if (highlightText != null) {
				SearchContext searchContext = new SearchContext(highlightText);
				searchContext.setMatchCase(true);
				searchContext.setMarkAll(true);
				SearchEngine.markAll(textArea, searchContext);
			}
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
				BackgroundWorker backgroundWorker = mainWindow.getBackgroundWorker();
				if (backgroundWorker == null) {
					return null;
				}
				backgroundWorker.exec();

				DecompileJob decompileJob = cache.getDecompileJob();
				progressPane.changeLabel(this, decompileJob.getInfoString());
				decompileJob.processAndWait();

				BackgroundJob indexJob = cache.getIndexJob();
				progressPane.changeLabel(this, indexJob.getInfoString());
				indexJob.processAndWait();
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

	protected void loadStartCommon() {
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		progressPane.setIndeterminate(true);
		progressPane.setVisible(true);
		resultsTable.setEnabled(false);
		warnLabel.setVisible(false);
	}

	private void loadFinishedCommon() {
		setCursor(null);
		resultsTable.setEnabled(true);
		progressPane.setVisible(false);

		TextSearchIndex textIndex = cache.getTextIndex();
		if (textIndex == null) {
			warnLabel.setText("Index not initialized, search will be disabled!");
			warnLabel.setVisible(true);
		}
	}

	protected abstract void loadFinished();

	protected abstract void loadStart();

}
