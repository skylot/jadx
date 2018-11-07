package jadx.gui.settings;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxArgs;
import jadx.cli.JadxCLIArgs;
import jadx.gui.ui.codearea.EditorTheme;
import jadx.gui.utils.LangLocale;
import jadx.gui.utils.NLS;

import static jadx.gui.utils.Utils.FONT_HACK;

public class JadxSettings extends JadxCLIArgs {
	private static final Logger LOG = LoggerFactory.getLogger(JadxSettings.class);

	private static final String USER_HOME = System.getProperty("user.home");
	private static final int RECENT_FILES_COUNT = 15;
	private static final int CURRENT_SETTINGS_VERSION = 5;

	private static final Font DEFAULT_FONT = FONT_HACK != null ? FONT_HACK : new RSyntaxTextArea().getFont();

	static final Set<String> SKIP_FIELDS = new HashSet<>(Arrays.asList(
			"files", "input", "outputDir", "verbose", "printHelp"
	));
	private String lastOpenFilePath = USER_HOME;
	private String lastSaveFilePath = USER_HOME;
	private boolean flattenPackage = false;
	private boolean checkForUpdates = false;
	private List<String> recentFiles = new ArrayList<>();
	private String fontStr = "";
	private String editorThemePath = "";
	private LangLocale langLocale = NLS.defaultLocale();
	private boolean autoStartJobs = false;

	private int settingsVersion = 0;

	private Map<String, WindowLocation> windowPos = new HashMap<>();

	public static JadxSettings makeDefault() {
		JadxSettings jadxSettings = new JadxSettings();
		jadxSettings.fixOnLoad();
		return jadxSettings;
	}

	public void sync() {
		JadxSettingsAdapter.store(this);
	}

	public void partialSync(ISettingsUpdater updater) {
		JadxSettings settings = JadxSettingsAdapter.load();
		updater.update(settings);
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

	public String getLastOpenFilePath() {
		return lastOpenFilePath;
	}

	public void setLastOpenFilePath(String lastOpenFilePath) {
		this.lastOpenFilePath = lastOpenFilePath;
		partialSync(settings -> settings.lastOpenFilePath = JadxSettings.this.lastOpenFilePath);
	}

	public String getLastSaveFilePath() {
		return lastSaveFilePath;
	}

	public void setLastSaveFilePath(String lastSaveFilePath) {
		this.lastSaveFilePath = lastSaveFilePath;
		partialSync(settings -> settings.lastSaveFilePath = JadxSettings.this.lastSaveFilePath);
	}

	public boolean isFlattenPackage() {
		return flattenPackage;
	}

	public void setFlattenPackage(boolean flattenPackage) {
		this.flattenPackage = flattenPackage;
		partialSync(settings -> settings.flattenPackage = JadxSettings.this.flattenPackage);
	}

	public boolean isCheckForUpdates() {
		return checkForUpdates;
	}

	public void setCheckForUpdates(boolean checkForUpdates) {
		this.checkForUpdates = checkForUpdates;
		sync();
	}

	public Iterable<String> getRecentFiles() {
		return recentFiles;
	}

	public void addRecentFile(String filePath) {
		recentFiles.remove(filePath);
		recentFiles.add(0, filePath);
		int count = recentFiles.size();
		if (count > RECENT_FILES_COUNT) {
			recentFiles.subList(RECENT_FILES_COUNT, count).clear();
		}
		partialSync(settings -> settings.recentFiles = recentFiles);
	}

	public void saveWindowPos(Window window) {
		WindowLocation pos = new WindowLocation(window.getClass().getSimpleName(),
				window.getX(), window.getY(),
				window.getWidth(), window.getHeight()
		);
		windowPos.put(pos.getWindowId(), pos);
		partialSync(settings -> settings.windowPos = windowPos);
	}

	public boolean loadWindowPos(Window window) {
		WindowLocation pos = windowPos.get(window.getClass().getSimpleName());
		if (pos == null) {
			return false;
		}
		window.setLocation(pos.getX(), pos.getY());
		window.setSize(pos.getWidth(), pos.getHeight());
		return true;
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

	public void setEscapeUnicode(boolean escapeUnicode) {
		this.escapeUnicode = escapeUnicode;
	}

	public void setReplaceConsts(boolean replaceConsts) {
		this.replaceConsts = replaceConsts;
	}

	public void setUseImports(boolean useImports) {
		this.useImports = useImports;
	}

	public boolean isAutoStartJobs() {
		return autoStartJobs;
	}

	public void setAutoStartJobs(boolean autoStartJobs) {
		this.autoStartJobs = autoStartJobs;
	}

	public void setExportAsGradleProject(boolean exportAsGradleProject) {
		this.exportAsGradleProject = exportAsGradleProject;
	}

	public Font getFont() {
		if (fontStr.isEmpty()) {
			return DEFAULT_FONT;
		}
		return Font.decode(fontStr);
	}

	public void setFont(Font font) {
		this.fontStr = font.getFontName() + addStyleName(font.getStyle()) + "-" + font.getSize();
	}

	public String getEditorThemePath() {
		return editorThemePath;
	}

	public void setEditorThemePath(String editorThemePath) {
		this.editorThemePath = editorThemePath;
	}

	private static String addStyleName(int style) {
		switch (style) {
			case Font.BOLD:
				return "-BOLD";
			case Font.PLAIN:
				return "-PLAIN";
			case Font.ITALIC:
				return "-ITALIC";
			default:
				return "";
		}
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
		}
		settingsVersion = CURRENT_SETTINGS_VERSION;
		sync();
	}
}
