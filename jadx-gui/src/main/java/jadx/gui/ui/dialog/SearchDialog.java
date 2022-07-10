package jadx.gui.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hu.akarnokd.rxjava2.swing.SwingSchedulers;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;

import jadx.api.JavaClass;
import jadx.core.utils.ListUtils;
import jadx.gui.jobs.ITaskProgress;
import jadx.gui.search.SearchSettings;
import jadx.gui.search.SearchTask;
import jadx.gui.search.providers.ClassSearchProvider;
import jadx.gui.search.providers.CodeSearchProvider;
import jadx.gui.search.providers.CommentSearchProvider;
import jadx.gui.search.providers.FieldSearchProvider;
import jadx.gui.search.providers.MergedSearchProvider;
import jadx.gui.search.providers.MethodSearchProvider;
import jadx.gui.search.providers.ResourceSearchProvider;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.JumpPosition;
import jadx.gui.utils.NLS;
import jadx.gui.utils.TextStandardActions;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.layout.WrapLayout;

import static jadx.gui.ui.dialog.SearchDialog.SearchOptions.ACTIVE_TAB;
import static jadx.gui.ui.dialog.SearchDialog.SearchOptions.CLASS;
import static jadx.gui.ui.dialog.SearchDialog.SearchOptions.CODE;
import static jadx.gui.ui.dialog.SearchDialog.SearchOptions.COMMENT;
import static jadx.gui.ui.dialog.SearchDialog.SearchOptions.FIELD;
import static jadx.gui.ui.dialog.SearchDialog.SearchOptions.IGNORE_CASE;
import static jadx.gui.ui.dialog.SearchDialog.SearchOptions.METHOD;
import static jadx.gui.ui.dialog.SearchDialog.SearchOptions.RESOURCE;
import static jadx.gui.ui.dialog.SearchDialog.SearchOptions.USE_REGEX;

public class SearchDialog extends CommonSearchDialog {
	private static final Logger LOG = LoggerFactory.getLogger(SearchDialog.class);
	private static final long serialVersionUID = -5105405456969134105L;

	private static final Color SEARCH_FIELD_ERROR_COLOR = new Color(255, 150, 150);

	public static void search(MainWindow window, SearchPreset preset) {
		SearchDialog searchDialog = new SearchDialog(window, preset, Collections.emptySet());
		searchDialog.setVisible(true);
	}

	public static void searchInActiveTab(MainWindow window, SearchPreset preset) {
		SearchDialog searchDialog = new SearchDialog(window, preset, EnumSet.of(SearchOptions.ACTIVE_TAB));
		searchDialog.setVisible(true);
	}

	public static void searchText(MainWindow window, String text) {
		SearchDialog searchDialog = new SearchDialog(window, SearchPreset.TEXT, Collections.emptySet());
		searchDialog.initSearchText = text;
		searchDialog.setVisible(true);
	}

	public enum SearchPreset {
		TEXT, CLASS, COMMENT
	}

	public enum SearchOptions {
		CLASS,
		METHOD,
		FIELD,
		CODE,
		RESOURCE,
		COMMENT,

		IGNORE_CASE,
		USE_REGEX,
		ACTIVE_TAB
	}

	private final transient SearchPreset searchPreset;
	private final transient Set<SearchOptions> options;

	private transient Color searchFieldDefaultBgColor;

	private transient JTextField searchField;

	private transient @Nullable SearchTask searchTask;
	private transient JButton loadAllButton;
	private transient JButton loadMoreButton;

	private transient Disposable searchDisposable;
	private transient SearchEventEmitter searchEmitter;
	private transient ChangeListener activeTabListener;

	private transient String initSearchText = null;

	// temporal list for pending results
	private final List<JNode> pendingResults = new ArrayList<>();

	private SearchDialog(MainWindow mainWindow, SearchPreset preset, Set<SearchOptions> additionalOptions) {
		super(mainWindow, NLS.str("menu.text_search"));
		this.searchPreset = preset;
		this.options = buildOptions(preset);
		this.options.addAll(additionalOptions);

		loadWindowPos();
		initUI();
		searchFieldSubscribe();
		registerInitOnOpen();
		registerActiveTabListener();
	}

