package jadx.gui.ui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Set;

import jadx.gui.utils.NLS;
import jadx.gui.utils.TextStandardActions;
import jadx.gui.utils.search.TextSearchIndex;

public class SearchDialog extends CommonSearchDialog {

	private static final long serialVersionUID = -5105405456969134105L;

	enum SearchOptions {
		CLASS,
		METHOD,
		FIELD,
		CODE
	}

	private Set<SearchOptions> options;

	private JTextField searchField;
	private JCheckBox caseChBox;

	public SearchDialog(MainWindow mainWindow, Set<SearchOptions> options) {
		super(mainWindow);
		this.options = options;

		initUI();
		registerInitOnOpen();
		loadWindowPos();
	}

	@Override
	protected void openInit() {
		prepare();
		String lastSearch = cache.getLastSearch();
		if (lastSearch != null) {
			searchField.setText(lastSearch);
			searchField.selectAll();
		}
		searchField.requestFocus();
	}

	@Override
	protected synchronized void performSearch() {
		resultsModel.clear();
		String text = searchField.getText();
		if (text == null || text.isEmpty() || options.isEmpty()) {
			return;
		}
		try {
			cache.setLastSearch(text);
			TextSearchIndex index = cache.getTextIndex();
			if (index == null) {
				return;
			}
			boolean caseInsensitive = caseChBox.isSelected();
			if (options.contains(SearchOptions.CLASS)) {
				resultsModel.addAll(index.searchClsName(text, caseInsensitive));
			}
			if (options.contains(SearchOptions.METHOD)) {
				resultsModel.addAll(index.searchMthName(text, caseInsensitive));
			}
			if (options.contains(SearchOptions.FIELD)) {
				resultsModel.addAll(index.searchFldName(text, caseInsensitive));
			}
			if (options.contains(SearchOptions.CODE)) {
				resultsModel.addAll(index.searchCode(text, caseInsensitive));
			}
			highlightText = text;
			highlightTextCaseInsensitive = caseInsensitive;
		} finally {
			super.performSearch();
		}
	}

	private class SearchFieldListener implements DocumentListener, ActionListener {
		private Timer timer;

		private synchronized void change() {
			if (timer != null) {
				timer.restart();
			} else {
				timer = new Timer(400, this);
				timer.setRepeats(false);
				timer.start();
			}
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			performSearch();
		}

		public void changedUpdate(DocumentEvent e) {
			change();
		}

		public void removeUpdate(DocumentEvent e) {
			change();
		}

		public void insertUpdate(DocumentEvent e) {
			change();
		}
	}

	private void initUI() {
		JLabel findLabel = new JLabel(NLS.str("search_dialog.open_by_name"));
		searchField = new JTextField();
		searchField.setAlignmentX(LEFT_ALIGNMENT);
		searchField.getDocument().addDocumentListener(new SearchFieldListener());
		new TextStandardActions(searchField);

		caseChBox = new JCheckBox(NLS.str("search_dialog.ignorecase"));
		caseChBox.addItemListener(e -> performSearch());

		JCheckBox clsChBox = makeOptionsCheckBox(NLS.str("search_dialog.class"), SearchOptions.CLASS);
		JCheckBox mthChBox = makeOptionsCheckBox(NLS.str("search_dialog.method"), SearchOptions.METHOD);
		JCheckBox fldChBox = makeOptionsCheckBox(NLS.str("search_dialog.field"), SearchOptions.FIELD);
		JCheckBox codeChBox = makeOptionsCheckBox(NLS.str("search_dialog.code"), SearchOptions.CODE);

		JPanel searchInPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		searchInPanel.setBorder(BorderFactory.createTitledBorder(NLS.str("search_dialog.search_in")));
		searchInPanel.add(clsChBox);
		searchInPanel.add(mthChBox);
		searchInPanel.add(fldChBox);
		searchInPanel.add(codeChBox);

		JPanel searchOptions = new JPanel(new FlowLayout(FlowLayout.LEFT));
		searchOptions.setBorder(BorderFactory.createTitledBorder(NLS.str("search_dialog.options")));
		searchOptions.add(caseChBox);

		Box box = Box.createHorizontalBox();
		box.setAlignmentX(LEFT_ALIGNMENT);
		box.add(searchInPanel);
		box.add(searchOptions);

		JPanel searchPane = new JPanel();
		searchPane.setLayout(new BoxLayout(searchPane, BoxLayout.PAGE_AXIS));
		findLabel.setLabelFor(searchField);
		searchPane.add(findLabel);
		searchPane.add(Box.createRigidArea(new Dimension(0, 5)));
		searchPane.add(searchField);
		searchPane.add(Box.createRigidArea(new Dimension(0, 5)));
		searchPane.add(box);
		searchPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		initCommon();
		JPanel resultsPanel = initResultsTable();
		JPanel buttonPane = initButtonsPanel();

		Container contentPane = getContentPane();
		contentPane.add(searchPane, BorderLayout.PAGE_START);
		contentPane.add(resultsPanel, BorderLayout.CENTER);
		contentPane.add(buttonPane, BorderLayout.PAGE_END);

		searchField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					if (resultsModel.getRowCount() != 0) {
						resultsTable.setRowSelectionInterval(0, 0);
					}
					resultsTable.requestFocus();
				}
			}
		});

		setTitle(NLS.str("menu.text_search"));
		pack();
		setSize(800, 500);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setModalityType(ModalityType.MODELESS);
	}

	private JCheckBox makeOptionsCheckBox(String name, final SearchOptions opt) {
		final JCheckBox chBox = new JCheckBox(name);
		chBox.setAlignmentX(LEFT_ALIGNMENT);
		chBox.setSelected(options.contains(opt));
		chBox.addItemListener(e -> {
			if (chBox.isSelected()) {
				options.add(opt);
			} else {
				options.remove(opt);
			}
			performSearch();
		});
		return chBox;
	}

	@Override
	protected void loadFinished() {
		searchField.setEnabled(true);
	}

	@Override
	protected void loadStart() {
		searchField.setEnabled(false);
	}
}
