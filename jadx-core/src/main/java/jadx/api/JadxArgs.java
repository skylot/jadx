package jadx.api;

import java.io.File;

public class JadxArgs implements IJadxArgs {

	private File outDir = new File("jadx-output");
	private int threadsCount = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);

	private boolean cfgOutput = false;
	private boolean rawCFGOutput = false;

	private boolean isVerbose = false;
	private boolean fallbackMode = false;
	private boolean showInconsistentCode = false;

	private boolean isSkipResources = false;
	private boolean isSkipSources = false;

	private boolean isDeobfuscationOn = false;
	private boolean isDeobfuscationForceSave = false;
	private boolean useSourceNameAsClassAlias = false;

	private int deobfuscationMinLength = 0;
	private int deobfuscationMaxLength = Integer.MAX_VALUE;

	@Override
	public File getOutDir() {
		return outDir;
	}

	public void setOutDir(File outDir) {
		this.outDir = outDir;
	}

	@Override
	public int getThreadsCount() {
		return threadsCount;
	}

	public void setThreadsCount(int threadsCount) {
		this.threadsCount = threadsCount;
	}

	@Override
	public boolean isCFGOutput() {
		return cfgOutput;
	}

	public void setCfgOutput(boolean cfgOutput) {
		this.cfgOutput = cfgOutput;
	}

	@Override
	public boolean isRawCFGOutput() {
		return rawCFGOutput;
	}

	public void setRawCFGOutput(boolean rawCFGOutput) {
		this.rawCFGOutput = rawCFGOutput;
	}

	@Override
	public boolean isFallbackMode() {
		return fallbackMode;
	}

	public void setFallbackMode(boolean fallbackMode) {
		this.fallbackMode = fallbackMode;
	}

	@Override
	public boolean isShowInconsistentCode() {
		return showInconsistentCode;
	}

	public void setShowInconsistentCode(boolean showInconsistentCode) {
		this.showInconsistentCode = showInconsistentCode;
	}

	@Override
	public boolean isVerbose() {
		return isVerbose;
	}

	public void setVerbose(boolean verbose) {
		isVerbose = verbose;
	}

	@Override
	public boolean isSkipResources() {
		return isSkipResources;
	}

	public void setSkipResources(boolean skipResources) {
		isSkipResources = skipResources;
	}

	@Override
	public boolean isSkipSources() {
		return isSkipSources;
	}

	public void setSkipSources(boolean skipSources) {
		isSkipSources = skipSources;
	}

	@Override
	public boolean isDeobfuscationOn() {
		return isDeobfuscationOn;
	}

	public void setDeobfuscationOn(boolean deobfuscationOn) {
		isDeobfuscationOn = deobfuscationOn;
	}

	@Override
	public boolean isDeobfuscationForceSave() {
		return isDeobfuscationForceSave;
	}

	public void setDeobfuscationForceSave(boolean deobfuscationForceSave) {
		isDeobfuscationForceSave = deobfuscationForceSave;
	}

	@Override
	public boolean useSourceNameAsClassAlias() {
		return useSourceNameAsClassAlias;
	}

	public void setUseSourceNameAsClassAlias(boolean useSourceNameAsClassAlias) {
		this.useSourceNameAsClassAlias = useSourceNameAsClassAlias;
	}

	@Override
	public int getDeobfuscationMinLength() {
		return deobfuscationMinLength;
	}

	public void setDeobfuscationMinLength(int deobfuscationMinLength) {
		this.deobfuscationMinLength = deobfuscationMinLength;
	}

	@Override
	public int getDeobfuscationMaxLength() {
		return deobfuscationMaxLength;
	}

	public void setDeobfuscationMaxLength(int deobfuscationMaxLength) {
		this.deobfuscationMaxLength = deobfuscationMaxLength;
	}
}
