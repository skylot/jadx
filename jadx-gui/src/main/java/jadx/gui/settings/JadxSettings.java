package jadx.gui.settings;

import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.JFrame;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;

import jadx.api.CommentsLevel;
import jadx.api.DecompilationMode;
import jadx.api.JadxArgs;
import jadx.api.args.GeneratedRenamesMappingFileMode;
import jadx.api.args.ResourceNameSource;
import jadx.api.args.UserRenamesMappingsMode;
import jadx.cli.JadxCLIArgs;
import jadx.cli.LogHelper;
import jadx.gui.cache.code.CodeCacheMode;
import jadx.gui.cache.usage.UsageCacheMode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.EditorTheme;
import jadx.gui.utils.FontUtils;
import jadx.gui.utils.LafManager;
import jadx.gui.utils.LangLocale;
import jadx.gui.utils.NLS;

public class JadxSettings extends JadxCLIArgs {
	private static final Logger LOG = LoggerFactory.getLogger(JadxSettings.class);

	private static final Path USER_HOME = Paths.get(System.getProperty("user.home"));
	private static final int RECENT_PROJECTS_COUNT = 15;
	private static final int CURRENT_SETTINGS_VERSION = 18;

	private static final Font DEFAULT_FONT = new RSyntaxTextArea().getFont();

	static final Set<String> SKIP_FIELDS = new HashSet<>(Arrays.asList(
			"files", "input", "outDir", "outDirSrc", "outDirRes", "outputFormat",
			"deobfuscationMapFile",
			"verbose", "quiet", "logLevel",
			"printVersion", "printHelp"));

	private Path lastSaveProjectPath = USER_HOME;
	private Path lastOpenFilePath = USER_HOME;
	private Path lastSaveFilePath = USER_HOME;
	private boolean flattenPackage = false;
	private boolean checkForUpdates = true;
	private List<Path> recentProjects = new ArrayList<>();
	private String fontStr = "";
	private String smaliFontStr = "";
	private String editorThemePath = EditorTheme.getDefaultTheme().getPath();
	private String lafTheme = LafManager.INITIAL_THEME_NAME;
	private LangLocale langLocale = NLS.defaultLocale();
	private boolean autoStartJobs = false;
	private String excludedPackages = "";
	private boolean autoSaveProject = true;

	private boolean showHeapUsageBar = false;
	private boolean alwaysSelectOpened = false;
	private boolean useAlternativeFileDialog = false;

	private Map<String, WindowLocation> windowPos = new HashMap<>();
	private int mainWindowExtendedState = JFrame.NORMAL;
	private boolean codeAreaLineWrap = false;
	private int srhResourceSkipSize = 1000;
	private String srhResourceFileExt = ".xml|.html|.js|.json|.txt";
	private int searchResultsPerPage = 50;
	private boolean useAutoSearch = true;
	private boolean keepCommonDialogOpen = false;
	private boolean smaliAreaShowBytecode = false;
	private LineNumbersMode lineNumbersMode = LineNumbersMode.AUTO;

	private int mainWindowVerticalSplitterLoc = 300;
	private int debuggerStackFrameSplitterLoc = 300;
	private int debuggerVarTreeSplitterLoc = 700;

	private String adbDialogPath = "";
	private String adbDialogHost = "localhost";
	private String adbDialogPort = "5037";

	private CodeCacheMode codeCacheMode = CodeCacheMode.DISK_WITH_CACHE;
	private UsageCacheMode usageCacheMode = UsageCacheMode.DISK;
	private boolean jumpOnDoubleClick = true;

	/**
	 * UI setting: the width of the tree showing the classes, resources, ...
	 */
	private int treeWidth = 130;

	private boolean dockLogViewer = true;

	private int settingsVersion = CURRENT_SETTINGS_VERSION;

	@JadxSettingsAdapter.GsonExclude
	@Parameter(names = { "-sc", "--select-class" }, description = "GUI: Open the selected class and show the decompiled code")
	private String cmdSelectClass = null;

	public static JadxSettings makeDefault() {
		JadxSettings jadxSettings = new JadxSettings();
		jadxSettings.fixOnLoad();
		return jadxSettings;
	}

	public void sync() {
		JadxSettingsAdapter.store(this);
	}

	private void partialSync(Consumer<JadxSettings> updater) {
		JadxSettings settings = JadxSettingsAdapter.load();
		updater.accept(settings);
		JadxSettingsAdapter.store(settings);
	}