	@Override
	public void dispose() {
		if (searchDisposable != null && !searchDisposable.isDisposed()) {
			searchDisposable.dispose();
		}
		resultsModel.clear();
		removeActiveTabListener();
		if (searchTask != null) {
			searchTask.cancel();
			mainWindow.getBackgroundExecutor().execute(NLS.str("progress.load"), () -> {
				stopSearchTask();
				unloadTempData();
			});
		}
		super.dispose();
	}

	private Set<SearchOptions> buildOptions(SearchPreset preset) {
		Set<SearchOptions> searchOptions = cache.getLastSearchOptions().get(preset);
		if (searchOptions == null) {
			searchOptions = EnumSet.noneOf(SearchOptions.class);
		}
		switch (preset) {
			case TEXT:
				if (searchOptions.isEmpty()) {
					searchOptions.add(SearchOptions.CODE);
					searchOptions.add(SearchOptions.IGNORE_CASE);
				}
				break;

			case CLASS:
				searchOptions.add(SearchOptions.CLASS);
				break;

			case COMMENT:
				searchOptions.add(SearchOptions.COMMENT);
				searchOptions.remove(SearchOptions.ACTIVE_TAB);
				break;
		}
		return searchOptions;
	}

	@Override
	protected void openInit() {
		String searchText = initSearchText != null ? initSearchText : cache.getLastSearch();
		if (searchText != null) {
			searchField.setText(searchText);
			searchField.selectAll();
		}
		searchField.requestFocus();
		resultsTable.initColumnWidth();

		if (options.contains(COMMENT)) {
			// show all comments on empty input
			searchEmitter.emitSearch();
		}
	}

	private void initUI() {
		searchField = new JTextField();
		searchFieldDefaultBgColor = searchField.getBackground();
		searchField.setAlignmentX(LEFT_ALIGNMENT);
		TextStandardActions.attach(searchField);

		JLabel findLabel = new JLabel(NLS.str("search_dialog.open_by_name"));
		findLabel.setAlignmentX(LEFT_ALIGNMENT);

		JPanel searchFieldPanel = new JPanel();
		searchFieldPanel.setLayout(new BoxLayout(searchFieldPanel, BoxLayout.PAGE_AXIS));
		searchFieldPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
		searchFieldPanel.setAlignmentX(LEFT_ALIGNMENT);
		searchFieldPanel.add(findLabel);
		searchFieldPanel.add(Box.createRigidArea(new Dimension(0, 5)));
		searchFieldPanel.add(searchField);

		JPanel searchInPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		searchInPanel.setBorder(BorderFactory.createTitledBorder(NLS.str("search_dialog.search_in")));
		searchInPanel.add(makeOptionsCheckBox(NLS.str("search_dialog.class"), SearchOptions.CLASS));
		searchInPanel.add(makeOptionsCheckBox(NLS.str("search_dialog.method"), SearchOptions.METHOD));
		searchInPanel.add(makeOptionsCheckBox(NLS.str("search_dialog.field"), SearchOptions.FIELD));
		searchInPanel.add(makeOptionsCheckBox(NLS.str("search_dialog.code"), SearchOptions.CODE));
		searchInPanel.add(makeOptionsCheckBox(NLS.str("search_dialog.resource"), SearchOptions.RESOURCE));
		searchInPanel.add(makeOptionsCheckBox(NLS.str("search_dialog.comments"), SearchOptions.COMMENT));

		JPanel searchOptions = new JPanel(new FlowLayout(FlowLayout.LEFT));
		searchOptions.setBorder(BorderFactory.createTitledBorder(NLS.str("search_dialog.options")));
		searchOptions.add(makeOptionsCheckBox(NLS.str("search_dialog.ignorecase"), SearchOptions.IGNORE_CASE));
		searchOptions.add(makeOptionsCheckBox(NLS.str("search_dialog.regex"), SearchOptions.USE_REGEX));
		searchOptions.add(makeOptionsCheckBox(NLS.str("search_dialog.active_tab"), SearchOptions.ACTIVE_TAB));

		JPanel optionsPanel = new JPanel(new WrapLayout(WrapLayout.LEFT));
		optionsPanel.setAlignmentX(LEFT_ALIGNMENT);
		optionsPanel.add(searchInPanel);
		optionsPanel.add(searchOptions);

		JPanel searchPane = new JPanel();
		searchPane.setLayout(new BoxLayout(searchPane, BoxLayout.PAGE_AXIS));
		searchPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		searchPane.add(searchFieldPanel);
		searchPane.add(Box.createRigidArea(new Dimension(0, 5)));
		searchPane.add(optionsPanel);

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
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}

