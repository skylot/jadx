package jadx.gui.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
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
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.RowFilter;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatClientProperties;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Emitter;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import jadx.api.JavaClass;
import jadx.core.utils.StringUtils;
import jadx.gui.JadxWrapper;
import jadx.gui.jobs.ITaskInfo;
import jadx.gui.jobs.ITaskProgress;
import jadx.gui.search.SearchSettings;
import jadx.gui.strings.SingleStringResult;
import jadx.gui.strings.StringResult;
import jadx.gui.strings.StringsTask;
import jadx.gui.strings.caching.IStringsInfoCache;
import jadx.gui.strings.pkg.PackageFilter;
import jadx.gui.strings.providers.CacheStringsProvider;
import jadx.gui.strings.providers.FallbackStringsProviderDelegate;
import jadx.gui.strings.providers.JavaClassStringReader;
import jadx.gui.strings.providers.ListStringsProviderDelegate;
import jadx.gui.strings.providers.SmaliStringsProvider;
import jadx.gui.strings.providers.StringsProviderDelegate;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.dialog.SearchDialog.SearchOptions;
import jadx.gui.ui.dialog.StringsPackageFilterDialog.PackageFilterUpdateType;
import jadx.gui.utils.Icons;
import jadx.gui.utils.JadxNodeWrapper;
import jadx.gui.utils.NLS;
import jadx.gui.utils.TextStandardActions;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.cache.ValueCache;
import jadx.gui.utils.layout.WrapLayout;

public class StringsDialog extends CommonStringsDialog {

	private static final Logger LOG = LoggerFactory.getLogger(StringsDialog.class);

	public static void open(MainWindow window) {
		StringsDialog stringsDialog = new StringsDialog(window);
		show(stringsDialog, window);
	}

	private static void show(StringsDialog stringsDialog, MainWindow mw) {
		mw.addLoadListener(loaded -> {
			if (!loaded) {
				stringsDialog.dispose();
				return true;
			}
			return false;
		});
		stringsDialog.setVisible(true);
	}

	private final Executor stringBackgroundExecutor = Executors.newSingleThreadExecutor();

	private final transient ValueCache<String, List<JavaClass>> includedClsCache = new ValueCache<>();
	private final transient Set<PackageFilter> pendingPackageFilters = new HashSet<>();
	private final transient List<StringResult> pendingResults;
	private final transient MainWindow mainWindow;
	private final transient Set<SearchDialog.SearchOptions> options;

	private transient StringsTask stringsTask;
	private transient JTextField searchField;
	private transient StringsEventEmitter stringsEmitter;
	private transient Disposable stringsDisposable;
	private transient SearchSettings searchSettings;
	private transient JButton stopBtn;
	private transient JButton sortBtn;
	private transient JCheckBox minimumStringSizeButton;
	private transient JSpinner minimumStringSizeSpinner;

	public StringsDialog(final MainWindow mainWindow) {
		super(mainWindow, NLS.str("menu.strings"));

		this.mainWindow = mainWindow;
		this.pendingResults = new LinkedList<>();
		this.options = buildOptions();

		initUi();
		loadWindowPos();
		initStringEvents();
		registerInitOnOpen();

		stringsEmitter.emitSearch();
	}

	@Override
	public void dispose() {
		resultsModel.clear();

		stringBackgroundExecutor.execute(() -> {
			stopStringsTask();
			mainWindow.getBackgroundExecutor().waitForComplete();
			unloadTempData();
		});

		super.dispose();
	}

	public void loadWindowPos() {
		if (!mainWindow.getSettings().loadWindowPos(this)) {
			setSize(800, 500);
		}
	}

	public void updateProgressLabel(final String text) {
		UiUtils.uiRun(() -> progressInfoLabel.setText(text));
	}

	public void packageFiltersChanged(final PackageFilter modifiedPackageFilter, final PackageFilterUpdateType updateType) {
		switch (updateType) {
			case ADDED:
				this.pendingPackageFilters.add(modifiedPackageFilter);
				updateSearch();
				break;
			case DELETED:
				if (this.pendingPackageFilters.contains(modifiedPackageFilter)) {
					this.pendingPackageFilters.remove(modifiedPackageFilter);
					updateSearch();
				} else {
					refreshSearch();
				}
				break;
			case EDITED:
				refreshSearch();
				break;
			default:
				refreshSearch();
				break;
		}
	}

	@Override
	protected void addResultsActions(final JPanel resultsActionsPanel) {
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

		resultsActionsPanel.add(stopBtn);
		resultsActionsPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		resultsActionsPanel.add(stopBtn);
		super.addResultsActions(resultsActionsPanel);
		resultsActionsPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		resultsActionsPanel.add(sortBtn);
	}

