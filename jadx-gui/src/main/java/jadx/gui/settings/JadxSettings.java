package jadx.gui.settings;

import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;

import jadx.api.CommentsLevel;
import jadx.api.DecompilationMode;
import jadx.api.JadxArgs;
import jadx.api.args.GeneratedRenamesMappingFileMode;
import jadx.api.args.IntegerFormat;
import jadx.api.args.ResourceNameSource;
import jadx.api.args.UseSourceNameAsClassNameAlias;
import jadx.api.args.UserRenamesMappingsMode;
import jadx.cli.config.JadxConfigAdapter;
import jadx.cli.config.JadxConfigExclude;
import jadx.core.utils.GsonUtils;
import jadx.gui.cache.code.CodeCacheMode;
import jadx.gui.cache.usage.UsageCacheMode;
import jadx.gui.settings.data.SaveOptionEnum;
import jadx.gui.settings.data.ShortcutsWrapper;
import jadx.gui.settings.font.FontSettings;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.tab.dnd.TabDndGhostType;
import jadx.gui.utils.LangLocale;
import jadx.gui.utils.PathTypeAdapter;
import jadx.gui.utils.RectangleTypeAdapter;

import static jadx.gui.settings.JadxSettingsData.CURRENT_SETTINGS_VERSION;

public class JadxSettings {
	private static final Logger LOG = LoggerFactory.getLogger(JadxSettings.class);

	private static final int RECENT_PROJECTS_COUNT = 30;

	private final JadxConfigAdapter<JadxSettingsData> configAdapter;
	private final Object dataWriteSync = new Object();
	private final ShortcutsWrapper shortcutsWrapper = new ShortcutsWrapper();
	private final FontSettings fontSettings = new FontSettings();

	private JadxSettingsData settingsData;

	public JadxSettings(JadxConfigAdapter<JadxSettingsData> configAdapter) {
		this.configAdapter = configAdapter;
	}

	public static JadxConfigAdapter<JadxSettingsData> buildConfigAdapter() {
		return new JadxConfigAdapter<>(JadxSettingsData.class, "gui", gsonBuilder -> {
			gsonBuilder.registerTypeHierarchyAdapter(Path.class, PathTypeAdapter.singleton());
			gsonBuilder.registerTypeHierarchyAdapter(Rectangle.class, RectangleTypeAdapter.singleton());
		});
	}

	public String getSettingsJsonString() {
		return configAdapter.objectToJsonString(settingsData);
	}

	public void loadSettingsFromJsonString(String jsonStr) {
		loadSettingsData(configAdapter.jsonStringToObject(jsonStr));
	}

	public void loadSettingsData(JadxSettingsData settingsData) {
		this.settingsData = settingsData;
		upgradeSettings(settingsData.getSettingsVersion());
		fixOnLoad();
		// update custom fields
		shortcutsWrapper.updateShortcuts(settingsData.getShortcuts());
		fontSettings.bindData(settingsData);
	}

	private void upgradeSettings(int fromVersion) {
		if (settingsData.getSettingsVersion() == CURRENT_SETTINGS_VERSION) {
			return;
		}
		LOG.debug("upgrade settings from version: {} to {}", fromVersion, CURRENT_SETTINGS_VERSION);
		if (fromVersion <= 22) {
			fromVersion++;
		}
		if (fromVersion != CURRENT_SETTINGS_VERSION) {
			LOG.warn("Incorrect settings upgrade. Expected version: {}, got: {}", CURRENT_SETTINGS_VERSION, fromVersion);
		}
		settingsData.setSettingsVersion(CURRENT_SETTINGS_VERSION);
		sync();
	}

	private void fixOnLoad() {
		if (settingsData.getThreadsCount() <= 0) {
			settingsData.setThreadsCount(JadxArgs.DEFAULT_THREADS_COUNT);
		}
		if (settingsData.getDeobfuscationMinLength() < 0) {
			settingsData.setDeobfuscationMinLength(0);
		}
		if (settingsData.getDeobfuscationMaxLength() < 0) {
			settingsData.setDeobfuscationMaxLength(0);
		}
	}

