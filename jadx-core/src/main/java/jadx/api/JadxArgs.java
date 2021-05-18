package jadx.api;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import jadx.api.data.ICodeData;
import jadx.api.impl.AnnotatedCodeWriter;
import jadx.api.impl.InMemoryCodeCache;

public class JadxArgs {

	public static final int DEFAULT_THREADS_COUNT = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);

	public static final String DEFAULT_OUT_DIR = "jadx-output";
	public static final String DEFAULT_SRC_DIR = "sources";
	public static final String DEFAULT_RES_DIR = "resources";

	private List<File> inputFiles = new ArrayList<>(1);

	private File outDir;
	private File outDirSrc;
	private File outDirRes;

	private ICodeCache codeCache = new InMemoryCodeCache();
	private Function<JadxArgs, ICodeWriter> codeWriterProvider = AnnotatedCodeWriter::new;

	private int threadsCount = DEFAULT_THREADS_COUNT;

	private boolean cfgOutput = false;
	private boolean rawCFGOutput = false;

	private boolean fallbackMode = false;
	private boolean showInconsistentCode = false;

	private boolean useImports = true;
	private boolean debugInfo = true;
	private boolean insertDebugLines = false;
	private boolean inlineAnonymousClasses = true;
	private boolean inlineMethods = true;

	private boolean skipResources = false;
	private boolean skipSources = false;

	/**
	 * Predicate that allows to filter the classes to be process based on their full name
	 */
	private Predicate<String> classFilter = null;

	private boolean deobfuscationOn = false;
	private boolean deobfuscationForceSave = false;
	private boolean useSourceNameAsClassAlias = false;
	private boolean parseKotlinMetadata = false;
	private File deobfuscationMapFile = null;

	private int deobfuscationMinLength = 0;
	private int deobfuscationMaxLength = Integer.MAX_VALUE;

	private boolean escapeUnicode = false;
	private boolean replaceConsts = true;
	private boolean respectBytecodeAccModifiers = false;
	private boolean exportAsGradleProject = false;

	private boolean fsCaseSensitive;

	public enum RenameEnum {
		CASE, VALID, PRINTABLE
	}

	private Set<RenameEnum> renameFlags = EnumSet.allOf(RenameEnum.class);

	public enum OutputFormatEnum {
		JAVA, JSON
	}

	private OutputFormatEnum outputFormat = OutputFormatEnum.JAVA;

	private ICodeData codeData;

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

	public void setInputFile(File inputFile) {
		this.inputFiles = Collections.singletonList(inputFile);
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

	public boolean isDebugInfo() {
		return debugInfo;
	}

	public void setDebugInfo(boolean debugInfo) {
		this.debugInfo = debugInfo;
	}

	public boolean isInsertDebugLines() {
		return insertDebugLines;
	}

	public void setInsertDebugLines(boolean insertDebugLines) {
		this.insertDebugLines = insertDebugLines;
	}

	public boolean isInlineAnonymousClasses() {
		return inlineAnonymousClasses;
	}

	public void setInlineAnonymousClasses(boolean inlineAnonymousClasses) {
		this.inlineAnonymousClasses = inlineAnonymousClasses;
	}

	public boolean isInlineMethods() {
		return inlineMethods;
	}

	public void setInlineMethods(boolean inlineMethods) {
		this.inlineMethods = inlineMethods;
	}

	public boolean isSkipResources() {
		return skipResources;
	}

	public void setSkipResources(boolean skipResources) {
		this.skipResources = skipResources;
	}

	public boolean isSkipSources() {
		return skipSources;
	}

	public void setSkipSources(boolean skipSources) {
		this.skipSources = skipSources;
	}

	public Predicate<String> getClassFilter() {
		return classFilter;
	}

	public void setClassFilter(Predicate<String> classFilter) {
		this.classFilter = classFilter;
	}

	public boolean isDeobfuscationOn() {
		return deobfuscationOn;
	}

	public void setDeobfuscationOn(boolean deobfuscationOn) {
		this.deobfuscationOn = deobfuscationOn;
	}

	public boolean isDeobfuscationForceSave() {
		return deobfuscationForceSave;
	}

	public void setDeobfuscationForceSave(boolean deobfuscationForceSave) {
		this.deobfuscationForceSave = deobfuscationForceSave;
	}

	public boolean isUseSourceNameAsClassAlias() {
		return useSourceNameAsClassAlias;
	}

	public void setUseSourceNameAsClassAlias(boolean useSourceNameAsClassAlias) {
		this.useSourceNameAsClassAlias = useSourceNameAsClassAlias;
	}

	public boolean isParseKotlinMetadata() {
		return parseKotlinMetadata;
	}

	public void setParseKotlinMetadata(boolean parseKotlinMetadata) {
		this.parseKotlinMetadata = parseKotlinMetadata;
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

	public File getDeobfuscationMapFile() {
		return deobfuscationMapFile;
	}

	public void setDeobfuscationMapFile(File deobfuscationMapFile) {
		this.deobfuscationMapFile = deobfuscationMapFile;
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

	public boolean isRespectBytecodeAccModifiers() {
		return respectBytecodeAccModifiers;
	}

	public void setRespectBytecodeAccModifiers(boolean respectBytecodeAccModifiers) {
		this.respectBytecodeAccModifiers = respectBytecodeAccModifiers;
	}

	public boolean isExportAsGradleProject() {
		return exportAsGradleProject;
	}

	public void setExportAsGradleProject(boolean exportAsGradleProject) {
		this.exportAsGradleProject = exportAsGradleProject;
	}

	public boolean isFsCaseSensitive() {
		return fsCaseSensitive;
	}

	public void setFsCaseSensitive(boolean fsCaseSensitive) {
		this.fsCaseSensitive = fsCaseSensitive;
	}

	public boolean isRenameCaseSensitive() {
		return renameFlags.contains(RenameEnum.CASE);
	}

	public void setRenameCaseSensitive(boolean renameCaseSensitive) {
		updateRenameFlag(renameCaseSensitive, RenameEnum.CASE);
	}

	public boolean isRenameValid() {
		return renameFlags.contains(RenameEnum.VALID);
	}

	public void setRenameValid(boolean renameValid) {
		updateRenameFlag(renameValid, RenameEnum.VALID);
	}

	public boolean isRenamePrintable() {
		return renameFlags.contains(RenameEnum.PRINTABLE);
	}

	public void setRenamePrintable(boolean renamePrintable) {
		updateRenameFlag(renamePrintable, RenameEnum.PRINTABLE);
	}

	private void updateRenameFlag(boolean enabled, RenameEnum flag) {
		if (enabled) {
			renameFlags.add(flag);
		} else {
			renameFlags.remove(flag);
		}
	}

	public void setRenameFlags(Set<RenameEnum> renameFlags) {
		this.renameFlags = renameFlags;
	}

	public Set<RenameEnum> getRenameFlags() {
		return renameFlags;
	}

	public OutputFormatEnum getOutputFormat() {
		return outputFormat;
	}

	public boolean isJsonOutput() {
		return outputFormat == OutputFormatEnum.JSON;
	}

	public void setOutputFormat(OutputFormatEnum outputFormat) {
		this.outputFormat = outputFormat;
	}

	public ICodeCache getCodeCache() {
		return codeCache;
	}

	public void setCodeCache(ICodeCache codeCache) {
		this.codeCache = codeCache;
	}

	public Function<JadxArgs, ICodeWriter> getCodeWriterProvider() {
		return codeWriterProvider;
	}

	public void setCodeWriterProvider(Function<JadxArgs, ICodeWriter> codeWriterProvider) {
		this.codeWriterProvider = codeWriterProvider;
	}

	public ICodeData getCodeData() {
		return codeData;
	}

	public void setCodeData(ICodeData codeData) {
		this.codeData = codeData;
	}

	@Override
	public String toString() {
		return "JadxArgs{" + "inputFiles=" + inputFiles
				+ ", outDir=" + outDir
				+ ", outDirSrc=" + outDirSrc
				+ ", outDirRes=" + outDirRes
				+ ", threadsCount=" + threadsCount
				+ ", cfgOutput=" + cfgOutput
				+ ", rawCFGOutput=" + rawCFGOutput
				+ ", fallbackMode=" + fallbackMode
				+ ", showInconsistentCode=" + showInconsistentCode
				+ ", useImports=" + useImports
				+ ", skipResources=" + skipResources
				+ ", skipSources=" + skipSources
				+ ", deobfuscationOn=" + deobfuscationOn
				+ ", deobfuscationMapFile=" + deobfuscationMapFile
				+ ", deobfuscationForceSave=" + deobfuscationForceSave
				+ ", useSourceNameAsClassAlias=" + useSourceNameAsClassAlias
				+ ", parseKotlinMetadata=" + parseKotlinMetadata
				+ ", deobfuscationMinLength=" + deobfuscationMinLength
				+ ", deobfuscationMaxLength=" + deobfuscationMaxLength
				+ ", escapeUnicode=" + escapeUnicode
				+ ", replaceConsts=" + replaceConsts
				+ ", respectBytecodeAccModifiers=" + respectBytecodeAccModifiers
				+ ", exportAsGradleProject=" + exportAsGradleProject
				+ ", fsCaseSensitive=" + fsCaseSensitive
				+ ", renameFlags=" + renameFlags
				+ ", outputFormat=" + outputFormat
				+ ", codeCache=" + codeCache
				+ ", codeWriter=" + codeWriterProvider.apply(this).getClass().getSimpleName()
				+ '}';
	}
}
