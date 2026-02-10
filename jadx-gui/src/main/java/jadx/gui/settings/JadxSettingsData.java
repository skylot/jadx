package jadx.gui.settings;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import org.jetbrains.annotations.Nullable;

import com.google.gson.annotations.SerializedName;

import jadx.cli.LogHelper;
import jadx.gui.cache.code.CodeCacheMode;
import jadx.gui.cache.usage.UsageCacheMode;
import jadx.gui.settings.data.SaveOptionEnum;
import jadx.gui.ui.action.ActionModel;
import jadx.gui.ui.tab.dnd.TabDndGhostType;
import jadx.gui.utils.LafManager;
import jadx.gui.utils.LangLocale;
import jadx.gui.utils.NLS;
import jadx.gui.utils.shortcut.Shortcut;

/**
 * Data class to hold all jadx-gui settings.
 * Also inherit all options from jadx-cli.
 * Serialized/deserialized as JSON in {@link jadx.cli.config.JadxConfigAdapter}.
 * Annotation {@link JadxConfigExcludeExport} used to exclude environment (files, window states)
 * fields from copy/export.
 */
public class JadxSettingsData extends JadxGUIArgs {
	public static final int CURRENT_SETTINGS_VERSION = 23;

	private static final Path USER_HOME = Paths.get(System.getProperty("user.home"));

	@JadxConfigExcludeExport
	private Path lastSaveProjectPath = USER_HOME;
	@JadxConfigExcludeExport
	private Path lastOpenFilePath = USER_HOME;
	@JadxConfigExcludeExport
	private Path lastSaveFilePath = USER_HOME;
	@JadxConfigExcludeExport
	private List<Path> recentProjects = new ArrayList<>();

	@JadxConfigExcludeExport
	private Map<String, WindowLocation> windowPos = new HashMap<>();
	@JadxConfigExcludeExport
	private int mainWindowExtendedState = JFrame.NORMAL;

	private boolean flattenPackage = false;
	private boolean checkForUpdates = true;
	private JadxUpdateChannel jadxUpdateChannel = JadxUpdateChannel.STABLE;

	private float uiZoom = 1.0f;
	private boolean applyUiZoomToFonts = true;

	private String uiFontStr = "";
	@SerializedName(value = "codeFontStr", alternate = "fontStr")
	private String codeFontStr = "";
	private String smaliFontStr = "";

	private String editorTheme = "";

	private String lafTheme = LafManager.INITIAL_THEME_NAME;
	private LangLocale langLocale = NLS.defaultLocale();
	private boolean autoStartJobs = false;
	private String excludedPackages = "";
	private SaveOptionEnum saveOption = SaveOptionEnum.ASK;

	private Map<ActionModel, Shortcut> shortcuts = new HashMap<>();

	private boolean showHeapUsageBar = false;
	private boolean alwaysSelectOpened = false;
	private boolean enablePreviewTab = false;
	private boolean useAlternativeFileDialog = false;
	private boolean codeAreaLineWrap = false;
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

	private CodeCacheMode codeCacheMode = CodeCacheMode.DISK;
	private UsageCacheMode usageCacheMode = UsageCacheMode.DISK;

	/**
	 * Cache dir option values:
	 * null - default (system)
	 * "." - at project dir
	 * other - custom
	 */
	private @Nullable String cacheDir = null;

	private boolean jumpOnDoubleClick = true;
	private boolean disableTooltipOnHover = false;

	private XposedCodegenLanguage xposedCodegenLanguage = XposedCodegenLanguage.JAVA;

	private int treeWidth = 130;
	private boolean dockLogViewer = true;
	private boolean dockQuickTabs = false;
	private TabDndGhostType tabDndGhostType = TabDndGhostType.OUTLINE;

	private int settingsVersion = CURRENT_SETTINGS_VERSION;

	public JadxSettingsData() {
		this.logLevel = LogHelper.LogLevelEnum.INFO;
	}

	public String getAdbDialogHost() {
		return adbDialogHost;
	}

	public void setAdbDialogHost(String adbDialogHost) {
		this.adbDialogHost = adbDialogHost;
	}

	public String getAdbDialogPath() {
		return adbDialogPath;
	}

	public void setAdbDialogPath(String adbDialogPath) {
		this.adbDialogPath = adbDialogPath;
	}

	public String getAdbDialogPort() {
		return adbDialogPort;
	}

	public void setAdbDialogPort(String adbDialogPort) {
		this.adbDialogPort = adbDialogPort;
	}

	public boolean isAlwaysSelectOpened() {
		return alwaysSelectOpened;
	}

	public void setAlwaysSelectOpened(boolean alwaysSelectOpened) {
		this.alwaysSelectOpened = alwaysSelectOpened;
	}

	public boolean isAutoStartJobs() {
		return autoStartJobs;
	}