	public void sync() {
		synchronized (dataWriteSync) {
			configAdapter.save(settingsData);
		}
	}

	public String exportSettingsString() {
		Gson gson = GsonUtils.defaultGsonBuilder()
				.setExclusionStrategies(new ExclusionStrategy() {
					@Override
					public boolean shouldSkipField(FieldAttributes f) {
						return f.getAnnotation(JadxConfigExclude.class) != null
								|| f.getAnnotation(JadxConfigExcludeExport.class) != null;
					}

					@Override
					public boolean shouldSkipClass(Class<?> clazz) {
						return false;
					}
				})
				.create();
		return gson.toJson(settingsData);
	}

	public JadxArgs toJadxArgs() {
		return settingsData.toJadxArgs();
	}

	public List<String> getFiles() {
		return settingsData.getFiles();
	}

	public String getCmdSelectClass() {
		return settingsData.getCmdSelectClass();
	}

	public Path getLastOpenFilePath() {
		return settingsData.getLastOpenFilePath();
	}

	public void setLastOpenFilePath(Path lastOpenFilePath) {
		settingsData.setLastOpenFilePath(lastOpenFilePath);
	}

	public Path getLastSaveProjectPath() {
		return settingsData.getLastSaveProjectPath();
	}

	public void setLastSaveProjectPath(Path lastSaveProjectPath) {
		settingsData.setLastSaveProjectPath(lastSaveProjectPath);
	}

	public Path getLastSaveFilePath() {
		return settingsData.getLastSaveFilePath();
	}

	public void setLastSaveFilePath(Path lastSaveFilePath) {
		settingsData.setLastSaveFilePath(lastSaveFilePath);
	}

	public boolean isFlattenPackage() {
		return settingsData.isFlattenPackage();
	}

	public void setFlattenPackage(boolean flattenPackage) {
		settingsData.setFlattenPackage(flattenPackage);
	}

	public boolean isCheckForUpdates() {
		return settingsData.isCheckForUpdates();
	}

	public void setCheckForUpdates(boolean checkForUpdates) {
		settingsData.setCheckForUpdates(checkForUpdates);
		sync();
	}

	public boolean isDisableTooltipOnHover() {
		return settingsData.isDisableTooltipOnHover();
	}

	public void setDisableTooltipOnHover(boolean disableTooltipOnHover) {
		settingsData.setDisableTooltipOnHover(disableTooltipOnHover);
	}

	public List<Path> getRecentProjects() {
		return Collections.unmodifiableList(settingsData.getRecentProjects());
	}

	public void addRecentProject(@Nullable Path projectPath) {
		if (projectPath == null) {
			return;
		}
		List<Path> recentProjects = settingsData.getRecentProjects();
		Path normPath = projectPath.toAbsolutePath().normalize();
		recentProjects.remove(normPath);
		recentProjects.add(0, normPath);
		int count = recentProjects.size();
		if (count > RECENT_PROJECTS_COUNT) {
			recentProjects.subList(RECENT_PROJECTS_COUNT, count).clear();
		}
	}

	public void removeRecentProject(Path projectPath) {
		List<Path> recentProjects = settingsData.getRecentProjects();
		recentProjects.remove(projectPath);
	}

	public void saveWindowPos(Window window) {
		synchronized (dataWriteSync) {
			WindowLocation pos = new WindowLocation(window.getClass().getSimpleName(), window.getBounds());
			settingsData.getWindowPos().put(pos.getWindowId(), pos);
		}
	}

