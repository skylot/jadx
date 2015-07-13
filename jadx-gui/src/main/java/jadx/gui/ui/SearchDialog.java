package jadx.gui.ui;

import jadx.api.JavaClass;
import jadx.gui.JadxWrapper;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.TextNode;
import jadx.gui.utils.CacheObject;
import jadx.gui.utils.NLS;
import jadx.gui.utils.Position;
import jadx.gui.utils.TextSearchIndex;
import jadx.gui.utils.TextStandardActions;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.EnumSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchDialog extends JDialog {

	private static final long serialVersionUID = -5105405456969134105L;

	private static final Logger LOG = LoggerFactory.getLogger(SearchDialog.class);
	private static final int MAX_RESULTS_COUNT = 500;

	private enum SearchOptions {
		CLASS,
		METHOD,
		FIELD,
		CODE
	}

	private static final Set<SearchOptions> OPTIONS = EnumSet.allOf(SearchOptions.class);

	private final TabbedPane tabbedPane;
	private final JadxWrapper wrapper;
	private final CacheObject cache;

	private JTextField searchField;
	private ResultsModel resultsModel;
	private JList resultsList;
	private JProgressBar busyBar;

	public SearchDialog(MainWindow mainWindow, TabbedPane tabbedPane, JadxWrapper wrapper) {
		super(mainWindow);
		this.tabbedPane = tabbedPane;
		this.wrapper = wrapper;
		this.cache = mainWindow.getCacheObject();

		initUI();
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowActivated(WindowEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						prepare();
					}
				});
			}
		});
	}

	public void prepare() {
		TextSearchIndex index = cache.getTextIndex();
		if (index != null) {
			return;
		}
		LoadTask task = new LoadTask();
		task.execute();
	}

	private void loadData() {
		TextSearchIndex index = cache.getTextIndex();
		if (index != null) {
			return;
		}
		index = new TextSearchIndex();
		for (JavaClass cls : wrapper.getClasses()) {
			index.indexNames(cls);
		}
		for (JavaClass cls : wrapper.getClasses()) {
			index.indexCode(cls);
		}
		cache.setTextIndex(index);
	}

	private synchronized void performSearch() {
		resultsModel.removeAllElements();
		String text = searchField.getText();
		if (text == null || text.isEmpty() || OPTIONS.isEmpty()) {
			return;
		}
		TextSearchIndex index = cache.getTextIndex();
		if (index == null) {
			return;
		}
		if (OPTIONS.contains(SearchOptions.CLASS)) {
			resultsModel.addAll(index.searchClsName(text));
		}
		if (OPTIONS.contains(SearchOptions.METHOD)) {
			resultsModel.addAll(index.searchMthName(text));
		}
		if (OPTIONS.contains(SearchOptions.FIELD)) {
			resultsModel.addAll(index.searchFldName(text));
		}
		if (OPTIONS.contains(SearchOptions.CODE)) {
			resultsModel.addAll(index.searchCode(text));
		}
		LOG.info("Search returned {} results", resultsModel.size());
	}

	private void openSelectedItem() {
		int selectedId = resultsList.getSelectedIndex();
		if (selectedId == -1) {
			return;
		}
		JNode node = (JNode) resultsModel.get(selectedId);
		tabbedPane.showCode(new Position(node.getRootClass(), node.getLine()));

		dispose();
	}

	private class LoadTask extends SwingWorker<Void, Void> {
		public LoadTask() {
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			busyBar.setVisible(true);
			searchField.setEnabled(false);
			resultsList.setEnabled(false);
		}

		@Override
		public Void doInBackground() {
			loadData();
			return null;
		}

		@Override
		public void done() {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					setCursor(null);
					searchField.setEnabled(true);
					resultsList.setEnabled(true);
					busyBar.setVisible(false);
				}
			});
		}
	}

	private static class ResultsModel extends DefaultListModel {
		private static final long serialVersionUID = -7821286846923903208L;

		private void addAll(Iterable<? extends JNode> nodes) {
			for (JNode node : nodes) {
				if (size() >= MAX_RESULTS_COUNT) {
					if (size() == MAX_RESULTS_COUNT) {
						addElement(new TextNode("Search results truncated (limit: " + MAX_RESULTS_COUNT + ")"));
					}
					return;
				}
				addElement(node);
			}
		}
	}

	private static class ResultsCellRenderer implements ListCellRenderer {
		private final Color selectedBackground;
		private final Color selectedForeground;

		ResultsCellRenderer() {
			UIDefaults defaults = UIManager.getDefaults();
			selectedBackground = defaults.getColor("List.selectionBackground");
			selectedForeground = defaults.getColor("List.selectionForeground");
		}

		@Override
		public Component getListCellRendererComponent(JList list,
				Object obj, int index, boolean isSelected, boolean cellHasFocus) {
			if (!(obj instanceof JNode)) {
				return null;
			}
			JNode value = (JNode) obj;
			JLabel label = new JLabel();
			label.setOpaque(true);
			label.setIcon(value.getIcon());
			label.setText(value.makeLongString());
			if (isSelected) {
				label.setBackground(selectedBackground);
				label.setForeground(selectedForeground);
			}
			return label;
		}
	}

	private class SearchFieldListener implements DocumentListener {

		public void changedUpdate(DocumentEvent e) {
			performSearch();
		}

		public void removeUpdate(DocumentEvent e) {
			performSearch();
		}

		public void insertUpdate(DocumentEvent e) {
			performSearch();
		}
	}

	private void initUI() {
		JLabel findLabel = new JLabel(NLS.str("search_dialog.open_by_name"));

		searchField = new JTextField();
		searchField.setAlignmentX(LEFT_ALIGNMENT);
		searchField.getDocument().addDocumentListener(new SearchFieldListener());
		new TextStandardActions(searchField);

		JCheckBox clsChBox = makeOptionsCheckBox(NLS.str("search_dialog.class"), SearchOptions.CLASS);
		JCheckBox mthChBox = makeOptionsCheckBox(NLS.str("search_dialog.method"), SearchOptions.METHOD);
		JCheckBox fldChBox = makeOptionsCheckBox(NLS.str("search_dialog.field"), SearchOptions.FIELD);
		JCheckBox codeChBox = makeOptionsCheckBox(NLS.str("search_dialog.code"), SearchOptions.CODE);

		resultsModel = new ResultsModel();
		resultsList = new JList(resultsModel);
		resultsList.setCellRenderer(new ResultsCellRenderer());
		resultsList.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() == 2) {
					openSelectedItem();
				}
			}
		});

		JPanel searchOptions = new JPanel(new FlowLayout(FlowLayout.LEFT));
		searchOptions.setBorder(BorderFactory.createTitledBorder(NLS.str("search_dialog.search_in")));
		searchOptions.add(clsChBox);
		searchOptions.add(mthChBox);
		searchOptions.add(fldChBox);
		searchOptions.add(codeChBox);
		searchOptions.setAlignmentX(LEFT_ALIGNMENT);

		JPanel searchPane = new JPanel();
		searchPane.setLayout(new BoxLayout(searchPane, BoxLayout.PAGE_AXIS));
		findLabel.setLabelFor(searchField);
		searchPane.add(findLabel);
		searchPane.add(Box.createRigidArea(new Dimension(0, 5)));
		searchPane.add(searchField);
		searchPane.add(Box.createRigidArea(new Dimension(0, 5)));
		searchPane.add(searchOptions);
		searchPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JPanel listPane = new JPanel();
		listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));
		listPane.add(new JScrollPane(resultsList));
		listPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

		busyBar = new JProgressBar();
		busyBar.setIndeterminate(true);
		busyBar.setVisible(false);

		//Create and initialize the buttons.
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

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		buttonPane.add(busyBar);
		buttonPane.add(Box.createRigidArea(new Dimension(5, 0)));
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(openBtn);
		buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPane.add(cancelButton);

		final Container contentPane = getContentPane();
		contentPane.add(searchPane, BorderLayout.PAGE_START);
		contentPane.add(listPane, BorderLayout.CENTER);
		contentPane.add(buttonPane, BorderLayout.PAGE_END);
		getRootPane().setDefaultButton(openBtn);

		searchField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					resultsList.requestFocus();
					if (!resultsModel.isEmpty()) {
						resultsList.setSelectedIndex(0);
					}
				}
			}
		});

		setTitle(NLS.str("menu.search"));
		pack();
		setSize(700, 500);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setModalityType(ModalityType.MODELESS);
	}

	private JCheckBox makeOptionsCheckBox(String name, final SearchOptions opt) {
		final JCheckBox chBox = new JCheckBox(name);
		chBox.setAlignmentX(LEFT_ALIGNMENT);
		chBox.setSelected(OPTIONS.contains(opt));
		chBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (chBox.isSelected()) {
					OPTIONS.add(opt);
				} else {
					OPTIONS.remove(opt);
				}
				performSearch();
			}
		});
		return chBox;
	}
}
