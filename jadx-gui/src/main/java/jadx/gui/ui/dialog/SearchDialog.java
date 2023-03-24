package jadx.gui.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeListener;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.icons.FlatSearchWithHistoryIcon;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import jadx.api.JavaClass;
import jadx.core.utils.ListUtils;
import jadx.gui.jobs.ITaskInfo;
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
import jadx.gui.treemodel.JResource;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.JumpPosition;
import jadx.gui.utils.NLS;
import jadx.gui.utils.TextStandardActions;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.layout.WrapLayout;
import jadx.gui.utils.rx.RxUtils;

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
		show(searchDialog, window);
	}

	public static void searchInActiveTab(MainWindow window, SearchPreset preset) {
		SearchDialog searchDialog = new SearchDialog(window, preset, EnumSet.of(SearchOptions.ACTIVE_TAB));
		show(searchDialog, window);
	}

	public static void searchText(MainWindow window, String text) {
		SearchDialog searchDialog = new SearchDialog(window, SearchPreset.TEXT, Collections.emptySet());
		searchDialog.initSearchText = text;
		show(searchDialog, window);
	}

	private static void show(SearchDialog searchDialog, MainWindow mw) {
		mw.addLoadListener(loaded -> {
			if (!loaded) {
				searchDialog.dispose();
				return true;
			}
			return false;
		});
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
	private transient JButton stopBtn;
	private transient JButton sortBtn;

	private transient Disposable searchDisposable;
	private transient SearchEventEmitter searchEmitter;
	private transient ChangeListener activeTabListener;

	private transient String initSearchText = null;

	// temporal list for pending results
	private final List<JNode> pendingResults = new ArrayList<>();

	/**
	 * Use single thread to do all background work, so additional synchronisation not needed
	 */
	private final Executor searchBackgroundExecutor = Executors.newSingleThreadExecutor();

	private SearchDialog(MainWindow mainWindow, SearchPreset preset, Set<SearchOptions> additionalOptions) {
		super(mainWindow, NLS.str("menu.text_search"));
		this.searchPreset = preset;
		this.options = buildOptions(preset);
		this.options.addAll(additionalOptions);

		loadWindowPos();
		initUI();
		initSearchEvents();
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
		searchBackgroundExecutor.execute(() -> {
			stopSearchTask();
			unloadTempData();
		});
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
					searchOptions.add(IGNORE_CASE);
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
		addSearchHistoryButton();
		searchField.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);

		boolean autoSearch = mainWindow.getSettings().isUseAutoSearch();
		JButton searchBtn = new JButton(NLS.str("search_dialog.search_button"));
		searchBtn.setVisible(!autoSearch);
		searchBtn.addActionListener(ev -> searchEmitter.emitSearch());

		JCheckBox autoSearchCB = new JCheckBox(NLS.str("search_dialog.auto_search"));
		autoSearchCB.setSelected(autoSearch);
		autoSearchCB.addActionListener(ev -> {
			boolean newValue = autoSearchCB.isSelected();
			mainWindow.getSettings().setUseAutoSearch(newValue);
			searchBtn.setVisible(!newValue);
			initSearchEvents();
			if (newValue) {
				searchEmitter.emitSearch();
			}
		});

		JPanel searchLinePanel = new JPanel();
		searchLinePanel.setLayout(new BoxLayout(searchLinePanel, BoxLayout.LINE_AXIS));
		searchLinePanel.add(searchField);
		searchLinePanel.add(Box.createRigidArea(new Dimension(5, 0)));
		searchLinePanel.add(searchBtn);
		searchLinePanel.add(Box.createRigidArea(new Dimension(5, 0)));
		searchLinePanel.add(autoSearchCB);
		searchLinePanel.setAlignmentX(LEFT_ALIGNMENT);

		JLabel findLabel = new JLabel(NLS.str("search_dialog.open_by_name"));
		findLabel.setAlignmentX(LEFT_ALIGNMENT);

		JPanel searchFieldPanel = new JPanel();
		searchFieldPanel.setLayout(new BoxLayout(searchFieldPanel, BoxLayout.PAGE_AXIS));
		searchFieldPanel.setAlignmentX(LEFT_ALIGNMENT);
		searchFieldPanel.add(findLabel);
		searchFieldPanel.add(Box.createRigidArea(new Dimension(0, 5)));
		searchFieldPanel.add(searchLinePanel);

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
		searchOptions.add(makeOptionsCheckBox(NLS.str("search_dialog.ignorecase"), IGNORE_CASE));
		searchOptions.add(makeOptionsCheckBox(NLS.str("search_dialog.regex"), USE_REGEX));
		searchOptions.add(makeOptionsCheckBox(NLS.str("search_dialog.active_tab"), SearchOptions.ACTIVE_TAB));

		JPanel optionsPanel = new JPanel(new WrapLayout(WrapLayout.LEFT, 0, 0));
		optionsPanel.setAlignmentX(LEFT_ALIGNMENT);
		optionsPanel.add(searchInPanel);
		optionsPanel.add(searchOptions);

		JPanel searchPane = new JPanel();
		searchPane.setLayout(new BoxLayout(searchPane, BoxLayout.PAGE_AXIS));
		searchPane.add(searchFieldPanel);
		searchPane.add(Box.createRigidArea(new Dimension(0, 5)));
		searchPane.add(optionsPanel);

		initCommon();
		JPanel resultsPanel = initResultsTable();
		JPanel buttonPane = initButtonsPanel();

		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BorderLayout(5, 5));
		contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPanel.add(searchPane, BorderLayout.PAGE_START);
		contentPanel.add(resultsPanel, BorderLayout.CENTER);
		contentPanel.add(buttonPane, BorderLayout.PAGE_END);
		getContentPane().add(contentPanel);

		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}

	private void addSearchHistoryButton() {
		JButton searchHistoryButton = new JButton(new FlatSearchWithHistoryIcon(true));
		searchHistoryButton.setToolTipText(NLS.str("search_dialog.search_history"));
		searchHistoryButton.addActionListener(e -> {
			JPopupMenu popupMenu = new JPopupMenu();
			List<String> searchHistory = mainWindow.getProject().getSearchHistory();
			if (searchHistory.isEmpty()) {
				popupMenu.add("(empty)");
			} else {
				for (String str : searchHistory) {
					JMenuItem item = popupMenu.add(str);
					item.addActionListener(ev -> searchField.setText(str));
				}
			}
			popupMenu.show(searchHistoryButton, 0, searchHistoryButton.getHeight());
		});
		searchField.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_COMPONENT, searchHistoryButton);
	}

	protected void addResultsActions(JPanel resultsActionsPanel) {
		loadAllButton = new JButton(NLS.str("search_dialog.load_all"));
		loadAllButton.addActionListener(e -> loadMoreResults(true));
		loadAllButton.setEnabled(false);

		loadMoreButton = new JButton(NLS.str("search_dialog.load_more"));
		loadMoreButton.addActionListener(e -> loadMoreResults(false));
		loadMoreButton.setEnabled(false);

		stopBtn = new JButton(NLS.str("search_dialog.stop"));
		stopBtn.addActionListener(e -> pauseSearch());
		stopBtn.setEnabled(false);

		sortBtn = new JButton(NLS.str("search_dialog.sort_results"));
		sortBtn.addActionListener(e -> {
			synchronized (pendingResults) {
				resultsModel.sort();
				resultsTable.updateTable();
			}
		});
		sortBtn.setEnabled(false);

		resultsActionsPanel.add(loadAllButton);
		resultsActionsPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		resultsActionsPanel.add(loadMoreButton);
		resultsActionsPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		resultsActionsPanel.add(stopBtn);
		resultsActionsPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		resultsActionsPanel.add(stopBtn);
		super.addResultsActions(resultsActionsPanel);
		resultsActionsPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		resultsActionsPanel.add(sortBtn);
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

	private void initSearchEvents() {
		if (searchDisposable != null) {
			searchDisposable.dispose();
			searchDisposable = null;
		}
		searchEmitter = new SearchEventEmitter();
		Flowable<String> searchEvents;
		if (mainWindow.getSettings().isUseAutoSearch()) {
			searchEvents = Flowable.merge(RxUtils.textFieldChanges(searchField),
					RxUtils.textFieldEnterPress(searchField), searchEmitter.getFlowable());
		} else {
			searchEvents = Flowable.merge(RxUtils.textFieldEnterPress(searchField), searchEmitter.getFlowable());
		}
		searchDisposable = searchEvents
				.debounce(50, TimeUnit.MILLISECONDS)
				.observeOn(Schedulers.from(searchBackgroundExecutor))
				.subscribe(this::search);
	}

	private void search(String text) {
		UiUtils.notUiThreadGuard();
		stopSearchTask();
		UiUtils.uiRun(this::resetSearch);
		searchTask = prepareSearch(text);
		if (searchTask == null) {
			return;
		}
		UiUtils.uiRunAndWait(() -> {
			updateTableHighlight();
			prepareForSearch();
		});
		this.searchTask.setResultsLimit(mainWindow.getSettings().getSearchResultsPerPage());
		this.searchTask.setProgressListener(this::updateProgress);
		this.searchTask.fetchResults();
		LOG.debug("Total search items count estimation: {}", this.searchTask.getTaskProgress().total());
	}

	private SearchTask prepareSearch(String text) {
		if (text == null || options.isEmpty()) {
			return null;
		}
		// allow empty text for comments search
		if (text.isEmpty() && !options.contains(SearchOptions.COMMENT)) {
			return null;
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
			return null;
		}
		SearchTask newSearchTask = new SearchTask(mainWindow, this::addSearchResult, this::searchFinished);
		if (!buildSearch(newSearchTask, text, searchSettings)) {
			return null;
		}
		return newSearchTask;
	}

	private boolean buildSearch(SearchTask newSearchTask, String text, SearchSettings searchSettings) {
		List<JavaClass> allClasses;
		if (options.contains(ACTIVE_TAB)) {
			JumpPosition currentPos = mainWindow.getTabbedPane().getCurrentPosition();
			if (currentPos == null) {
				resultsInfoLabel.setText("Can't search in current tab");
				return false;
			}
			JNode currentNode = currentPos.getNode();
			if (currentNode instanceof JClass) {
				JClass activeCls = currentNode.getRootClass();
				searchSettings.setActiveCls(activeCls);
				allClasses = Collections.singletonList(activeCls.getCls());
			} else if (currentNode instanceof JResource) {
				searchSettings.setActiveResource((JResource) currentNode);
				allClasses = Collections.emptyList();
			} else {
				resultsInfoLabel.setText("Can't search in current tab");
				return false;
			}
		} else {
			allClasses = mainWindow.getWrapper().getIncludedClassesWithInners();
		}
		// allow empty text for comments search
		if (text.isEmpty() && options.contains(SearchOptions.COMMENT)) {
			newSearchTask.addProviderJob(new CommentSearchProvider(mainWindow, searchSettings));
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
			int clsCount = allClasses.size();
			if (clsCount == 1) {
				newSearchTask.addProviderJob(new CodeSearchProvider(mainWindow, searchSettings, allClasses));
			} else if (clsCount > 1) {
				List<List<JavaClass>> batches = mainWindow.getCacheObject().getDecompileBatches();
				if (batches == null) {
					List<JavaClass> topClasses = ListUtils.filter(allClasses, c -> !c.isInner());
					batches = mainWindow.getWrapper().buildDecompileBatches(topClasses);
					mainWindow.getCacheObject().setDecompileBatches(batches);
				}
				for (List<JavaClass> batch : batches) {
					newSearchTask.addProviderJob(new CodeSearchProvider(mainWindow, searchSettings, batch));
				}
			}
		}
		if (options.contains(RESOURCE)) {
			newSearchTask.addProviderJob(new ResourceSearchProvider(mainWindow, searchSettings, this));
		}
		if (options.contains(COMMENT)) {
			newSearchTask.addProviderJob(new CommentSearchProvider(mainWindow, searchSettings));
		}
		merged.prepare();
		newSearchTask.addProviderJob(merged);
		return true;
	}

	@Override
	protected void openItem(JNode node) {
		if (mainWindow.getSettings().isUseAutoSearch()) {
			// for auto search save only searches which leads to node opening
			mainWindow.getProject().addToSearchHistory(searchField.getText());
		}
		super.openItem(node);
	}

	private void pauseSearch() {
		stopBtn.setEnabled(false);
		searchBackgroundExecutor.execute(() -> {
			if (searchTask != null) {
				searchTask.cancel();
			}
		});
	}

	private void stopSearchTask() {
		UiUtils.notUiThreadGuard();
		if (searchTask != null) {
			searchTask.cancel();
			searchTask.waitTask();
			searchTask = null;
		}
	}

	private void loadMoreResults(boolean all) {
		searchBackgroundExecutor.execute(() -> {
			if (searchTask == null) {
				return;
			}
			searchTask.cancel();
			searchTask.waitTask();
			UiUtils.uiRunAndWait(this::prepareForSearch);
			if (all) {
				searchTask.setResultsLimit(0);
			}
			searchTask.fetchResults();
		});
	}

	private void resetSearch() {
		UiUtils.uiThreadGuard();
		resultsModel.clear();
		resultsTable.updateTable();
		synchronized (pendingResults) {
			pendingResults.clear();
		}
		updateProgressLabel("");
		progressPane.setVisible(false);
		warnLabel.setVisible(false);
		loadAllButton.setEnabled(false);
		loadMoreButton.setEnabled(false);
	}

	private void prepareForSearch() {
		UiUtils.uiThreadGuard();
		stopBtn.setEnabled(true);
		sortBtn.setEnabled(false);
		showSearchState();
		progressStartCommon();
	}

	private void addSearchResult(JNode node) {
		Objects.requireNonNull(node);
		synchronized (pendingResults) {
			UiUtils.notUiThreadGuard();
			pendingResults.add(node);
		}
	}

	private void updateTable() {
		synchronized (pendingResults) {
			UiUtils.uiThreadGuard();
			Collections.sort(pendingResults);
			resultsModel.addAll(pendingResults);
			pendingResults.clear();
			resultsTable.updateTable();
		}
	}

	private void updateTableHighlight() {
		String text = searchField.getText();
		updateHighlightContext(text, !options.contains(IGNORE_CASE), options.contains(USE_REGEX));
		cache.setLastSearch(text);
		cache.getLastSearchOptions().put(searchPreset, options);
		if (!mainWindow.getSettings().isUseAutoSearch()) {
			mainWindow.getProject().addToSearchHistory(text);
		}
	}

	private void updateProgress(ITaskProgress progress) {
		UiUtils.uiRun(() -> {
			progressPane.setProgress(progress);
			updateTable();
		});
	}

	public void updateProgressLabel(String text) {
		UiUtils.uiRun(() -> progressInfoLabel.setText(text));
	}

	private void searchFinished(ITaskInfo status, Boolean complete) {
		UiUtils.uiThreadGuard();
		LOG.debug("Search complete: {}, complete: {}", status, complete);
		loadAllButton.setEnabled(!complete);
		loadMoreButton.setEnabled(!complete);
		stopBtn.setEnabled(false);
		progressFinishedCommon();
		updateTable();
		updateProgressLabel(complete);
		sortBtn.setEnabled(resultsModel.getRowCount() != 0);
	}

	private void unloadTempData() {
		mainWindow.getWrapper().unloadClasses();
		System.gc();
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