	public void fixOnLoad() {
		if (threadsCount <= 0) {
			threadsCount = JadxArgs.DEFAULT_THREADS_COUNT;
		}
		if (deobfuscationMinLength < 0) {
			deobfuscationMinLength = 0;
		}
		if (deobfuscationMaxLength < 0) {
			deobfuscationMaxLength = 0;
		}
		if (settingsVersion != CURRENT_SETTINGS_VERSION) {
			upgradeSettings(settingsVersion);
		}
	}

	public int getSettingsVersion() {
		return settingsVersion;
	}

	public void setSettingsVersion(int settingsVersion) {
		this.settingsVersion = settingsVersion;
	}

	public String getCmdSelectClass() {
		return cmdSelectClass;
	}

	public Path getLastOpenFilePath() {
		return lastOpenFilePath;
	}

	public void setLastOpenFilePath(Path lastOpenFilePath) {
		this.lastOpenFilePath = lastOpenFilePath;
		partialSync(settings -> settings.lastOpenFilePath = lastOpenFilePath);
	}

	public Path getLastSaveProjectPath() {
		return lastSaveProjectPath;
	}

	public Path getLastSaveFilePath() {
		return lastSaveFilePath;
	}

	public void setLastSaveProjectPath(Path lastSaveProjectPath) {
		this.lastSaveProjectPath = lastSaveProjectPath;
		partialSync(settings -> settings.lastSaveProjectPath = lastSaveProjectPath);
	}

	public void setLastSaveFilePath(Path lastSaveFilePath) {
		this.lastSaveFilePath = lastSaveFilePath;
		partialSync(settings -> settings.lastSaveFilePath = lastSaveFilePath);
	}

	public boolean isFlattenPackage() {
		return flattenPackage;
	}

	public void setFlattenPackage(boolean flattenPackage) {
		this.flattenPackage = flattenPackage;
		partialSync(settings -> settings.flattenPackage = flattenPackage);
	}

	public boolean isCheckForUpdates() {
		return checkForUpdates;
	}

	public void setCheckForUpdates(boolean checkForUpdates) {
		this.checkForUpdates = checkForUpdates;
		sync();
	}

	public List<Path> getRecentProjects() {
		return Collections.unmodifiableList(recentProjects);
	}

	public void addRecentProject(@Nullable Path projectPath) {
		if (projectPath == null) {
			return;
		}
		recentProjects.remove(projectPath);
		recentProjects.add(0, projectPath);
		int count = recentProjects.size();
		if (count > RECENT_PROJECTS_COUNT) {
			recentProjects.subList(RECENT_PROJECTS_COUNT, count).clear();
		}
		partialSync(settings -> settings.recentProjects = recentProjects);
	}

	public void removeRecentProject(Path projectPath) {
		recentProjects.remove(projectPath);
		partialSync(settings -> settings.recentProjects = recentProjects);
	}

	public void saveWindowPos(Window window) {
		WindowLocation pos = new WindowLocation(window.getClass().getSimpleName(), window.getBounds());
		windowPos.put(pos.getWindowId(), pos);
		partialSync(settings -> settings.windowPos = windowPos);
	}

	public boolean loadWindowPos(Window window) {
		WindowLocation pos = windowPos.get(window.getClass().getSimpleName());
		if (pos == null || pos.getBounds() == null) {
			return false;
		}
		if (!isAccessibleInAnyScreen(pos)) {
			return false;
		}
		window.setBounds(pos.getBounds());
		if (window instanceof MainWindow) {
			((JFrame) window).setExtendedState(getMainWindowExtendedState());
		}
		return true;
	}

