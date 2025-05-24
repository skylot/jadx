package jadx.gui.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeListener;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.icons.FlatSearchWithHistoryIcon;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Emitter;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import jadx.api.JavaClass;
import jadx.api.JavaPackage;
import jadx.core.utils.ListUtils;
import jadx.core.utils.StringUtils;
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
import jadx.gui.utils.Icons;
import jadx.gui.utils.JumpPosition;
import jadx.gui.utils.NLS;
import jadx.gui.utils.TextStandardActions;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.cache.ValueCache;
import jadx.gui.utils.rx.RxUtils;
import jadx.gui.utils.ui.DocumentUpdateListener;

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

	public static void searchPackage(MainWindow window, String packageName) {
		SearchDialog searchDialog = new SearchDialog(window, SearchPreset.TEXT, Collections.emptySet());
		searchDialog.initSearchPackage = packageName;
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

	private transient JTextField searchField;
	private transient JTextField packageField;

	private transient @Nullable SearchTask searchTask;
	private transient JButton loadAllButton;
	private transient JButton loadMoreButton;
	private transient JButton stopBtn;
	private transient JButton sortBtn;

	private transient Disposable searchDisposable;
	private transient SearchEventEmitter searchEmitter;
	private transient ChangeListener activeTabListener;

	private transient String initSearchText = null;
	private transient String initSearchPackage = null;

	// temporal list for pending results
	private final List<JNode> pendingResults = new ArrayList<>();

	/**
	 * Use single thread to do all background work, so additional synchronisation not needed
	 */
	private final Executor searchBackgroundExecutor = Executors.newSingleThreadExecutor();

	// save values between searches
	private final ValueCache<String, List<JavaClass>> includedClsCache = new ValueCache<>();
	private final ValueCache<List<JavaClass>, List<List<JavaClass>>> batchesCache = new ValueCache<>();

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
			mainWindow.getBackgroundExecutor().waitForComplete();
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
		String searchPackage = initSearchPackage != null ? initSearchPackage : cache.getLastSearchPackage();
		if (searchPackage != null) {
			packageField.setText(searchPackage);
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
		searchLinePanel.add(makeOptionsToggleButton(NLS.str("search_dialog.ignorecase"), Icons.ICON_MATCH, Icons.ICON_MATCH_SELECTED,
				SearchOptions.IGNORE_CASE));
		searchLinePanel.add(Box.createRigidArea(new Dimension(5, 0)));
		searchLinePanel.add(makeOptionsToggleButton(NLS.str("search_dialog.regex"), Icons.ICON_REGEX, Icons.ICON_REGEX_SELECTED,
				SearchOptions.USE_REGEX));
		searchLinePanel.add(Box.createRigidArea(new Dimension(5, 0)));
		searchLinePanel.add(makeOptionsToggleButton(NLS.str("search_dialog.active_tab"), Icons.ICON_ACTIVE_TAB,
				Icons.ICON_ACTIVE_TAB_SELECTED, SearchOptions.ACTIVE_TAB));
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

		packageField = new JTextField();
		packageField.setMaximumSize(new Dimension(Integer.MAX_VALUE, searchField.getPreferredSize().height));
		TextStandardActions.attach(packageField);
		packageField.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);

		JPanel searchPackageOptions = new JPanel();
		searchPackageOptions.setLayout(new BoxLayout(searchPackageOptions, BoxLayout.LINE_AXIS));
		searchPackageOptions.setBorder(BorderFactory.createTitledBorder(NLS.str("search_dialog.limit_package")));
		searchPackageOptions.add(packageField);

		JTextField fileExtField = new JTextField();
		TextStandardActions.attach(fileExtField);
		fileExtField.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
		fileExtField.getDocument().addDocumentListener(new DocumentUpdateListener(ev -> {
			String ext = fileExtField.getText();
			mainWindow.getSettings().setSrhResourceFileExt(ext);
		}));
		fileExtField.setText(mainWindow.getSettings().getSrhResourceFileExt());
		fileExtField.setMaximumSize(new Dimension(Integer.MAX_VALUE, searchField.getPreferredSize().height));

		JPanel searchExtFileOptions = new JPanel();
		searchExtFileOptions.setLayout(new BoxLayout(searchExtFileOptions, BoxLayout.LINE_AXIS));
		searchExtFileOptions.setBorder(BorderFactory.createTitledBorder(NLS.str("preferences.res_file_ext")));
		searchExtFileOptions.add(fileExtField);

		JPanel optionsPanel = new JPanel(new GridLayout(2, 2, 5, 5));
		optionsPanel.setAlignmentX(LEFT_ALIGNMENT);
		optionsPanel.add(searchInPanel);
		optionsPanel.add(searchPackageOptions);
		optionsPanel.add(searchExtFileOptions);

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
			searchEvents = Flowable.merge(List.of(
					RxUtils.textFieldChanges(searchField),
					RxUtils.textFieldEnterPress(searchField),
					RxUtils.textFieldChanges(packageField),
					RxUtils.textFieldEnterPress(packageField),
					searchEmitter.getFlowable()));
		} else {
			searchEvents = Flowable.merge(
					RxUtils.textFieldEnterPress(searchField),
					RxUtils.textFieldEnterPress(packageField),
					searchEmitter.getFlowable());
		}
		searchDisposable = searchEvents
				.debounce(50, TimeUnit.MILLISECONDS)
				.observeOn(Schedulers.from(searchBackgroundExecutor))
				.subscribe(t -> this.search(searchField.getText()));
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
		// allow empty text for search in comments
		if (text.isEmpty() && !options.contains(SearchOptions.COMMENT)) {
			return null;
		}
		LOG.debug("Building search for '{}', options: {}", text, options);
		boolean ignoreCase = options.contains(IGNORE_CASE);
		boolean useRegex = options.contains(USE_REGEX);
		String searchPackageText = packageField.getText();
		SearchSettings searchSettings = new SearchSettings(text, !ignoreCase, useRegex, searchPackageText);
		String error = searchSettings.prepare(mainWindow);
		UiUtils.highlightAsErrorField(searchField, !StringUtils.isEmpty(error));
		if (!StringUtils.isEmpty(error)) {
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
		List<JavaClass> searchClasses;
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
				searchClasses = Collections.singletonList(activeCls.getCls());
			} else if (currentNode instanceof JResource) {
				searchSettings.setActiveResource((JResource) currentNode);
				searchClasses = Collections.emptyList();
			} else {
				resultsInfoLabel.setText("Can't search in current tab");
				return false;
			}
		} else {
			searchClasses = includedClsCache.get(mainWindow.getSettings().getExcludedPackages(),
					exc -> mainWindow.getWrapper().getIncludedClassesWithInners());
		}
		JavaPackage searchPkg = searchSettings.getSearchPackage();
		if (searchPkg != null) {
			searchClasses = searchClasses.stream()
					.filter(searchSettings::isInSearchPkg)
					.collect(Collectors.toList());
		}
		if (text.isEmpty() && options.contains(SearchOptions.COMMENT)) {
			// allow empty text for comment search
			newSearchTask.addProviderJob(new CommentSearchProvider(mainWindow, searchSettings, searchClasses));
			return true;
		}
		if (!searchClasses.isEmpty()) {
			// using ordered execution for fast tasks
			MergedSearchProvider merged = new MergedSearchProvider();
			if (options.contains(CLASS)) {
				merged.add(new ClassSearchProvider(mainWindow, searchSettings, searchClasses));
			}
			if (options.contains(METHOD)) {
				merged.add(new MethodSearchProvider(mainWindow, searchSettings, searchClasses));
			}
			if (options.contains(FIELD)) {
				merged.add(new FieldSearchProvider(mainWindow, searchSettings, searchClasses));
			}
			if (!merged.isEmpty()) {
				merged.prepare();
				newSearchTask.addProviderJob(merged);
			}

			if (options.contains(CODE)) {
				int clsCount = searchClasses.size();
				if (clsCount == 1) {
					newSearchTask.addProviderJob(new CodeSearchProvider(mainWindow, searchSettings, searchClasses, null));
				} else if (clsCount > 1) {
					List<JavaClass> topClasses = ListUtils.filter(searchClasses, c -> !c.isInner());
					List<List<JavaClass>> batches = batchesCache.get(topClasses,
							clsList -> mainWindow.getWrapper().buildDecompileBatches(clsList));
					Set<JavaClass> includedClasses = new HashSet<>(topClasses);
					for (List<JavaClass> batch : batches) {
						newSearchTask.addProviderJob(new CodeSearchProvider(mainWindow, searchSettings, batch, includedClasses));
					}
				}
			}
			if (options.contains(COMMENT)) {
				newSearchTask.addProviderJob(new CommentSearchProvider(mainWindow, searchSettings, searchClasses));
			}
		}
		if (options.contains(RESOURCE)) {
			newSearchTask.addProviderJob(new ResourceSearchProvider(mainWindow, searchSettings, this));
		}
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
		updateHighlightContext(text, !options.contains(IGNORE_CASE), options.contains(USE_REGEX), false);
		cache.setLastSearch(text);
		cache.setLastSearchPackage(packageField.getText());
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

	private JToggleButton makeOptionsToggleButton(String name, ImageIcon icon, ImageIcon selectedIcon, final SearchOptions opt) {
		final JToggleButton toggleButton = new JToggleButton();
		toggleButton.setToolTipText(name);
		toggleButton.setIcon(icon);
		toggleButton.setSelectedIcon(selectedIcon);
		toggleButton.setSelected(options.contains(opt));
		toggleButton.addItemListener(e -> {
			if (toggleButton.isSelected()) {
				options.add(opt);
			} else {
				options.remove(opt);
			}
			searchEmitter.emitSearch();
		});
		return toggleButton;
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
