package jadx.gui.settings;

import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.*;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;

import jadx.api.JadxArgs;
import jadx.cli.JadxCLIArgs;
import jadx.cli.LogHelper;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.EditorTheme;
import jadx.gui.utils.FontUtils;
import jadx.gui.utils.LangLocale;
import jadx.gui.utils.NLS;

public class JadxSettings extends JadxCLIArgs {
	private static final Logger LOG = LoggerFactory.getLogger(JadxSettings.class);

	private static final Path USER_HOME = Paths.get(System.getProperty("user.home"));
	private static final int RECENT_PROJECTS_COUNT = 15;
	private static final int CURRENT_SETTINGS_VERSION = 9;

	private static final Font DEFAULT_FONT = new RSyntaxTextArea().getFont();

	static final Set<String> SKIP_FIELDS = new HashSet<>(Arrays.asList(
			"files", "input", "outDir", "outDirSrc", "outDirRes", "outputFormat",
			"verbose", "quiet", "logLevel",
			"printVersion", "printHelp"));

	private Path lastSaveProjectPath = USER_HOME;
	private Path lastOpenFilePath = USER_HOME;
	private Path lastSaveFilePath = USER_HOME;
	private boolean flattenPackage = false;
	private boolean checkForUpdates = false;
	private List<Path> recentProjects = new ArrayList<>();
	private String fontStr = "";
	private String editorThemePath = "";
	private LangLocale langLocale = NLS.defaultLocale();
	private boolean autoStartJobs = false;
	protected String excludedPackages = "";
	private boolean autoSaveProject = false;

	private boolean showHeapUsageBar = true;

	private Map<String, WindowLocation> windowPos = new HashMap<>();
	private int mainWindowExtendedState = JFrame.NORMAL;
	/**
	 * UI setting: the width of the tree showing the classes, resources, ...
	 */
	private int treeWidth = 130;

	private int settingsVersion = 0;

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

	public Iterable<Path> getRecentProjects() {
		return recentProjects;
	}

	public void addRecentProject(Path projectPath) {
		recentProjects.remove(projectPath);
		recentProjects.add(0, projectPath);
		int count = recentProjects.size();
		if (count > RECENT_PROJECTS_COUNT) {
			recentProjects.subList(RECENT_PROJECTS_COUNT, count).clear();
		}
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
		if (window instanceof MainWindow) {
			int extendedState = getMainWindowExtendedState();
			if (extendedState != JFrame.NORMAL) {
				((JFrame) window).setExtendedState(extendedState);
				return true;
			}
		}

		if (!isContainedInAnyScreen(pos)) {
			return false;
		}

		window.setBounds(pos.getBounds());
		return true;
	}

	private static boolean isContainedInAnyScreen(WindowLocation pos) {
		for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
			if (gd.getDefaultConfiguration().getBounds().contains(pos.getBounds())) {
				return true;
			}
		}
		return false;
	}

	public boolean isShowHeapUsageBar() {
		return showHeapUsageBar;
	}

	public void setShowHeapUsageBar(boolean showHeapUsageBar) {
		this.showHeapUsageBar = showHeapUsageBar;
		partialSync(settings -> settings.showHeapUsageBar = showHeapUsageBar);
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

	public void setSkipResources(boolean skipResources) {
		this.skipResources = skipResources;
	}

	public void setSkipSources(boolean skipSources) {
		this.skipSources = skipSources;
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

	public void setDeobfuscationOn(boolean deobfuscationOn) {
		this.deobfuscationOn = deobfuscationOn;
	}

	public void setDeobfuscationMinLength(int deobfuscationMinLength) {
		this.deobfuscationMinLength = deobfuscationMinLength;
	}

	public void setDeobfuscationMaxLength(int deobfuscationMaxLength) {
		this.deobfuscationMaxLength = deobfuscationMaxLength;
	}

	public void setDeobfuscationForceSave(boolean deobfuscationForceSave) {
		this.deobfuscationForceSave = deobfuscationForceSave;
	}

	public void setDeobfuscationUseSourceNameAsAlias(boolean deobfuscationUseSourceNameAsAlias) {
		this.deobfuscationUseSourceNameAsAlias = deobfuscationUseSourceNameAsAlias;
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

	public Font getFont() {
		if (fontStr.isEmpty()) {
			return DEFAULT_FONT;
		}
		try {
			return FontUtils.loadByStr(fontStr);
		} catch (Exception e) {
			LOG.warn("Failed to load font: {}, reset to default", fontStr, e);
			setFont(DEFAULT_FONT);
			return DEFAULT_FONT;
		}
	}

	public void setFont(@Nullable Font font) {
		if (font == null) {
			this.fontStr = "";
		} else {
			this.fontStr = FontUtils.convertToStr(font);
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

	public int getMainWindowExtendedState() {
		return mainWindowExtendedState;
	}

	public void setMainWindowExtendedState(int mainWindowExtendedState) {
		this.mainWindowExtendedState = mainWindowExtendedState;
		partialSync(settings -> settings.mainWindowExtendedState = mainWindowExtendedState);
	}

	private void upgradeSettings(int fromVersion) {
		LOG.debug("upgrade settings from version: {} to {}", fromVersion, CURRENT_SETTINGS_VERSION);
		if (fromVersion == 0) {
			setDeobfuscationMinLength(3);
			setDeobfuscationUseSourceNameAsAlias(true);
			setDeobfuscationForceSave(true);
			setThreadsCount(1);
			setReplaceConsts(true);
			setSkipResources(false);
			setAutoStartJobs(false);
			fromVersion++;
		}
		if (fromVersion == 1) {
			setEditorThemePath(EditorTheme.getDefaultTheme().getPath());
			fromVersion++;
		}
		if (fromVersion == 2) {
			if (getDeobfuscationMinLength() == 4) {
				setDeobfuscationMinLength(3);
			}
			fromVersion++;
		}
		if (fromVersion == 3) {
			setLangLocale(NLS.defaultLocale());
			fromVersion++;
		}
		if (fromVersion == 4) {
			setUseImports(true);
			fromVersion++;
		}
		if (fromVersion == 5) {
			setRespectBytecodeAccessModifiers(false);
			fromVersion++;
		}
		if (fromVersion == 6) {
			if (getFont().getFontName().equals("Hack Regular")) {
				setFont(null);
			}
			fromVersion++;
		}
		if (fromVersion == 7) {
			outDir = null;
			outDirSrc = null;
			outDirRes = null;
			fromVersion++;
		}
		if (fromVersion == 8) {
			fromVersion++;
		}
		settingsVersion = CURRENT_SETTINGS_VERSION;
		sync();
	}

	@Override
	protected JadxCLIArgs newInstance() {
		return new JadxSettings();
	}
}