	public void setAutoStartJobs(boolean autoStartJobs) {
		this.autoStartJobs = autoStartJobs;
	}

	public @Nullable String getCacheDir() {
		return cacheDir;
	}

	public void setCacheDir(@Nullable String cacheDir) {
		this.cacheDir = cacheDir;
	}

	public boolean isCheckForUpdates() {
		return checkForUpdates;
	}

	public void setCheckForUpdates(boolean checkForUpdates) {
		this.checkForUpdates = checkForUpdates;
	}

	public boolean isCodeAreaLineWrap() {
		return codeAreaLineWrap;
	}

	public void setCodeAreaLineWrap(boolean codeAreaLineWrap) {
		this.codeAreaLineWrap = codeAreaLineWrap;
	}

	public CodeCacheMode getCodeCacheMode() {
		return codeCacheMode;
	}

	public void setCodeCacheMode(CodeCacheMode codeCacheMode) {
		this.codeCacheMode = codeCacheMode;
	}

	public int getDebuggerStackFrameSplitterLoc() {
		return debuggerStackFrameSplitterLoc;
	}

	public void setDebuggerStackFrameSplitterLoc(int debuggerStackFrameSplitterLoc) {
		this.debuggerStackFrameSplitterLoc = debuggerStackFrameSplitterLoc;
	}

	public int getDebuggerVarTreeSplitterLoc() {
		return debuggerVarTreeSplitterLoc;
	}

	public void setDebuggerVarTreeSplitterLoc(int debuggerVarTreeSplitterLoc) {
		this.debuggerVarTreeSplitterLoc = debuggerVarTreeSplitterLoc;
	}

	public boolean isDisableTooltipOnHover() {
		return disableTooltipOnHover;
	}

	public void setDisableTooltipOnHover(boolean disableTooltipOnHover) {
		this.disableTooltipOnHover = disableTooltipOnHover;
	}

	public boolean isDockLogViewer() {
		return dockLogViewer;
	}

	public void setDockLogViewer(boolean dockLogViewer) {
		this.dockLogViewer = dockLogViewer;
	}

	public boolean isDockQuickTabs() {
		return dockQuickTabs;
	}

	public void setDockQuickTabs(boolean dockQuickTabs) {
		this.dockQuickTabs = dockQuickTabs;
	}

	public String getEditorTheme() {
		return editorTheme;
	}

	public void setEditorTheme(String editorTheme) {
		this.editorTheme = editorTheme;
	}

	public boolean isEnablePreviewTab() {
		return enablePreviewTab;
	}

	public void setEnablePreviewTab(boolean enablePreviewTab) {
		this.enablePreviewTab = enablePreviewTab;
	}

	public String getExcludedPackages() {
		return excludedPackages;
	}

	public void setExcludedPackages(String excludedPackages) {
		this.excludedPackages = excludedPackages;
	}

	public boolean isFlattenPackage() {
		return flattenPackage;
	}

	public void setFlattenPackage(boolean flattenPackage) {
		this.flattenPackage = flattenPackage;
	}

	public JadxUpdateChannel getJadxUpdateChannel() {
		return jadxUpdateChannel;
	}

	public void setJadxUpdateChannel(JadxUpdateChannel jadxUpdateChannel) {
		this.jadxUpdateChannel = jadxUpdateChannel;
	}

	public boolean isJumpOnDoubleClick() {
		return jumpOnDoubleClick;
	}

	public void setJumpOnDoubleClick(boolean jumpOnDoubleClick) {
		this.jumpOnDoubleClick = jumpOnDoubleClick;
	}

	public boolean isKeepCommonDialogOpen() {
		return keepCommonDialogOpen;
	}

	public void setKeepCommonDialogOpen(boolean keepCommonDialogOpen) {
		this.keepCommonDialogOpen = keepCommonDialogOpen;
	}

	public String getLafTheme() {
		return lafTheme;
	}

	public void setLafTheme(String lafTheme) {
		this.lafTheme = lafTheme;
	}

	public LangLocale getLangLocale() {
		return langLocale;
	}

	public void setLangLocale(LangLocale langLocale) {
		this.langLocale = langLocale;
	}

	public Path getLastOpenFilePath() {
		return lastOpenFilePath;
	}

	public void setLastOpenFilePath(Path lastOpenFilePath) {
		this.lastOpenFilePath = lastOpenFilePath;
	}

	public Path getLastSaveFilePath() {
		return lastSaveFilePath;
	}

	public void setLastSaveFilePath(Path lastSaveFilePath) {
		this.lastSaveFilePath = lastSaveFilePath;
	}

	public Path getLastSaveProjectPath() {
		return lastSaveProjectPath;
	}

	public void setLastSaveProjectPath(Path lastSaveProjectPath) {
		this.lastSaveProjectPath = lastSaveProjectPath;
	}