	private static boolean isAccessibleInAnyScreen(WindowLocation pos) {
		Rectangle windowBounds = pos.getBounds();
		for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
			Rectangle screenBounds = gd.getDefaultConfiguration().getBounds();
			if (screenBounds.intersects(windowBounds)) {
				return true;
			}
		}
		LOG.debug("Window saved position was ignored: {}", pos);
		return false;
	}

	public boolean isShowHeapUsageBar() {
		return showHeapUsageBar;
	}

	public void setShowHeapUsageBar(boolean showHeapUsageBar) {
		this.showHeapUsageBar = showHeapUsageBar;
		partialSync(settings -> settings.showHeapUsageBar = showHeapUsageBar);
	}

	public boolean isAlwaysSelectOpened() {
		return alwaysSelectOpened;
	}

	public void setAlwaysSelectOpened(boolean alwaysSelectOpened) {
		this.alwaysSelectOpened = alwaysSelectOpened;
		partialSync(settings -> settings.alwaysSelectOpened = alwaysSelectOpened);
	}

	public boolean isUseAlternativeFileDialog() {
		return useAlternativeFileDialog;
	}

	public void setUseAlternativeFileDialog(boolean useAlternativeFileDialog) {
		this.useAlternativeFileDialog = useAlternativeFileDialog;
	}

	public String getExcludedPackages() {
		return excludedPackages;
	}

	public void setExcludedPackages(String excludedPackages) {
		this.excludedPackages = excludedPackages;
	}

	public void setThreadsCount(int threadsCount) {
		this.threadsCount = threadsCount;
	}

	public void setFallbackMode(boolean fallbackMode) {
		this.fallbackMode = fallbackMode;
	}

	public void setUseDx(boolean useDx) {
		this.useDx = useDx;
	}

	public void setSkipResources(boolean skipResources) {
		this.skipResources = skipResources;
	}

	public void setSkipSources(boolean skipSources) {
		this.skipSources = skipSources;
	}

	public void setDecompilationMode(DecompilationMode decompilationMode) {
		this.decompilationMode = decompilationMode;
	}

	public void setShowInconsistentCode(boolean showInconsistentCode) {
		this.showInconsistentCode = showInconsistentCode;
	}

	public LangLocale getLangLocale() {
		return this.langLocale;
	}

	public void setLangLocale(LangLocale langLocale) {
		this.langLocale = langLocale;
	}

	public void setCfgOutput(boolean cfgOutput) {
		this.cfgOutput = cfgOutput;
	}

	public void setRawCfgOutput(boolean rawCfgOutput) {
		this.rawCfgOutput = rawCfgOutput;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public void setDebugInfo(boolean useDebugInfo) {
		this.debugInfo = useDebugInfo;
	}

	public void setUserRenamesMappingsMode(UserRenamesMappingsMode mode) {
		this.userRenamesMappingsMode = mode;
	}

	public void setDeobfuscationOn(boolean deobfuscationOn) {
		this.deobfuscationOn = deobfuscationOn;
	}

	public void setDeobfuscationMinLength(int deobfuscationMinLength) {
		this.deobfuscationMinLength = deobfuscationMinLength;
	}

	public void setDeobfuscationMaxLength(int deobfuscationMaxLength) {
		this.deobfuscationMaxLength = deobfuscationMaxLength;
	}

	public void setGeneratedRenamesMappingFileMode(GeneratedRenamesMappingFileMode mode) {
		this.generatedRenamesMappingFileMode = mode;
	}

	public void setDeobfuscationUseSourceNameAsAlias(boolean deobfuscationUseSourceNameAsAlias) {
		this.deobfuscationUseSourceNameAsAlias = deobfuscationUseSourceNameAsAlias;
	}

	public void setDeobfuscationParseKotlinMetadata(boolean deobfuscationParseKotlinMetadata) {
		this.deobfuscationParseKotlinMetadata = deobfuscationParseKotlinMetadata;
	}

	public void setUseKotlinMethodsForVarNames(JadxArgs.UseKotlinMethodsForVarNames useKotlinMethodsForVarNames) {
		this.useKotlinMethodsForVarNames = useKotlinMethodsForVarNames;
	}

	public void setResourceNameSource(ResourceNameSource source) {
		this.resourceNameSource = source;
	}

	public void updateRenameFlag(JadxArgs.RenameEnum flag, boolean enabled) {
		if (enabled) {
			renameFlags.add(flag);
		} else {
			renameFlags.remove(flag);
		}
	}

	public void setEscapeUnicode(boolean escapeUnicode) {
		this.escapeUnicode = escapeUnicode;
	}

	public void setReplaceConsts(boolean replaceConsts) {
		this.replaceConsts = replaceConsts;
	}

	public void setRespectBytecodeAccessModifiers(boolean respectBytecodeAccessModifiers) {
		this.respectBytecodeAccessModifiers = respectBytecodeAccessModifiers;
	}

	public void setUseImports(boolean useImports) {
		this.useImports = useImports;
	}

	public void setInlineAnonymousClasses(boolean inlineAnonymousClasses) {
		this.inlineAnonymousClasses = inlineAnonymousClasses;
	}

	public void setInlineMethods(boolean inlineMethods) {
		this.inlineMethods = inlineMethods;
	}

	public void setAllowInlineKotlinLambda(boolean allowInlineKotlinLambda) {
		this.allowInlineKotlinLambda = allowInlineKotlinLambda;
	}

	public void setExtractFinally(boolean extractFinally) {
		this.extractFinally = extractFinally;
	}

	public void setFsCaseSensitive(boolean fsCaseSensitive) {
		this.fsCaseSensitive = fsCaseSensitive;
	}

	public boolean isAutoStartJobs() {
		return autoStartJobs;
	}

	public void setAutoStartJobs(boolean autoStartJobs) {
		this.autoStartJobs = autoStartJobs;
	}

	public boolean isAutoSaveProject() {
		return autoSaveProject;
	}

	public void setAutoSaveProject(boolean autoSaveProject) {
		this.autoSaveProject = autoSaveProject;
	}

	public void setExportAsGradleProject(boolean exportAsGradleProject) {
		this.exportAsGradleProject = exportAsGradleProject;
	}

	public int getTreeWidth() {
		return treeWidth;
	}

	public void setTreeWidth(int treeWidth) {
		this.treeWidth = treeWidth;
		partialSync(settings -> settings.treeWidth = JadxSettings.this.treeWidth);
	}

	@JadxSettingsAdapter.GsonExclude
	private Font cachedFont = null;

	public Font getFont() {
		if (cachedFont != null) {
			return cachedFont;
		}
		if (fontStr.isEmpty()) {
			return DEFAULT_FONT;
		}
		try {
			Font font = FontUtils.loadByStr(fontStr);
			this.cachedFont = font;
			return font;
		} catch (Exception e) {
			LOG.warn("Failed to load font: {}, reset to default", fontStr, e);
			setFont(DEFAULT_FONT);
			return DEFAULT_FONT;
		}
	}

	public void setFont(@Nullable Font font) {
		if (font == null) {
			setFontStr("");
		} else {
			setFontStr(FontUtils.convertToStr(font));
			this.cachedFont = font;
		}
	}

	public String getFontStr() {
		return fontStr;
	}

	public void setFontStr(String fontStr) {
		this.fontStr = fontStr;
		this.cachedFont = null;
	}

	public Font getSmaliFont() {
		if (smaliFontStr.isEmpty()) {
			return DEFAULT_FONT;
		}
		try {
			return FontUtils.loadByStr(smaliFontStr);
		} catch (Exception e) {
			LOG.warn("Failed to load font: {} for smali, reset to default", smaliFontStr, e);
			setSmaliFont(DEFAULT_FONT);
			return DEFAULT_FONT;
		}
	}

	public void setSmaliFont(@Nullable Font font) {
		if (font == null) {
			this.smaliFontStr = "";
		} else {
			this.smaliFontStr = FontUtils.convertToStr(font);
		}
	}

	public void setLogLevel(LogHelper.LogLevelEnum level) {
		this.logLevel = level;
	}

	public String getEditorThemePath() {
		return editorThemePath;
	}

	public void setEditorThemePath(String editorThemePath) {
		this.editorThemePath = editorThemePath;
	}

	public String getLafTheme() {
		return lafTheme;
	}

	public void setLafTheme(String lafTheme) {
		this.lafTheme = lafTheme;
	}

	public int getMainWindowExtendedState() {
		return mainWindowExtendedState;
	}

	public void setMainWindowExtendedState(int mainWindowExtendedState) {
		this.mainWindowExtendedState = mainWindowExtendedState;
		partialSync(settings -> settings.mainWindowExtendedState = mainWindowExtendedState);
	}

	public void setCodeAreaLineWrap(boolean lineWrap) {
		this.codeAreaLineWrap = lineWrap;
	}

	public boolean isCodeAreaLineWrap() {
		return this.codeAreaLineWrap;
	}

	public int getSrhResourceSkipSize() {
		return srhResourceSkipSize;
	}

	public void setSrhResourceSkipSize(int size) {
		srhResourceSkipSize = size;
	}

	public String getSrhResourceFileExt() {
		return srhResourceFileExt;
	}

	public void setSrhResourceFileExt(String all) {
		srhResourceFileExt = all.trim();
	}

	public int getSearchResultsPerPage() {
		return searchResultsPerPage;
	}

	public void setSearchResultsPerPage(int searchResultsPerPage) {
		this.searchResultsPerPage = searchResultsPerPage;
	}

	public boolean isUseAutoSearch() {
		return useAutoSearch;
	}

	public void setUseAutoSearch(boolean useAutoSearch) {
		this.useAutoSearch = useAutoSearch;
		partialSync(settings -> settings.useAutoSearch = useAutoSearch);
	}

	public void setKeepCommonDialogOpen(boolean yes) {
		keepCommonDialogOpen = yes;
	}

	public boolean getKeepCommonDialogOpen() {
		return keepCommonDialogOpen;
	}

	public void setSmaliAreaShowBytecode(boolean yes) {
		smaliAreaShowBytecode = yes;
	}

	public boolean getSmaliAreaShowBytecode() {
		return smaliAreaShowBytecode;
	}

	public void setMainWindowVerticalSplitterLoc(int location) {
		mainWindowVerticalSplitterLoc = location;
		partialSync(settings -> settings.mainWindowVerticalSplitterLoc = location);
	}

	public int getMainWindowVerticalSplitterLoc() {
		return mainWindowVerticalSplitterLoc;
	}

	public void setDebuggerStackFrameSplitterLoc(int location) {
		debuggerStackFrameSplitterLoc = location;
		partialSync(settings -> settings.debuggerStackFrameSplitterLoc = location);
	}

	public int getDebuggerStackFrameSplitterLoc() {
		return debuggerStackFrameSplitterLoc;
	}

	public void setDebuggerVarTreeSplitterLoc(int location) {
		debuggerVarTreeSplitterLoc = location;
		partialSync(settings -> debuggerVarTreeSplitterLoc = location);
	}

	public int getDebuggerVarTreeSplitterLoc() {
		return debuggerVarTreeSplitterLoc;
	}

	public String getAdbDialogPath() {
		return adbDialogPath;
	}

	public void setAdbDialogPath(String path) {
		this.adbDialogPath = path;
	}

	public String getAdbDialogHost() {
		return adbDialogHost;
	}

	public void setAdbDialogHost(String host) {
		this.adbDialogHost = host;
	}

	public String getAdbDialogPort() {
		return adbDialogPort;
	}

	public void setAdbDialogPort(String port) {
		this.adbDialogPort = port;
	}

	public void setCommentsLevel(CommentsLevel level) {
		this.commentsLevel = level;
	}

	public LineNumbersMode getLineNumbersMode() {
		return lineNumbersMode;
	}

	public void setLineNumbersMode(LineNumbersMode lineNumbersMode) {
		this.lineNumbersMode = lineNumbersMode;
	}

	public void setPluginOptions(Map<String, String> pluginOptions) {
		this.pluginOptions = pluginOptions;
	}

	public CodeCacheMode getCodeCacheMode() {
		return codeCacheMode;
	}

	public void setCodeCacheMode(CodeCacheMode codeCacheMode) {
		this.codeCacheMode = codeCacheMode;
	}

	public UsageCacheMode getUsageCacheMode() {
		return usageCacheMode;
	}

	public void setUsageCacheMode(UsageCacheMode usageCacheMode) {
		this.usageCacheMode = usageCacheMode;
	}

	public boolean isJumpOnDoubleClick() {
		return jumpOnDoubleClick;
	}

	public void setJumpOnDoubleClick(boolean jumpOnDoubleClick) {
		this.jumpOnDoubleClick = jumpOnDoubleClick;
	}

	public boolean isDockLogViewer() {
		return dockLogViewer;
	}

	public void setDockLogViewer(boolean dockLogViewer) {
		this.dockLogViewer = dockLogViewer;
		partialSync(settings -> this.dockLogViewer = dockLogViewer);
	}

	private void upgradeSettings(int fromVersion) {
		LOG.debug("upgrade settings from version: {} to {}", fromVersion, CURRENT_SETTINGS_VERSION);
		if (fromVersion <= 10) {
			fromVersion = 11;
		}
		if (fromVersion == 11) {
			inlineMethods = true;
			fromVersion++;
		}
		if (fromVersion == 12) {
			alwaysSelectOpened = false;
			fromVersion++;
		}
		if (fromVersion == 13) {
			lafTheme = LafManager.INITIAL_THEME_NAME;
			fromVersion++;
		}
		if (fromVersion == 14) {
			useKotlinMethodsForVarNames = JadxArgs.UseKotlinMethodsForVarNames.APPLY;
			fromVersion++;
		}
		if (fromVersion == 15) {
			generatedRenamesMappingFileMode = GeneratedRenamesMappingFileMode.getDefault();
			fromVersion++;
		}
		if (fromVersion == 16) {
			if (fallbackMode) {
				decompilationMode = DecompilationMode.FALLBACK;
			} else {
				decompilationMode = DecompilationMode.AUTO;
			}
			fromVersion++;
		}
		if (fromVersion == 17) {
			checkForUpdates = true;
			fromVersion++;
		}
		if (fromVersion != CURRENT_SETTINGS_VERSION) {
			LOG.warn("Incorrect settings upgrade. Expected version: {}, got: {}", CURRENT_SETTINGS_VERSION, fromVersion);
		}
		settingsVersion = CURRENT_SETTINGS_VERSION;
		sync();
	}

	@Override
	protected JadxCLIArgs newInstance() {
		return new JadxSettings();
	}
}
