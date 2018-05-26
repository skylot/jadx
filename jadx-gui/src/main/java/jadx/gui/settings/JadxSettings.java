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

public class JadxSettings extends JadxCLIArgs {
	private static final Logger LOG = LoggerFactory.getLogger(JadxSettings.class);

	private static final String USER_HOME = System.getProperty("user.home");
	private static final int RECENT_FILES_COUNT = 15;
	private static final int CURRENT_SETTINGS_VERSION = 1;

	private static final Font DEFAULT_FONT = new RSyntaxTextArea().getFont();

	static final Set<String> SKIP_FIELDS = new HashSet<>(Arrays.asList(
			"files", "input", "outputDir", "verbose", "printHelp"
	));
	private String lastOpenFilePath = USER_HOME;
	private String lastSaveFilePath = USER_HOME;
	private boolean flattenPackage = false;
	private boolean checkForUpdates = false;
	private List<String> recentFiles = new ArrayList<>();
	private String fontStr = "";
	private boolean autoStartJobs = false;

	private int settingsVersion = 0;

	private Map<String, WindowLocation> windowPos = new HashMap<>();

	public JadxSettings() {
	}

	public void sync() {
		JadxSettingsAdapter.store(this);
	}

	public void fixOnLoad() {
		if (threadsCount <= 0) {
			threadsCount = JadxArgs.DEFAULT_THREADS_COUNT;
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
		sync();
	}

	public String getLastSaveFilePath() {
		return lastSaveFilePath;
	}

	public void setLastSaveFilePath(String lastSaveFilePath) {
		this.lastSaveFilePath = lastSaveFilePath;
		sync();
	}

	public boolean isFlattenPackage() {
		return flattenPackage;
	}

	public void setFlattenPackage(boolean flattenPackage) {
		this.flattenPackage = flattenPackage;
		sync();
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
			recentFiles.subList(0, count - RECENT_FILES_COUNT).clear();
		}
		sync();
	}

	public void saveWindowPos(Window window) {
		WindowLocation pos = new WindowLocation(window.getClass().getSimpleName(),
				window.getX(), window.getY(),
				window.getWidth(), window.getHeight()
		);
		windowPos.put(pos.getWindowId(), pos);
		sync();
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
			setDeobfuscationMinLength(4);
			setDeobfuscationUseSourceNameAsAlias(true);
			setDeobfuscationForceSave(true);
			setThreadsCount(1);
			setReplaceConsts(true);
			setSkipResources(false);
			setAutoStartJobs(false);
//			fromVersion++;
		}
		settingsVersion = CURRENT_SETTINGS_VERSION;
		sync();
	}
}
