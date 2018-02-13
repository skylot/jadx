package jadx.api;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JadxArgs {

	public static final int DEFAULT_THREADS_COUNT = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);

	public static final String DEFAULT_OUT_DIR = "jadx-output";
	public static final String DEFAULT_SRC_DIR = "sources";
	public static final String DEFAULT_RES_DIR = "resources";

	private List<File> inputFiles = new ArrayList<>(1);

	private File outDir;
	private File outDirSrc;
	private File outDirRes;

	private int threadsCount = DEFAULT_THREADS_COUNT;

	private boolean cfgOutput = false;
	private boolean rawCFGOutput = false;

	private boolean fallbackMode = false;
	private boolean showInconsistentCode = false;

	private boolean useImports = true;

	private boolean isSkipResources = false;
	private boolean isSkipSources = false;

	private boolean isDeobfuscationOn = false;
	private boolean isDeobfuscationForceSave = false;
	private boolean useSourceNameAsClassAlias = false;

	private int deobfuscationMinLength = 0;
	private int deobfuscationMaxLength = Integer.MAX_VALUE;

	private boolean escapeUnicode = false;
	private boolean replaceConsts = true;
	private boolean exportAsGradleProject = false;

	public JadxArgs() {
		// use default options
	}

	public void setRootDir(File rootDir) {
		setOutDir(rootDir);
		setOutDirSrc(new File(rootDir, DEFAULT_SRC_DIR));
		setOutDirRes(new File(rootDir, DEFAULT_RES_DIR));
	}

	public List<File> getInputFiles() {
		return inputFiles;
	}

	public void setInputFiles(List<File> inputFiles) {
		this.inputFiles = inputFiles;
	}

	public File getOutDir() {
		return outDir;
	}

	public void setOutDir(File outDir) {
		this.outDir = outDir;
	}

	public File getOutDirSrc() {
		return outDirSrc;
	}

	public void setOutDirSrc(File outDirSrc) {
		this.outDirSrc = outDirSrc;
	}

	public File getOutDirRes() {
		return outDirRes;
	}

	public void setOutDirRes(File outDirRes) {
		this.outDirRes = outDirRes;
	}

	public int getThreadsCount() {
		return threadsCount;
	}

	public void setThreadsCount(int threadsCount) {
		this.threadsCount = threadsCount;
	}

	public boolean isCfgOutput() {
		return cfgOutput;
	}

	public void setCfgOutput(boolean cfgOutput) {
		this.cfgOutput = cfgOutput;
	}

	public boolean isRawCFGOutput() {
		return rawCFGOutput;
	}

	public void setRawCFGOutput(boolean rawCFGOutput) {
		this.rawCFGOutput = rawCFGOutput;
	}

	public boolean isFallbackMode() {
		return fallbackMode;
	}

	public void setFallbackMode(boolean fallbackMode) {
		this.fallbackMode = fallbackMode;
	}

	public boolean isShowInconsistentCode() {
		return showInconsistentCode;
	}

	public void setShowInconsistentCode(boolean showInconsistentCode) {
		this.showInconsistentCode = showInconsistentCode;
	}

	public boolean isUseImports() {
		return useImports;
	}

	public void setUseImports(boolean useImports) {
		this.useImports = useImports;
	}

	public boolean isSkipResources() {
		return isSkipResources;
	}

	public void setSkipResources(boolean skipResources) {
		isSkipResources = skipResources;
	}

	public boolean isSkipSources() {
		return isSkipSources;
	}

	public void setSkipSources(boolean skipSources) {
		isSkipSources = skipSources;
	}

	public boolean isDeobfuscationOn() {
		return isDeobfuscationOn;
	}

	public void setDeobfuscationOn(boolean deobfuscationOn) {
		isDeobfuscationOn = deobfuscationOn;
	}

	public boolean isDeobfuscationForceSave() {
		return isDeobfuscationForceSave;
	}

	public void setDeobfuscationForceSave(boolean deobfuscationForceSave) {
		isDeobfuscationForceSave = deobfuscationForceSave;
	}

	public boolean isUseSourceNameAsClassAlias() {
		return useSourceNameAsClassAlias;
	}

	public void setUseSourceNameAsClassAlias(boolean useSourceNameAsClassAlias) {
		this.useSourceNameAsClassAlias = useSourceNameAsClassAlias;
	}

	public int getDeobfuscationMinLength() {
		return deobfuscationMinLength;
	}

	public void setDeobfuscationMinLength(int deobfuscationMinLength) {
		this.deobfuscationMinLength = deobfuscationMinLength;
	}

	public int getDeobfuscationMaxLength() {
		return deobfuscationMaxLength;
	}

	public void setDeobfuscationMaxLength(int deobfuscationMaxLength) {
		this.deobfuscationMaxLength = deobfuscationMaxLength;
	}

	public boolean isEscapeUnicode() {
		return escapeUnicode;
	}

	public void setEscapeUnicode(boolean escapeUnicode) {
		this.escapeUnicode = escapeUnicode;
	}

	public boolean isReplaceConsts() {
		return replaceConsts;
	}

	public void setReplaceConsts(boolean replaceConsts) {
		this.replaceConsts = replaceConsts;
	}

	public boolean isExportAsGradleProject() {
		return exportAsGradleProject;
	}

	public void setExportAsGradleProject(boolean exportAsGradleProject) {
		this.exportAsGradleProject = exportAsGradleProject;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("JadxArgs{");
		sb.append("inputFiles=").append(inputFiles);
		sb.append(", outDir=").append(outDir);
		sb.append(", outDirSrc=").append(outDirSrc);
		sb.append(", outDirRes=").append(outDirRes);
		sb.append(", threadsCount=").append(threadsCount);
		sb.append(", cfgOutput=").append(cfgOutput);
		sb.append(", rawCFGOutput=").append(rawCFGOutput);
		sb.append(", fallbackMode=").append(fallbackMode);
		sb.append(", showInconsistentCode=").append(showInconsistentCode);
		sb.append(", useImports=").append(useImports);
		sb.append(", isSkipResources=").append(isSkipResources);
		sb.append(", isSkipSources=").append(isSkipSources);
		sb.append(", isDeobfuscationOn=").append(isDeobfuscationOn);
		sb.append(", isDeobfuscationForceSave=").append(isDeobfuscationForceSave);
		sb.append(", useSourceNameAsClassAlias=").append(useSourceNameAsClassAlias);
		sb.append(", deobfuscationMinLength=").append(deobfuscationMinLength);
		sb.append(", deobfuscationMaxLength=").append(deobfuscationMaxLength);
		sb.append(", escapeUnicode=").append(escapeUnicode);
		sb.append(", replaceConsts=").append(replaceConsts);
		sb.append(", exportAsGradleProject=").append(exportAsGradleProject);
		sb.append('}');
		return sb.toString();
	}
}