	public boolean loadWindowPos(Window window) {
		Map<String, WindowLocation> windowPos = settingsData.getWindowPos();
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

	public int getMainWindowExtendedState() {
		return settingsData.getMainWindowExtendedState();
	}

	public void setMainWindowExtendedState(int mainWindowExtendedState) {
		settingsData.setMainWindowExtendedState(mainWindowExtendedState);
	}

	public boolean isShowHeapUsageBar() {
		return settingsData.isShowHeapUsageBar();
	}

	public void setShowHeapUsageBar(boolean showHeapUsageBar) {
		settingsData.setShowHeapUsageBar(showHeapUsageBar);
	}

	public boolean isAlwaysSelectOpened() {
		return settingsData.isAlwaysSelectOpened();
	}

	public void setAlwaysSelectOpened(boolean alwaysSelectOpened) {
		settingsData.setAlwaysSelectOpened(alwaysSelectOpened);
	}

	public boolean isEnablePreviewTab() {
		return settingsData.isEnablePreviewTab();
	}

	public void setEnablePreviewTab(boolean enablePreviewTab) {
		settingsData.setEnablePreviewTab(enablePreviewTab);
	}

	public boolean isUseAlternativeFileDialog() {
		return settingsData.isUseAlternativeFileDialog();
	}

	public void setUseAlternativeFileDialog(boolean useAlternativeFileDialog) {
		settingsData.setUseAlternativeFileDialog(useAlternativeFileDialog);
	}

	public String getExcludedPackages() {
		return settingsData.getExcludedPackages();
	}

	public void setExcludedPackages(String excludedPackages) {
		settingsData.setExcludedPackages(excludedPackages);
	}

	public LangLocale getLangLocale() {
		return settingsData.getLangLocale();
	}

	public void setLangLocale(LangLocale langLocale) {
		settingsData.setLangLocale(langLocale);
	}

	public boolean isAutoStartJobs() {
		return settingsData.isAutoStartJobs();
	}

	public void setAutoStartJobs(boolean autoStartJobs) {
		settingsData.setAutoStartJobs(autoStartJobs);
	}

	public ShortcutsWrapper getShortcuts() {
		return shortcutsWrapper;
	}

	public int getTreeWidth() {
		return settingsData.getTreeWidth();
	}

	public void setTreeWidth(int treeWidth) {
		settingsData.setTreeWidth(treeWidth);
	}

	public float getUiZoom() {
		return settingsData.getUiZoom();
	}

	public void setUiZoom(float uiZoom) {
		settingsData.setUiZoom(uiZoom);
		fontSettings.applyUiZoom(uiZoom, isApplyUiZoomToFonts());
	}

	public boolean isApplyUiZoomToFonts() {
		return settingsData.isApplyUiZoomToFonts();
	}

	public void setApplyUiZoomToFonts(boolean applyUiZoomToFonts) {
		settingsData.setApplyUiZoomToFonts(applyUiZoomToFonts);
		fontSettings.applyUiZoom(getUiZoom(), applyUiZoomToFonts);
	}

	public FontSettings getFontSettings() {
		return fontSettings;
	}

	public Font getUiFont() {
		return fontSettings.getUiFontAdapter().getEffectiveFont();
	}

	public void setUiFont(Font font) {
		fontSettings.getUiFontAdapter().setFont(font);
	}

	public Font getCodeFont() {
		return fontSettings.getCodeFontAdapter().getEffectiveFont();
	}

	public void setCodeFont(Font font) {
		fontSettings.getCodeFontAdapter().setFont(font);
	}

	public Font getSmaliFont() {
		return fontSettings.getSmaliFontAdapter().getEffectiveFont();
	}

	public void setSmaliFont(Font font) {
		fontSettings.getSmaliFontAdapter().setFont(font);
	}

	public String getEditorTheme() {
		return settingsData.getEditorTheme();
	}

	public void setEditorTheme(String editorTheme) {
		settingsData.setEditorTheme(editorTheme);
	}

	public String getLafTheme() {
		return settingsData.getLafTheme();
	}

	public void setLafTheme(String lafTheme) {
		settingsData.setLafTheme(lafTheme);
	}

	public boolean isCodeAreaLineWrap() {
		return settingsData.isCodeAreaLineWrap();
	}

	public void setCodeAreaLineWrap(boolean lineWrap) {
		settingsData.setCodeAreaLineWrap(lineWrap);
	}

	public int getSearchResultsPerPage() {
		return settingsData.getSearchResultsPerPage();
	}

	public void setSearchResultsPerPage(int searchResultsPerPage) {
		settingsData.setSearchResultsPerPage(searchResultsPerPage);
	}

	public boolean isUseAutoSearch() {
		return settingsData.isUseAutoSearch();
	}

	public void saveUseAutoSearch(boolean useAutoSearch) {
		settingsData.setUseAutoSearch(useAutoSearch);
		sync();
	}

	public void saveKeepCommonDialogOpen(boolean keepCommonDialogOpen) {
		settingsData.setKeepCommonDialogOpen(keepCommonDialogOpen);
		sync();
	}

	public boolean isKeepCommonDialogOpen() {
		return settingsData.isKeepCommonDialogOpen();
	}

	public int getMainWindowVerticalSplitterLoc() {
		return settingsData.getMainWindowVerticalSplitterLoc();
	}

	public void setMainWindowVerticalSplitterLoc(int location) {
		settingsData.setMainWindowVerticalSplitterLoc(location);
	}

	public int getDebuggerStackFrameSplitterLoc() {
		return settingsData.getDebuggerStackFrameSplitterLoc();
	}

	public void setDebuggerStackFrameSplitterLoc(int location) {
		settingsData.setDebuggerStackFrameSplitterLoc(location);
	}

	public int getDebuggerVarTreeSplitterLoc() {
		return settingsData.getDebuggerVarTreeSplitterLoc();
	}

	public void setDebuggerVarTreeSplitterLoc(int location) {
		settingsData.setDebuggerVarTreeSplitterLoc(location);
	}

	public String getAdbDialogHost() {
		return settingsData.getAdbDialogHost();
	}

	public void setAdbDialogHost(String adbDialogHost) {
		settingsData.setAdbDialogHost(adbDialogHost);
	}

	public String getAdbDialogPath() {
		return settingsData.getAdbDialogPath();
	}

	public void setAdbDialogPath(String adbDialogPath) {
		settingsData.setAdbDialogPath(adbDialogPath);
	}

	public String getAdbDialogPort() {
		return settingsData.getAdbDialogPort();
	}

	public void setAdbDialogPort(String adbDialogPort) {
		settingsData.setAdbDialogPort(adbDialogPort);
	}

	public CommentsLevel getCommentsLevel() {
		return settingsData.getCommentsLevel();
	}

	public void setCommentsLevel(CommentsLevel level) {
		settingsData.setCommentsLevel(level);
	}

	public int getTypeUpdatesLimitCount() {
		return settingsData.getTypeUpdatesLimitCount();
	}

	public void setTypeUpdatesLimitCount(int typeUpdatesLimitCount) {
		settingsData.setTypeUpdatesLimitCount(typeUpdatesLimitCount);
	}

	public LineNumbersMode getLineNumbersMode() {
		return settingsData.getLineNumbersMode();
	}

	public void setLineNumbersMode(LineNumbersMode lineNumbersMode) {
		settingsData.setLineNumbersMode(lineNumbersMode);
	}

	public CodeCacheMode getCodeCacheMode() {
		return settingsData.getCodeCacheMode();
	}

	public void setCodeCacheMode(CodeCacheMode codeCacheMode) {
		settingsData.setCodeCacheMode(codeCacheMode);
	}

	public UsageCacheMode getUsageCacheMode() {
		return settingsData.getUsageCacheMode();
	}

	public void setUsageCacheMode(UsageCacheMode usageCacheMode) {
		settingsData.setUsageCacheMode(usageCacheMode);
	}

	public @Nullable String getCacheDir() {
		return settingsData.getCacheDir();
	}

	public void setCacheDir(@Nullable String cacheDir) {
		settingsData.setCacheDir(cacheDir);
	}

	public boolean isJumpOnDoubleClick() {
		return settingsData.isJumpOnDoubleClick();
	}

	public void setJumpOnDoubleClick(boolean jumpOnDoubleClick) {
		settingsData.setJumpOnDoubleClick(jumpOnDoubleClick);
	}

	public boolean isDockLogViewer() {
		return settingsData.isDockLogViewer();
	}

	public void saveDockLogViewer(boolean dockLogViewer) {
		settingsData.setDockLogViewer(dockLogViewer);
		sync();
	}

	public boolean isDockQuickTabs() {
		return settingsData.isDockQuickTabs();
	}

	public void saveDockQuickTabs(boolean dockQuickTabs) {
		settingsData.setDockQuickTabs(dockQuickTabs);
		sync();
	}

	public XposedCodegenLanguage getXposedCodegenLanguage() {
		return settingsData.getXposedCodegenLanguage();
	}

	public void setXposedCodegenLanguage(XposedCodegenLanguage language) {
		settingsData.setXposedCodegenLanguage(language);
	}

	public JadxUpdateChannel getJadxUpdateChannel() {
		return settingsData.getJadxUpdateChannel();
	}

	public void setJadxUpdateChannel(JadxUpdateChannel channel) {
		settingsData.setJadxUpdateChannel(channel);
	}

	public TabDndGhostType getTabDndGhostType() {
		return settingsData.getTabDndGhostType();
	}

	public void setTabDndGhostType(TabDndGhostType tabDndGhostType) {
		settingsData.setTabDndGhostType(tabDndGhostType);
	}

	public boolean isRestoreSwitchOverString() {
		return settingsData.isRestoreSwitchOverString();
	}

	public void setRestoreSwitchOverString(boolean restoreSwitchOverString) {
		settingsData.setRestoreSwitchOverString(restoreSwitchOverString);
	}

	public boolean isRenamePrintable() {
		return settingsData.isRenamePrintable();
	}

	public UserRenamesMappingsMode getUserRenamesMappingsMode() {
		return settingsData.getUserRenamesMappingsMode();
	}

	public void setUserRenamesMappingsMode(UserRenamesMappingsMode userRenamesMappingsMode) {
		settingsData.setUserRenamesMappingsMode(userRenamesMappingsMode);
	}

	public boolean isInlineAnonymousClasses() {
		return settingsData.isInlineAnonymousClasses();
	}

	public void setInlineAnonymousClasses(boolean inlineAnonymousClasses) {
		settingsData.setInlineAnonymousClasses(inlineAnonymousClasses);
	}

	public boolean isRespectBytecodeAccessModifiers() {
		return settingsData.isRespectBytecodeAccessModifiers();
	}

	public void setRespectBytecodeAccessModifiers(boolean respectBytecodeAccessModifiers) {
		settingsData.setRespectBytecodeAccessModifiers(respectBytecodeAccessModifiers);
	}

	public boolean isRenameCaseSensitive() {
		return settingsData.isRenameCaseSensitive();
	}

	public DecompilationMode getDecompilationMode() {
		return settingsData.getDecompilationMode();
	}

	public void setDecompilationMode(DecompilationMode decompilationMode) {
		settingsData.setDecompilationMode(decompilationMode);
	}

	public boolean isInlineMethods() {
		return settingsData.isInlineMethods();
	}

	public void setInlineMethods(boolean inlineMethods) {
		settingsData.setInlineMethods(inlineMethods);
	}

	public boolean isFsCaseSensitive() {
		return settingsData.isFsCaseSensitive();
	}

	public void setFsCaseSensitive(boolean fsCaseSensitive) {
		settingsData.setFsCaseSensitive(fsCaseSensitive);
	}

	public boolean isExtractFinally() {
		return settingsData.isExtractFinally();
	}

	public void setExtractFinally(boolean extractFinally) {
		settingsData.setExtractFinally(extractFinally);
	}

	public int getSourceNameRepeatLimit() {
		return settingsData.getSourceNameRepeatLimit();
	}

	public void setSourceNameRepeatLimit(int sourceNameRepeatLimit) {
		settingsData.setSourceNameRepeatLimit(sourceNameRepeatLimit);
	}

	public boolean isRenameValid() {
		return settingsData.isRenameValid();
	}

	public boolean isSkipXmlPrettyPrint() {
		return settingsData.isSkipXmlPrettyPrint();
	}

	public void setSkipXmlPrettyPrint(boolean skipXmlPrettyPrint) {
		settingsData.setSkipXmlPrettyPrint(skipXmlPrettyPrint);
	}

	public UseSourceNameAsClassNameAlias getUseSourceNameAsClassNameAlias() {
		return settingsData.getUseSourceNameAsClassNameAlias();
	}

	public void setUseSourceNameAsClassNameAlias(UseSourceNameAsClassNameAlias useSourceNameAsClassNameAlias) {
		settingsData.setUseSourceNameAsClassNameAlias(useSourceNameAsClassNameAlias);
	}

	public boolean isShowInconsistentCode() {
		return settingsData.isShowInconsistentCode();
	}

	public void setShowInconsistentCode(boolean showInconsistentCode) {
		settingsData.setShowInconsistentCode(showInconsistentCode);
	}

	public boolean isCfgOutput() {
		return settingsData.isCfgOutput();
	}

	public void setCfgOutput(boolean cfgOutput) {
		settingsData.setCfgOutput(cfgOutput);
	}

	public boolean isEscapeUnicode() {
		return settingsData.isEscapeUnicode();
	}

	public void setEscapeUnicode(boolean escapeUnicode) {
		settingsData.setEscapeUnicode(escapeUnicode);
	}

	public JadxArgs.UseKotlinMethodsForVarNames getUseKotlinMethodsForVarNames() {
		return settingsData.getUseKotlinMethodsForVarNames();
	}

	public void setUseKotlinMethodsForVarNames(JadxArgs.UseKotlinMethodsForVarNames useKotlinMethodsForVarNames) {
		settingsData.setUseKotlinMethodsForVarNames(useKotlinMethodsForVarNames);
	}

	public String getDeobfuscationWhitelistStr() {
		return settingsData.getDeobfuscationWhitelistStr();
	}

	public void setDeobfuscationWhitelistStr(String deobfuscationWhitelistStr) {
		settingsData.setDeobfuscationWhitelistStr(deobfuscationWhitelistStr);
	}

	public String getGeneratedRenamesMappingFile() {
		return settingsData.getGeneratedRenamesMappingFile();
	}

	public boolean isRawCfgOutput() {
		return settingsData.isRawCfgOutput();
	}

	public void setRawCfgOutput(boolean rawCfgOutput) {
		settingsData.setRawCfgOutput(rawCfgOutput);
	}

	public boolean isMoveInnerClasses() {
		return settingsData.isMoveInnerClasses();
	}

	public void setMoveInnerClasses(boolean moveInnerClasses) {
		settingsData.setMoveInnerClasses(moveInnerClasses);
	}

	public boolean isUseDx() {
		return settingsData.isUseDx();
	}

	public void setUseDx(boolean useDx) {
		settingsData.setUseDx(useDx);
	}

	public boolean isAddDebugLines() {
		return settingsData.isAddDebugLines();
	}

	public boolean isUseHeadersForDetectResourceExtensions() {
		return settingsData.isUseHeadersForDetectResourceExtensions();
	}

	public void setUseHeadersForDetectResourceExtensions(boolean useHeadersForDetectResourceExtensions) {
		settingsData.setUseHeadersForDetectResourceExtensions(useHeadersForDetectResourceExtensions);
	}

	public Map<String, String> getPluginOptions() {
		return settingsData.getPluginOptions();
	}

	public boolean isDeobfuscationOn() {
		return settingsData.isDeobfuscationOn();
	}

	public void setDeobfuscationOn(boolean deobfuscationOn) {
		settingsData.setDeobfuscationOn(deobfuscationOn);
	}

	public boolean isReplaceConsts() {
		return settingsData.isReplaceConsts();
	}

	public void setReplaceConsts(boolean replaceConsts) {
		settingsData.setReplaceConsts(replaceConsts);
	}

	public boolean isAllowInlineKotlinLambda() {
		return settingsData.isAllowInlineKotlinLambda();
	}

	public void setAllowInlineKotlinLambda(boolean allowInlineKotlinLambda) {
		settingsData.setAllowInlineKotlinLambda(allowInlineKotlinLambda);
	}

	public void setDeobfuscationUseSourceNameAsAlias(Boolean deobfuscationUseSourceNameAsAlias) {
		settingsData.setDeobfuscationUseSourceNameAsAlias(deobfuscationUseSourceNameAsAlias);
	}

	public void setRenameFlags(Set<JadxArgs.RenameEnum> renameFlags) {
		settingsData.setRenameFlags(renameFlags);
	}

	public void updateRenameFlag(JadxArgs.RenameEnum flag, boolean enabled) {
		if (enabled) {
			settingsData.getRenameFlags().add(flag);
		} else {
			settingsData.getRenameFlags().remove(flag);
		}
	}

	public void setUserRenamesMappingsPath(Path userRenamesMappingsPath) {
		settingsData.setUserRenamesMappingsPath(userRenamesMappingsPath);
	}

	public boolean isSkipSources() {
		return settingsData.isSkipSources();
	}

	public boolean isDebugInfo() {
		return settingsData.isDebugInfo();
	}

	public void setDebugInfo(boolean debugInfo) {
		settingsData.setDebugInfo(debugInfo);
	}

	public boolean isSkipResources() {
		return settingsData.isSkipResources();
	}

	public void setSkipResources(boolean skipResources) {
		settingsData.setSkipResources(skipResources);
	}

	public ResourceNameSource getResourceNameSource() {
		return settingsData.getResourceNameSource();
	}

	public void setResourceNameSource(ResourceNameSource resourceNameSource) {
		settingsData.setResourceNameSource(resourceNameSource);
	}

	public IntegerFormat getIntegerFormat() {
		return settingsData.getIntegerFormat();
	}

	public void setIntegerFormat(IntegerFormat format) {
		settingsData.setIntegerFormat(format);
	}

	public boolean isFallbackMode() {
		return settingsData.isFallbackMode();
	}

	public boolean isUseImports() {
		return settingsData.isUseImports();
	}

	public void setUseImports(boolean useImports) {
		settingsData.setUseImports(useImports);
	}

	public int getDeobfuscationMinLength() {
		return settingsData.getDeobfuscationMinLength();
	}

	public void setDeobfuscationMinLength(int deobfuscationMinLength) {
		settingsData.setDeobfuscationMinLength(deobfuscationMinLength);
	}

	public GeneratedRenamesMappingFileMode getGeneratedRenamesMappingFileMode() {
		return settingsData.getGeneratedRenamesMappingFileMode();
	}

	public void setGeneratedRenamesMappingFileMode(GeneratedRenamesMappingFileMode generatedRenamesMappingFileMode) {
		settingsData.setGeneratedRenamesMappingFileMode(generatedRenamesMappingFileMode);
	}

	public int getDeobfuscationMaxLength() {
		return settingsData.getDeobfuscationMaxLength();
	}

	public void setDeobfuscationMaxLength(int deobfuscationMaxLength) {
		settingsData.setDeobfuscationMaxLength(deobfuscationMaxLength);
	}

	public int getThreadsCount() {
		return settingsData.getThreadsCount();
	}

	public void setThreadsCount(int threadsCount) {
		settingsData.setThreadsCount(threadsCount);
	}

	public SaveOptionEnum getSaveOption() {
		return settingsData.getSaveOption();
	}

	public void setSaveOption(SaveOptionEnum saveOption) {
		settingsData.setSaveOption(saveOption);
	}

	public boolean isSmaliAreaShowBytecode() {
		return settingsData.isSmaliAreaShowBytecode();
	}

	public void setSmaliAreaShowBytecode(boolean smaliAreaShowBytecode) {
		settingsData.setSmaliAreaShowBytecode(smaliAreaShowBytecode);
	}

}
