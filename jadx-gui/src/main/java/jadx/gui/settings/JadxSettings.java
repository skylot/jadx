package jadx.gui.settings;

import jadx.cli.JadxCLIArgs;

import javax.swing.JLabel;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JadxSettings extends JadxCLIArgs {

	private static final String USER_HOME = System.getProperty("user.home");
	private static final int RECENT_FILES_COUNT = 15;

	private static final Font DEFAULT_FONT = new JLabel().getFont();

	static final Set<String> SKIP_FIELDS = new HashSet<String>(Arrays.asList(
			"files", "input", "outputDir", "verbose", "printHelp"
	));

	private String lastOpenFilePath = USER_HOME;
	private String lastSaveFilePath = USER_HOME;
	private boolean flattenPackage = false;
	private boolean checkForUpdates = true;
	private List<String> recentFiles = new ArrayList<String>();
	private String fontStr = "";

	public void sync() {
		JadxSettingsAdapter.store(this);
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
		if (recentFiles.contains(filePath)) {
			return;
		}
		recentFiles.add(filePath);
		int count = recentFiles.size();
		if (count > RECENT_FILES_COUNT) {
			recentFiles.subList(0, count - RECENT_FILES_COUNT).clear();
		}
		sync();
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
}