	protected void addCustomResultsActions(JPanel resultsActionsPanel) {
		loadAllButton = new JButton(NLS.str("search_dialog.load_all"));
		loadAllButton.addActionListener(e -> loadAll());
		loadAllButton.setEnabled(false);

		loadMoreButton = new JButton(NLS.str("search_dialog.load_more"));
		loadMoreButton.addActionListener(e -> loadMore());
		loadMoreButton.setEnabled(false);

		resultsActionsPanel.add(loadAllButton);
		resultsActionsPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		resultsActionsPanel.add(loadMoreButton);
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
				.debounce(500, TimeUnit.MILLISECONDS)
				.observeOn(SwingSchedulers.edt())
				.subscribe(this::search);
	}

	@Nullable
	private synchronized void search(String text) {
		UiUtils.uiThreadGuard();
		resetSearch();
		if (text == null || options.isEmpty()) {
			return;
		}
		// allow empty text for comments search
		if (text.isEmpty() && !options.contains(SearchOptions.COMMENT)) {
			return;
		}
		LOG.debug("Building search for '{}', options: {}", text, options);
		boolean ignoreCase = options.contains(IGNORE_CASE);
		boolean useRegex = options.contains(USE_REGEX);
		SearchSettings searchSettings = new SearchSettings(text, ignoreCase, useRegex);
		String error = searchSettings.prepare();
		if (error == null) {
			if (Objects.equals(searchField.getBackground(), SEARCH_FIELD_ERROR_COLOR)) {
				searchField.setBackground(searchFieldDefaultBgColor);
			}
		} else {
			searchField.setBackground(SEARCH_FIELD_ERROR_COLOR);
			resultsInfoLabel.setText(error);
			return;
		}
		searchTask = new SearchTask(mainWindow, this::addSearchResult, s -> searchComplete());
		if (!buildSearch(text, searchSettings)) {
			return;
		}

		updateTableHighlight();
		startSearch();
		searchTask.setResultsLimit(100);
		searchTask.setProgressListener(this::updateProgress);
		searchTask.fetchResults();
		LOG.debug("Total search items count estimation: {}", searchTask.getTaskProgress().total());
	}

	private boolean buildSearch(String text, SearchSettings searchSettings) {
		Objects.requireNonNull(searchTask);

		List<JavaClass> allClasses;
		if (options.contains(ACTIVE_TAB)) {
			JumpPosition currentPos = mainWindow.getTabbedPane().getCurrentPosition();
			if (currentPos == null) {
				resultsInfoLabel.setText("Can't search in current tab");
				return false;
			}
			JClass activeCls = currentPos.getNode().getRootClass();
			searchSettings.setActiveCls(activeCls);
			allClasses = Collections.singletonList(activeCls.getCls());
		} else {
			allClasses = mainWindow.getWrapper().getIncludedClassesWithInners();
		}
		// allow empty text for comments search
		if (text.isEmpty() && options.contains(SearchOptions.COMMENT)) {
			searchTask.addProviderJob(new CommentSearchProvider(mainWindow, searchSettings));
			return true;
		}
		// using ordered execution for fast tasks
		MergedSearchProvider merged = new MergedSearchProvider();
		if (options.contains(CLASS)) {
			merged.add(new ClassSearchProvider(mainWindow, searchSettings, allClasses));
		}
		if (options.contains(METHOD)) {
			merged.add(new MethodSearchProvider(mainWindow, searchSettings, allClasses));
		}
		if (options.contains(FIELD)) {
			merged.add(new FieldSearchProvider(mainWindow, searchSettings, allClasses));
		}
		if (options.contains(CODE)) {
			if (allClasses.size() == 1) {
				searchTask.addProviderJob(new CodeSearchProvider(mainWindow, searchSettings, allClasses));
			} else {
				List<JavaClass> topClasses = ListUtils.filter(allClasses, c -> !c.isInner());
				for (List<JavaClass> batch : mainWindow.getWrapper().buildDecompileBatches(topClasses)) {
					searchTask.addProviderJob(new CodeSearchProvider(mainWindow, searchSettings, batch));
				}
			}
		}
		if (options.contains(RESOURCE)) {
			searchTask.addProviderJob(new ResourceSearchProvider(mainWindow, searchSettings));
		}
		if (options.contains(COMMENT)) {
			searchTask.addProviderJob(new CommentSearchProvider(mainWindow, searchSettings));
		}
		merged.prepare();
		searchTask.addProviderJob(merged);
		return true;
	}