	public LineNumbersMode getLineNumbersMode() {
		return lineNumbersMode;
	}

	public void setLineNumbersMode(LineNumbersMode lineNumbersMode) {
		this.lineNumbersMode = lineNumbersMode;
	}

	public int getMainWindowExtendedState() {
		return mainWindowExtendedState;
	}

	public void setMainWindowExtendedState(int mainWindowExtendedState) {
		this.mainWindowExtendedState = mainWindowExtendedState;
	}

	public int getMainWindowVerticalSplitterLoc() {
		return mainWindowVerticalSplitterLoc;
	}

	public void setMainWindowVerticalSplitterLoc(int mainWindowVerticalSplitterLoc) {
		this.mainWindowVerticalSplitterLoc = mainWindowVerticalSplitterLoc;
	}

	public List<Path> getRecentProjects() {
		return recentProjects;
	}

	public void setRecentProjects(List<Path> recentProjects) {
		this.recentProjects = recentProjects;
	}

	public SaveOptionEnum getSaveOption() {
		return saveOption;
	}

	public void setSaveOption(SaveOptionEnum saveOption) {
		this.saveOption = saveOption;
	}

	public int getSearchResultsPerPage() {
		return searchResultsPerPage;
	}

	public void setSearchResultsPerPage(int searchResultsPerPage) {
		this.searchResultsPerPage = searchResultsPerPage;
	}

	public int getSettingsVersion() {
		return settingsVersion;
	}

	public void setSettingsVersion(int settingsVersion) {
		this.settingsVersion = settingsVersion;
	}

	public Map<ActionModel, Shortcut> getShortcuts() {
		return shortcuts;
	}

	public void setShortcuts(Map<ActionModel, Shortcut> shortcuts) {
		this.shortcuts = shortcuts;
	}

	public boolean isShowHeapUsageBar() {
		return showHeapUsageBar;
	}

	public void setShowHeapUsageBar(boolean showHeapUsageBar) {
		this.showHeapUsageBar = showHeapUsageBar;
	}

	public boolean isSmaliAreaShowBytecode() {
		return smaliAreaShowBytecode;
	}

	public void setSmaliAreaShowBytecode(boolean smaliAreaShowBytecode) {
		this.smaliAreaShowBytecode = smaliAreaShowBytecode;
	}

	public String getUiFontStr() {
		return uiFontStr;
	}

	public void setUiFontStr(String uiFontStr) {
		this.uiFontStr = uiFontStr;
	}

	public String getCodeFontStr() {
		return codeFontStr;
	}

	public void setCodeFontStr(String codeFontStr) {
		this.codeFontStr = codeFontStr;
	}

	public String getSmaliFontStr() {
		return smaliFontStr;
	}

	public void setSmaliFontStr(String smaliFontStr) {
		this.smaliFontStr = smaliFontStr;
	}

	public TabDndGhostType getTabDndGhostType() {
		return tabDndGhostType;
	}

	public void setTabDndGhostType(TabDndGhostType tabDndGhostType) {
		this.tabDndGhostType = tabDndGhostType;
	}

	public int getTreeWidth() {
		return treeWidth;
	}

	public void setTreeWidth(int treeWidth) {
		this.treeWidth = treeWidth;
	}

	public float getUiZoom() {
		return uiZoom;
	}

	public void setUiZoom(float uiZoom) {
		this.uiZoom = uiZoom;
	}

	public boolean isApplyUiZoomToFonts() {
		return applyUiZoomToFonts;
	}

	public void setApplyUiZoomToFonts(boolean applyUiZoomToFonts) {
		this.applyUiZoomToFonts = applyUiZoomToFonts;
	}

	public UsageCacheMode getUsageCacheMode() {
		return usageCacheMode;
	}

	public void setUsageCacheMode(UsageCacheMode usageCacheMode) {
		this.usageCacheMode = usageCacheMode;
	}

	public boolean isUseAlternativeFileDialog() {
		return useAlternativeFileDialog;
	}

	public void setUseAlternativeFileDialog(boolean useAlternativeFileDialog) {
		this.useAlternativeFileDialog = useAlternativeFileDialog;
	}

	public boolean isUseAutoSearch() {
		return useAutoSearch;
	}

	public void setUseAutoSearch(boolean useAutoSearch) {
		this.useAutoSearch = useAutoSearch;
	}

	public Map<String, WindowLocation> getWindowPos() {
		return windowPos;
	}

	public void setWindowPos(Map<String, WindowLocation> windowPos) {
		this.windowPos = windowPos;
	}

	public XposedCodegenLanguage getXposedCodegenLanguage() {
		return xposedCodegenLanguage;
	}

	public void setXposedCodegenLanguage(XposedCodegenLanguage xposedCodegenLanguage) {
		this.xposedCodegenLanguage = xposedCodegenLanguage;
	}

}
