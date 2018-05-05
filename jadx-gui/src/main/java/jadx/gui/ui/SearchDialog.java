package jadx.gui.ui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import hu.akarnokd.rxjava2.swing.SwingSchedulers;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.treemodel.JNode;
import jadx.gui.utils.NLS;
import jadx.gui.utils.TextStandardActions;
import jadx.gui.utils.search.TextSearchIndex;

public class SearchDialog extends CommonSearchDialog {

	private static final Logger LOG = LoggerFactory.getLogger(SearchDialog.class);
	private static final long serialVersionUID = -5105405456969134105L;
	private final boolean textSearch;

	public enum SearchOptions {
		CLASS,
		METHOD,
		FIELD,
		CODE,
		IGNORE_CASE
	}

	private transient Set<SearchOptions> options;

	private transient JTextField searchField;

	private transient Disposable searchDisposable;
	private transient SearchEventEmitter searchEmitter;

	public SearchDialog(MainWindow mainWindow, boolean textSearch) {
		super(mainWindow);
		this.textSearch = textSearch;
		if (textSearch) {
			Set<SearchOptions> lastSearchOptions = cache.getLastSearchOptions();
			if (!lastSearchOptions.isEmpty()) {
				this.options = lastSearchOptions;
			} else {
				this.options = EnumSet.of(SearchOptions.CODE, SearchOptions.IGNORE_CASE);
			}
		} else {
			this.options = EnumSet.of(SearchOptions.CLASS);
		}

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
			searchEmitter.emitSearch();
		}
		searchField.requestFocus();
	}

	private void initUI() {
		JLabel findLabel = new JLabel(NLS.str("search_dialog.open_by_name"));
		searchField = new JTextField();
		searchField.setAlignmentX(LEFT_ALIGNMENT);
		new TextStandardActions(searchField);
		searchFieldSubscribe();

		JCheckBox caseChBox = makeOptionsCheckBox(NLS.str("search_dialog.ignorecase"), SearchOptions.IGNORE_CASE);

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

	private class SearchEventEmitter {
		private final Flowable<String> flowable;
		private Emitter<String> emitter;

		public SearchEventEmitter() {
			flowable = Flowable.create(this::saveEmitter, BackpressureStrategy.LATEST);
		}

		public Flowable<String> getFlowable() {
			return flowable;
		}

		private void saveEmitter(Emitter<String> emitter) {
			this.emitter = emitter;
		}

		public synchronized void emitSearch() {
			this.emitter.onNext(searchField.getText());
		}
	}

	private void searchFieldSubscribe() {
		searchEmitter = new SearchEventEmitter();

		Flowable<String> textChanges = onTextFieldChanges(searchField);
		Flowable<String> searchEvents = Flowable.merge(textChanges, searchEmitter.getFlowable());
		searchDisposable = searchEvents
				.filter(text -> text.length() > 0)
				.subscribeOn(Schedulers.single())
				.doOnNext(r -> LOG.debug("search event: {}", r))
				.switchMap(text -> prepareSearch(text)
						.subscribeOn(Schedulers.single())
						.toList()
						.toFlowable(), 1)
				.observeOn(SwingSchedulers.edt())
				.subscribe(this::processSearchResults);
	}

	private Flowable<JNode> prepareSearch(String text) {
		if (text == null || text.isEmpty() || options.isEmpty()) {
			return Flowable.empty();
		}
		TextSearchIndex index = cache.getTextIndex();
		if (index == null) {
			return Flowable.empty();
		}
		return index.buildSearch(text, options);
	}

	private void processSearchResults(java.util.List<JNode> results) {
		LOG.debug("search result size: {}", results.size());
		String text = searchField.getText();
		highlightText = text;
		highlightTextCaseInsensitive = options.contains(SearchOptions.IGNORE_CASE);

		cache.setLastSearch(text);
		if (textSearch) {
			cache.setLastSearchOptions(options);
		}

		resultsModel.clear();
		resultsModel.addAll(results);
		super.performSearch();
	}

	private static Flowable<String> onTextFieldChanges(final JTextField textField) {
		return Flowable.<String>create(emitter -> {
			DocumentListener listener = new DocumentListener() {
				@Override
				public void insertUpdate(DocumentEvent e) {
					change();
				}

				@Override
				public void removeUpdate(DocumentEvent e) {
					change();
				}

				@Override
				public void changedUpdate(DocumentEvent e) {
					change();
				}

				public void change() {
					emitter.onNext(textField.getText());
				}
			};

			textField.getDocument().addDocumentListener(listener);
			emitter.setDisposable(new Disposable() {
				private boolean disposed = false;

				@Override
				public void dispose() {
					textField.getDocument().removeDocumentListener(listener);
					disposed = true;
				}

				@Override
				public boolean isDisposed() {
					return disposed;
				}
			});
		}, BackpressureStrategy.LATEST)
				.debounce(300, TimeUnit.MILLISECONDS)
				.distinctUntilChanged();
	}

	@Override
	public void dispose() {
		if (searchDisposable != null && !searchDisposable.isDisposed()) {
			searchDisposable.dispose();
		}
		super.dispose();
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
			searchEmitter.emitSearch();
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