	private synchronized void stopSearchTask() {
		if (searchTask != null) {
			searchTask.cancel();
			searchTask = null;
		}
	}

	private synchronized void loadMore() {
		if (searchTask == null) {
			return;
		}
		startSearch();
		searchTask.fetchResults();
	}

	private synchronized void loadAll() {
		if (searchTask == null) {
			return;
		}
		startSearch();
		searchTask.setResultsLimit(0);
		searchTask.fetchResults();
	}

	private synchronized void resetSearch() {
		resultsModel.clear();
		updateTable();
		progressPane.setVisible(false);
		warnLabel.setVisible(false);
		loadAllButton.setEnabled(false);
		loadMoreButton.setEnabled(false);
		stopSearchTask();
	}

	private void startSearch() {
		showSearchState();
		progressStartCommon();
	}

	private void addSearchResult(JNode node) {
		synchronized (pendingResults) {
			pendingResults.add(node);
		}
	}

	private void updateTable() {
		synchronized (pendingResults) {
			Collections.sort(pendingResults);
			resultsModel.addAll(pendingResults);
			pendingResults.clear();
		}
		resultsTable.updateTable();
	}

	private void updateTableHighlight() {
		String text = searchField.getText();
		setHighlightText(text);
		highlightTextCaseInsensitive = options.contains(SearchOptions.IGNORE_CASE);
		highlightTextUseRegex = options.contains(SearchOptions.USE_REGEX);
		cache.setLastSearch(text);
		cache.getLastSearchOptions().put(searchPreset, options);
	}

	private void updateProgress(ITaskProgress progress) {
		UiUtils.uiRun(() -> {
			progressPane.setProgress(progress);
			updateTable();
		});
	}

	private synchronized void searchComplete() {
		UiUtils.uiThreadGuard();
		LOG.debug("Search complete");
		updateTable();

		boolean complete = searchTask == null || searchTask.isSearchComplete();
		loadAllButton.setEnabled(!complete);
		loadMoreButton.setEnabled(!complete);
		updateProgressLabel(complete);
		unloadTempData();
		progressFinishedCommon();
	}

	private void unloadTempData() {
		mainWindow.getWrapper().unloadClasses();
		System.gc();
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
				.distinctUntilChanged();
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
		resultsTable.setEnabled(true);
		searchField.setEnabled(true);
		searchEmitter.emitSearch();
	}

	@Override
	protected void loadStart() {
		resultsTable.setEnabled(false);
		searchField.setEnabled(false);
	}

	private void registerActiveTabListener() {
		removeActiveTabListener();
		activeTabListener = e -> {
			if (options.contains(SearchOptions.ACTIVE_TAB)) {
				LOG.debug("active tab change event received");
				searchEmitter.emitSearch();
			}
		};
		mainWindow.getTabbedPane().addChangeListener(activeTabListener);
	}

	private void removeActiveTabListener() {
		if (activeTabListener != null) {
			mainWindow.getTabbedPane().removeChangeListener(activeTabListener);
			activeTabListener = null;
		}
	}
}
