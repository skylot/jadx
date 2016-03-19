package jadx.gui.ui;

import jadx.gui.utils.NLS;
import jadx.gui.utils.TextStandardActions;
import jadx.gui.utils.search.TextSearchIndex;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.EnumSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchDialog extends CommonSearchDialog {

	private static final long serialVersionUID = -5105405456969134105L;

	private static final Logger LOG = LoggerFactory.getLogger(SearchDialog.class);

	enum SearchOptions {
		CLASS,
		METHOD,
		FIELD,
		CODE
	}

	private Set<SearchOptions> options = EnumSet.allOf(SearchOptions.class);

	private JTextField searchField;

	public SearchDialog(MainWindow mainWindow, Set<SearchOptions> options) {
		super(mainWindow);
		this.options = options;

		initUI();
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(WindowEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						openInit();
					}
				});
			}
		});
		loadWindowPos();
	}

	protected void openInit() {
		prepare();
		String lastSearch = cache.getLastSearch();
		if (lastSearch != null) {
			searchField.setText(lastSearch);
			searchField.selectAll();
		}
		searchField.requestFocus();
	}

	private synchronized void performSearch() {
		resultsModel.clear();
		String text = searchField.getText();
		if (text == null || text.isEmpty() || options.isEmpty()) {
			resultsTable.updateTable();
			return;
		}
		cache.setLastSearch(text);
		TextSearchIndex index = cache.getTextIndex();
		if (index == null) {
			resultsTable.updateTable();
			return;
		}
		if (options.contains(SearchOptions.CLASS)) {
			resultsModel.addAll(index.searchClsName(text));
		}
		if (options.contains(SearchOptions.METHOD)) {
			resultsModel.addAll(index.searchMthName(text));
		}
		if (options.contains(SearchOptions.FIELD)) {
			resultsModel.addAll(index.searchFldName(text));
		}
		if (options.contains(SearchOptions.CODE)) {
			resultsModel.addAll(index.searchCode(text));
		}
		highlightText = text;
		resultsTable.updateTable();
	}

	private class SearchFieldListener implements DocumentListener, ActionListener {

		private Timer timer;

		private synchronized void change() {
			if (timer != null) {
				timer.restart();
			} else {
				timer = new Timer(300, this);
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

		JCheckBox clsChBox = makeOptionsCheckBox(NLS.str("search_dialog.class"), SearchOptions.CLASS);
		JCheckBox mthChBox = makeOptionsCheckBox(NLS.str("search_dialog.method"), SearchOptions.METHOD);
		JCheckBox fldChBox = makeOptionsCheckBox(NLS.str("search_dialog.field"), SearchOptions.FIELD);
		JCheckBox codeChBox = makeOptionsCheckBox(NLS.str("search_dialog.code"), SearchOptions.CODE);

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
		chBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (chBox.isSelected()) {
					options.add(opt);
				} else {
					options.remove(opt);
				}
				performSearch();
			}
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