	private void pauseSearch() {
		stopBtn.setEnabled(false);
		stringBackgroundExecutor.execute(() -> {
			if (stringsTask != null) {
				stringsTask.cancel();
			}
		});
	}

	private void unloadTempData() {
		mainWindow.getWrapper().unloadClasses();
		System.gc();
	}

	private void initUi() {
		initCommon();
		final JPanel resultsPanel = initResultsTable();
		final TableRowSorter<ResultsModel> sorter = new TableRowSorter<>(resultsModel);
		sorter.setRowFilter(new StringResultTableRowSorter());
		resultsTable.setRowSorter(sorter);

		final JPanel buttonPane = initButtonsPanel();
		searchField = new JTextField();
		searchField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(final DocumentEvent e) {
				updateSearch();
			}

			@Override
			public void removeUpdate(final DocumentEvent e) {
				updateSearch();
			}

			@Override
			public void changedUpdate(final DocumentEvent e) {
				updateSearch();
			}
		});
		TextStandardActions.attach(searchField);
		searchField.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);

		final JButton packageFilterButton = new JButton(NLS.str("strings.filter_pkg"));
		packageFilterButton.addActionListener((final ActionEvent ev) -> {
			final JDialog dialog = new StringsPackageFilterDialog(this, this::packageFiltersChanged);
			dialog.setVisible(true);
		});
		packageFilterButton.setIcon(Icons.PACKAGE);

		final JPanel searchButtons = new JPanel();
		searchButtons.setLayout(new BoxLayout(searchButtons, BoxLayout.LINE_AXIS));
		searchButtons.add(Box.createRigidArea(new Dimension(5, 0)));
		searchButtons.add(makeOptionsToggleButton(NLS.str("search_dialog.ignorecase"), Icons.ICON_MATCH, Icons.ICON_MATCH_SELECTED,
				SearchDialog.SearchOptions.IGNORE_CASE));
		searchButtons.add(Box.createRigidArea(new Dimension(5, 0)));
		searchButtons.add(makeOptionsToggleButton(NLS.str("search_dialog.regex"), Icons.ICON_REGEX, Icons.ICON_REGEX_SELECTED,
				SearchDialog.SearchOptions.USE_REGEX));

		final SpinnerModel minimumStringSizeSpinnerModel = new SpinnerNumberModel(5, 1, 1000, 1);
		minimumStringSizeSpinner = new JSpinner(minimumStringSizeSpinnerModel);
		minimumStringSizeSpinner.addChangeListener((final ChangeEvent ev) -> updateSearch());

		minimumStringSizeButton = new JCheckBox("Minimum");
		minimumStringSizeButton.setSelected(true);
		minimumStringSizeButton.addItemListener((final ItemEvent ev) -> {
			final boolean isEnabled = ev.getStateChange() == ItemEvent.SELECTED;
			minimumStringSizeSpinner.setEnabled(isEnabled);
			updateSearch();
		});

		final JPanel sizeOptionsPanel = new JPanel();
		sizeOptionsPanel.setLayout(new BoxLayout(sizeOptionsPanel, BoxLayout.LINE_AXIS));
		sizeOptionsPanel.setBorder(BorderFactory.createTitledBorder("String Size Filters"));
		sizeOptionsPanel.add(minimumStringSizeButton);
		sizeOptionsPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		sizeOptionsPanel.add(minimumStringSizeSpinner);

		final JPanel packageFiltersPanel = new JPanel();
		packageFiltersPanel.setLayout(new BoxLayout(packageFiltersPanel, BoxLayout.LINE_AXIS));
		packageFiltersPanel.setBorder(BorderFactory.createTitledBorder("Package Filters"));
		packageFiltersPanel.add(packageFilterButton);

		final JPanel optionsPanel = new JPanel();
		optionsPanel.setLayout(new WrapLayout(FlowLayout.LEFT));
		optionsPanel.add(packageFiltersPanel);
		optionsPanel.add(Box.createRigidArea(new Dimension(5, 0)));
		optionsPanel.add(sizeOptionsPanel);

		final JPanel searchFieldPanel = new JPanel();
		searchFieldPanel.setLayout(new BorderLayout(5, 5));
		searchFieldPanel.add(new JLabel(NLS.str("strings.filter_strings")), BorderLayout.LINE_START);
		searchFieldPanel.add(searchField, BorderLayout.CENTER);
		searchFieldPanel.add(searchButtons, BorderLayout.LINE_END);

		final JPanel filterPanel = new JPanel();
		filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.PAGE_AXIS));
		filterPanel.add(searchFieldPanel);
		filterPanel.add(optionsPanel);

		final JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BorderLayout(5, 5));
		contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPanel.add(filterPanel, BorderLayout.PAGE_START);
		contentPanel.add(resultsPanel, BorderLayout.CENTER);
		contentPanel.add(buttonPane, BorderLayout.PAGE_END);
		getContentPane().add(contentPanel);

		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}

	private void initStringEvents() {
		if (stringsDisposable != null) {
			stringsDisposable.dispose();
			stringsDisposable = null;
		}
		stringsEmitter = new StringsEventEmitter();
		final Flowable<StringResult> stringEvents = Flowable.merge(List.of(
				stringsEmitter.getFlowable()));
		stringsDisposable = stringEvents
				.debounce(50, TimeUnit.MILLISECONDS)
				.observeOn(Schedulers.from(stringBackgroundExecutor))
				.subscribe(t -> this.search(searchField.getText()));
	}

	private void search(final String text) {
		UiUtils.notUiThreadGuard();
		stopStringsTask();
		UiUtils.uiRun(this::resetSearch);
		stringsTask = prepareSearch(text);
		if (stringsTask == null) {
			return;
		}
		UiUtils.uiRunAndWait(() -> {
			updateTableHighlight();
			prepareForSearch();
		});
		final int allResults = 0;
		this.stringsTask.setResultsLimit(allResults);
		this.stringsTask.setProgressListener(this::updateProgress);
		this.stringsTask.fetchResults();
		LOG.debug("Total string search items count estimation: {}", this.stringsTask.getTaskProgress().total());
	}

	private void updateProgress(final ITaskProgress progress) {
		UiUtils.uiRun(() -> {
			progressPane.setProgress(progress);
			updateTable();
		});
	}

	private void updateTableHighlight() {
		cache.setLastSearch(searchField.getText());
		if (!mainWindow.getSettings().isUseAutoSearch()) {
			mainWindow.getProject().addToSearchHistory(searchField.getText());
		}
	}

	private void prepareForSearch() {
		UiUtils.uiThreadGuard();
		stopBtn.setEnabled(true);
		sortBtn.setEnabled(false);
		showSearchState();
		progressStartCommon();
	}

	private StringsTask prepareSearch(final String text) {
		if (text == null || options.isEmpty()) {
			return null;
		}
		LOG.debug("Building string search");

		final boolean successful = createSearchSettings();
		if (!successful) {
			return null;
		}

		final StringsTask newStringTask = new StringsTask(mainWindow, this::addSearchResult, this::searchFinished);
		if (!buildSearch(newStringTask, text)) {
			return null;
		}

		return newStringTask;
	}

	private boolean createSearchSettings() {
		searchSettings = new SearchSettings(searchField.getText());
		searchSettings.setIgnoreCase(options.contains(SearchDialog.SearchOptions.IGNORE_CASE));
		searchSettings.setUseRegex(options.contains(SearchDialog.SearchOptions.USE_REGEX));
		searchSettings.setSearchPkgStr("");
		searchSettings.setResFilterStr("");
		searchSettings.setResSizeLimit(Integer.MAX_VALUE);

		final String error = searchSettings.prepare(mainWindow);
		final boolean hasError = !StringUtils.isEmpty(error);
		UiUtils.highlightAsErrorField(searchField, hasError);
		if (hasError) {
			resultsInfoLabel.setText(error);
			searchSettings = null;
		}
		return !hasError;
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
	}

	private void refreshSearch() {
		this.pendingPackageFilters.clear();
		resetSearch();
		stringsEmitter.emitSearch();
	}

	private boolean buildSearch(final StringsTask newStringsTask, final String text) {
		final List<JavaClass> searchClasses = includedClsCache.get(mainWindow.getSettings().getExcludedPackages(),
				exc -> mainWindow.getWrapper().getIncludedClasses())
				.stream()
				.filter(cls -> StringsPackageFilterDialog.isPackageAllowed(cls.getJavaPackage()))
				.collect(Collectors.toList());
		if (options.contains(SearchOptions.CODE)) {
			final JadxWrapper jadxWrapper = mainWindow.getWrapper();
			final IStringsInfoCache cache = jadxWrapper.getStringsInfoCache();
			final JadxNodeWrapper nodeWrapper = new JadxNodeWrapper(jadxWrapper, getNodeCache());

			final CacheStringsProvider cacheProvider = new CacheStringsProvider(cache);

			final SmaliStringsProvider smaliProvider =
					new SmaliStringsProvider(nodeWrapper, new JavaClassStringReader(), cache);

			final StringsProviderDelegate cacheDelegate =
					ListStringsProviderDelegate.createListDelegateForProvider(cacheProvider, searchClasses);
			final StringsProviderDelegate smaliDelegate =
					FallbackStringsProviderDelegate.createFallbackForProvider(cacheProvider, smaliProvider);

			newStringsTask.addProviderJob(cacheDelegate);
			newStringsTask.addProviderJob(smaliDelegate);
		}
		return true;
	}

	private void stopStringsTask() {
		UiUtils.notUiThreadGuard();
		if (stringsTask != null) {
			stringsTask.cancel();
			stringsTask.waitTask();
			stringsTask = null;
		}
	}

	private Set<SearchDialog.SearchOptions> buildOptions() {
		final Set<SearchOptions> searchOptions = EnumSet.noneOf(SearchOptions.class);
		searchOptions.add(SearchOptions.CODE);
		return searchOptions;
	}

	private void addSearchResult(final StringResult result) {
		Objects.requireNonNull(result);
		synchronized (pendingResults) {
			UiUtils.notUiThreadGuard();
			pendingResults.add(result);
		}
	}

	private void searchFinished(final ITaskInfo status, final Boolean complete) {
		UiUtils.uiThreadGuard();
		LOG.debug("Search complete: {}, complete: {}", status, complete);
		stopBtn.setEnabled(false);
		progressFinishedCommon();
		updateTable();
		updateProgressLabel(complete);
		sortBtn.setEnabled(resultsModel.getRowCount() != 0);
	}

	private void updateTable() {
		synchronized (pendingResults) {
			UiUtils.uiThreadGuard();
			resultsModel.addAll(pendingResults);
			pendingResults.clear();
			resultsTable.updateTable();
		}
	}

	private JToggleButton makeOptionsToggleButton(final String name, final ImageIcon icon, final ImageIcon selectedIcon,
			final SearchDialog.SearchOptions opt) {
		final JToggleButton toggleButton = new JToggleButton();
		toggleButton.setToolTipText(name);
		toggleButton.setIcon(icon);
		toggleButton.setSelectedIcon(selectedIcon);
		toggleButton.setSelected(options.contains(opt));
		toggleButton.addItemListener((final ItemEvent ev) -> {
			if (toggleButton.isSelected()) {
				options.add(opt);
			} else {
				options.remove(opt);
			}
			updateSearch();
		});
		return toggleButton;
	}

	private void updateSearch() {
		final boolean successful = createSearchSettings();
		if (!successful) {
			return;
		}

		resultsModel.fireTableDataChanged();
	}

	@Override
	protected void openInit() {
		resultsTable.initColumnWidth();
	}

	private class StringResultTableRowSorter extends RowFilter<ResultsModel, Integer> {
		@Override
		public boolean include(final Entry<? extends ResultsModel, ? extends Integer> entry) {
			if (searchSettings == null) {
				return true;
			}

			final Object val = entry.getValue(STRING_NODE_COLUMN_INDEX);
			if (!StringResult.class.isAssignableFrom(val.getClass())) {
				throw new ClassCastException("Values within the first column of the Strings table must be of type StringResult");
			}

			final StringResult result = StringResult.class.cast(val);

			final boolean stringMatches = searchSettings.isMatch(result.toString());
			if (stringMatches == false) {
				return false;
			}

			final boolean useMinimumStringSize = minimumStringSizeButton.isSelected();
			if (useMinimumStringSize) {
				final int minimumStringSize = (int) minimumStringSizeSpinner.getValue();
				final String queriedString = result.getRepresentativeString();
				final String strippedString = queriedString.strip();
				final int actualStringSize = strippedString.length();
				if (actualStringSize < minimumStringSize) {
					return false;
				}
			}

			return result.isIncludedForPackageFilters(StringsPackageFilterDialog.STRINGS_PACKAGE_FILTERS);
		}
	}

	private class StringsEventEmitter {
		private final Flowable<StringResult> flowable;
		private Emitter<StringResult> emitter;

		public StringsEventEmitter() {
			flowable = Flowable.create(this::saveEmitter, BackpressureStrategy.LATEST);
		}

		public Flowable<StringResult> getFlowable() {
			return flowable;
		}

		private void saveEmitter(final Emitter<StringResult> emitter) {
			this.emitter = emitter;
		}

		public synchronized void emitSearch() {
			this.emitter.onNext(new SingleStringResult("", null));
		}
	}
}
