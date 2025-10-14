package jadx.api;

import java.io.Closeable;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.args.GeneratedRenamesMappingFileMode;
import jadx.api.args.IntegerFormat;
import jadx.api.args.ResourceNameSource;
import jadx.api.args.UseSourceNameAsClassNameAlias;
import jadx.api.args.UserRenamesMappingsMode;
import jadx.api.data.ICodeData;
import jadx.api.deobf.IAliasProvider;
import jadx.api.deobf.IRenameCondition;
import jadx.api.impl.AnnotatedCodeWriter;
import jadx.api.impl.InMemoryCodeCache;
import jadx.api.plugins.loader.JadxBasePluginLoader;
import jadx.api.plugins.loader.JadxPluginLoader;
import jadx.api.security.IJadxSecurity;
import jadx.api.security.JadxSecurityFlag;
import jadx.api.security.impl.JadxSecurity;
import jadx.api.usage.IUsageInfoCache;
import jadx.api.usage.impl.InMemoryUsageInfoCache;
import jadx.core.deobf.DeobfAliasProvider;
import jadx.core.deobf.conditions.DeobfWhitelist;
import jadx.core.deobf.conditions.JadxRenameConditions;
import jadx.core.export.ExportGradleType;
import jadx.core.plugins.PluginContext;
import jadx.core.plugins.files.IJadxFilesGetter;
import jadx.core.plugins.files.TempFilesGetter;
import jadx.core.utils.files.FileUtils;

public class JadxArgs implements Closeable {
	private static final Logger LOG = LoggerFactory.getLogger(JadxArgs.class);

	public static final int DEFAULT_THREADS_COUNT = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);

	public static final String DEFAULT_NEW_LINE_STR = System.lineSeparator();
	public static final String DEFAULT_INDENT_STR = "    ";

	public static final String DEFAULT_OUT_DIR = "jadx-output";
	public static final String DEFAULT_SRC_DIR = "sources";
	public static final String DEFAULT_RES_DIR = "resources";

	private List<File> inputFiles = new ArrayList<>(1);

	private File outDir;
	private File outDirSrc;
	private File outDirRes;

	private ICodeCache codeCache = new InMemoryCodeCache();

	/**
	 * Usage data cache. Saves use places of classes, methods and fields between code reloads.
	 * Can be set to {@link jadx.api.usage.impl.EmptyUsageInfoCache} if code reload not needed.
	 */
	private IUsageInfoCache usageInfoCache = new InMemoryUsageInfoCache();

	private Function<JadxArgs, ICodeWriter> codeWriterProvider = AnnotatedCodeWriter::new;

	private int threadsCount = DEFAULT_THREADS_COUNT;

	private boolean cfgOutput = false;
	private boolean rawCFGOutput = false;

	private boolean showInconsistentCode = false;

	private boolean useImports = true;
	private boolean debugInfo = true;
	private boolean insertDebugLines = false;
	private boolean extractFinally = true;
	private boolean inlineAnonymousClasses = true;
	private boolean inlineMethods = true;
	private boolean allowInlineKotlinLambda = true;
	private boolean moveInnerClasses = true;

	private boolean skipResources = false;
	private boolean skipSources = false;
	private boolean useHeadersForDetectResourceExtensions;

	/**
	 * Predicate that allows to filter the classes to be process based on their full name
	 */
	private Predicate<String> classFilter = null;

	/**
	 * Save dependencies for classes accepted by {@code classFilter}
	 */
	private boolean includeDependencies = false;

	private Path userRenamesMappingsPath = null;
	private UserRenamesMappingsMode userRenamesMappingsMode = UserRenamesMappingsMode.getDefault();

	private boolean deobfuscationOn = false;
	private UseSourceNameAsClassNameAlias useSourceNameAsClassNameAlias = UseSourceNameAsClassNameAlias.getDefault();
	private int sourceNameRepeatLimit = 10;

	private File generatedRenamesMappingFile = null;
	private GeneratedRenamesMappingFileMode generatedRenamesMappingFileMode = GeneratedRenamesMappingFileMode.getDefault();
	private ResourceNameSource resourceNameSource = ResourceNameSource.AUTO;

	private int deobfuscationMinLength = 0;
	private int deobfuscationMaxLength = Integer.MAX_VALUE;

	/**
	 * List of classes and packages (ends with '.*') to exclude from deobfuscation
	 */
	private List<String> deobfuscationWhitelist = DeobfWhitelist.DEFAULT_LIST;

	/**
	 * Nodes alias provider for deobfuscator and rename visitor
	 */
	private IAliasProvider aliasProvider = new DeobfAliasProvider();

	/**
	 * Condition to rename node in deobfuscator
	 */
	private IRenameCondition renameCondition = JadxRenameConditions.buildDefault();

	private boolean escapeUnicode = false;
	private boolean replaceConsts = true;
	private boolean respectBytecodeAccModifiers = false;
	private @Nullable ExportGradleType exportGradleType = null;

	private boolean restoreSwitchOverString = true;

	private boolean skipXmlPrettyPrint = false;

	private boolean fsCaseSensitive;

	public enum RenameEnum {
		CASE, VALID, PRINTABLE
	}

	private Set<RenameEnum> renameFlags = EnumSet.allOf(RenameEnum.class);

	public enum OutputFormatEnum {
		JAVA, JSON
	}

	private OutputFormatEnum outputFormat = OutputFormatEnum.JAVA;

	private DecompilationMode decompilationMode = DecompilationMode.AUTO;

	private ICodeData codeData;

	private String codeNewLineStr = DEFAULT_NEW_LINE_STR;

	private String codeIndentStr = DEFAULT_INDENT_STR;

	private CommentsLevel commentsLevel = CommentsLevel.INFO;

	private IntegerFormat integerFormat = IntegerFormat.AUTO;

	/**
	 * Maximum updates allowed total in method per one instruction.
	 * Should be more or equal 1, default value is 10.
	 */
	private int typeUpdatesLimitCount = 10;

	private boolean useDxInput = false;

	public enum UseKotlinMethodsForVarNames {
		DISABLE, APPLY, APPLY_AND_HIDE
	}

	private UseKotlinMethodsForVarNames useKotlinMethodsForVarNames = UseKotlinMethodsForVarNames.APPLY;

	/**
	 * Additional files structure info.
	 * Defaults to tmp dirs.
	 */
	private IJadxFilesGetter filesGetter = TempFilesGetter.INSTANCE;

	/**
	 * Additional data validation and security checks
	 */
	private IJadxSecurity security = new JadxSecurity(JadxSecurityFlag.all());

	/**
	 * Don't save files (can be using for performance testing)
	 */
	private boolean skipFilesSave = false;

	/**
	 * Run additional expensive checks to verify internal invariants and info integrity
	 */
	private boolean runDebugChecks = false;

	private Map<String, String> pluginOptions = new HashMap<>();

	private Set<String> disabledPlugins = new HashSet<>();

	private JadxPluginLoader pluginLoader = new JadxBasePluginLoader();

	private boolean loadJadxClsSetFile = true;

	public JadxArgs() {
		// use default options
	}

	public void setRootDir(File rootDir) {
		setOutDir(rootDir);
		setOutDirSrc(new File(rootDir, DEFAULT_SRC_DIR));
		setOutDirRes(new File(rootDir, DEFAULT_RES_DIR));
	}

	@Override
	public void close() {
		try {
			inputFiles = null;
			if (codeCache != null) {
				codeCache.close();
			}
			if (usageInfoCache != null) {
				usageInfoCache.close();
			}
			if (pluginLoader != null) {
				pluginLoader.close();
			}
		} catch (Exception e) {
			LOG.error("Failed to close JadxArgs", e);
		} finally {
			codeCache = null;
			usageInfoCache = null;
		}
	}

	public List<File> getInputFiles() {
		return inputFiles;
	}

	public void addInputFile(File inputFile) {
		this.inputFiles.add(inputFile);
	}

	public void setInputFile(File inputFile) {
		addInputFile(inputFile);
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
		this.threadsCount = Math.max(1, threadsCount); // make sure threadsCount >= 1
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
		return decompilationMode == DecompilationMode.FALLBACK;
	}

	/**
	 * Deprecated: use 'decompilation mode' property
	 */
	@Deprecated
	public void setFallbackMode(boolean fallbackMode) {
		if (fallbackMode) {
			this.decompilationMode = DecompilationMode.FALLBACK;
		}
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

	public boolean isAllowInlineKotlinLambda() {
		return allowInlineKotlinLambda;
	}

	public void setAllowInlineKotlinLambda(boolean allowInlineKotlinLambda) {
		this.allowInlineKotlinLambda = allowInlineKotlinLambda;
	}

	public boolean isMoveInnerClasses() {
		return moveInnerClasses;
	}

	public void setMoveInnerClasses(boolean moveInnerClasses) {
		this.moveInnerClasses = moveInnerClasses;
	}

	public boolean isExtractFinally() {
		return extractFinally;
	}

	public void setExtractFinally(boolean extractFinally) {
		this.extractFinally = extractFinally;
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

	public void setIncludeDependencies(boolean includeDependencies) {
		this.includeDependencies = includeDependencies;
	}

	public boolean isIncludeDependencies() {
		return includeDependencies;
	}

	public Predicate<String> getClassFilter() {
		return classFilter;
	}

	public void setClassFilter(Predicate<String> classFilter) {
		this.classFilter = classFilter;
	}

	public Path getUserRenamesMappingsPath() {
		return userRenamesMappingsPath;
	}

	public void setUserRenamesMappingsPath(Path path) {
		this.userRenamesMappingsPath = path;
	}

	public UserRenamesMappingsMode getUserRenamesMappingsMode() {
		return userRenamesMappingsMode;
	}

	public void setUserRenamesMappingsMode(UserRenamesMappingsMode mode) {
		this.userRenamesMappingsMode = mode;
	}

	public boolean isDeobfuscationOn() {
		return deobfuscationOn;
	}

	public void setDeobfuscationOn(boolean deobfuscationOn) {
		this.deobfuscationOn = deobfuscationOn;
	}

	public boolean isDeobfuscationForceSave() {
		return generatedRenamesMappingFileMode == GeneratedRenamesMappingFileMode.OVERWRITE;
	}

	public void setDeobfuscationForceSave(boolean deobfuscationForceSave) {
		if (deobfuscationForceSave) {
			this.generatedRenamesMappingFileMode = GeneratedRenamesMappingFileMode.OVERWRITE;
		}
	}

	public GeneratedRenamesMappingFileMode getGeneratedRenamesMappingFileMode() {
		return generatedRenamesMappingFileMode;
	}

	public void setGeneratedRenamesMappingFileMode(GeneratedRenamesMappingFileMode mode) {
		this.generatedRenamesMappingFileMode = mode;
	}

	public UseSourceNameAsClassNameAlias getUseSourceNameAsClassNameAlias() {
		return useSourceNameAsClassNameAlias;
	}

	public void setUseSourceNameAsClassNameAlias(UseSourceNameAsClassNameAlias useSourceNameAsClassNameAlias) {
		this.useSourceNameAsClassNameAlias = useSourceNameAsClassNameAlias;
	}

	public int getSourceNameRepeatLimit() {
		return sourceNameRepeatLimit;
	}

	public void setSourceNameRepeatLimit(int sourceNameRepeatLimit) {
		this.sourceNameRepeatLimit = sourceNameRepeatLimit;
	}

	/**
	 * @deprecated Use {@link #getUseSourceNameAsClassNameAlias()} instead.
	 */
	@Deprecated
	public boolean isUseSourceNameAsClassAlias() {
		return getUseSourceNameAsClassNameAlias().toBoolean();
	}

	/**
	 * @deprecated Use {@link #setUseSourceNameAsClassNameAlias(UseSourceNameAsClassNameAlias)} instead.
	 */
	@Deprecated
	public void setUseSourceNameAsClassAlias(boolean useSourceNameAsClassAlias) {
		final var useSourceNameAsClassNameAlias = UseSourceNameAsClassNameAlias.create(useSourceNameAsClassAlias);
		setUseSourceNameAsClassNameAlias(useSourceNameAsClassNameAlias);
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

	public List<String> getDeobfuscationWhitelist() {
		return this.deobfuscationWhitelist;
	}

	public void setDeobfuscationWhitelist(List<String> deobfuscationWhitelist) {
		this.deobfuscationWhitelist = deobfuscationWhitelist;
	}

	public File getGeneratedRenamesMappingFile() {
		return generatedRenamesMappingFile;
	}

	public void setGeneratedRenamesMappingFile(File file) {
		this.generatedRenamesMappingFile = file;
	}

	public ResourceNameSource getResourceNameSource() {
		return resourceNameSource;
	}

	public void setResourceNameSource(ResourceNameSource resourceNameSource) {
		this.resourceNameSource = resourceNameSource;
	}

	public IAliasProvider getAliasProvider() {
		return aliasProvider;
	}

	public void setAliasProvider(IAliasProvider aliasProvider) {
		this.aliasProvider = aliasProvider;
	}

	public IRenameCondition getRenameCondition() {
		return renameCondition;
	}

	public void setRenameCondition(IRenameCondition renameCondition) {
		this.renameCondition = renameCondition;
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
		return exportGradleType != null;
	}

	public void setExportAsGradleProject(boolean exportAsGradleProject) {
		if (exportAsGradleProject) {
			if (exportGradleType == null) {
				exportGradleType = ExportGradleType.AUTO;
			}
		} else {
			exportGradleType = null;
		}
	}

	public @Nullable ExportGradleType getExportGradleType() {
		return exportGradleType;
	}

	public void setExportGradleType(@Nullable ExportGradleType exportGradleType) {
		this.exportGradleType = exportGradleType;
	}

	public boolean isRestoreSwitchOverString() {
		return restoreSwitchOverString;
	}

	public void setRestoreSwitchOverString(boolean restoreSwitchOverString) {
		this.restoreSwitchOverString = restoreSwitchOverString;
	}

	public boolean isSkipXmlPrettyPrint() {
		return skipXmlPrettyPrint;
	}

	public void setSkipXmlPrettyPrint(boolean skipXmlPrettyPrint) {
		this.skipXmlPrettyPrint = skipXmlPrettyPrint;
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

	public DecompilationMode getDecompilationMode() {
		return decompilationMode;
	}

	public void setDecompilationMode(DecompilationMode decompilationMode) {
		this.decompilationMode = decompilationMode;
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

	public IUsageInfoCache getUsageInfoCache() {
		return usageInfoCache;
	}

	public void setUsageInfoCache(IUsageInfoCache usageInfoCache) {
		this.usageInfoCache = usageInfoCache;
	}

	public ICodeData getCodeData() {
		return codeData;
	}

	public void setCodeData(ICodeData codeData) {
		this.codeData = codeData;
	}

	public String getCodeIndentStr() {
		return codeIndentStr;
	}

	public void setCodeIndentStr(String codeIndentStr) {
		this.codeIndentStr = codeIndentStr;
	}

	public String getCodeNewLineStr() {
		return codeNewLineStr;
	}

	public void setCodeNewLineStr(String codeNewLineStr) {
		this.codeNewLineStr = codeNewLineStr;
	}

	public CommentsLevel getCommentsLevel() {
		return commentsLevel;
	}

	public void setCommentsLevel(CommentsLevel commentsLevel) {
		this.commentsLevel = commentsLevel;
	}

	public IntegerFormat getIntegerFormat() {
		return integerFormat;
	}

	public void setIntegerFormat(IntegerFormat format) {
		this.integerFormat = format;
	}

	public int getTypeUpdatesLimitCount() {
		return typeUpdatesLimitCount;
	}

	public void setTypeUpdatesLimitCount(int typeUpdatesLimitCount) {
		this.typeUpdatesLimitCount = Math.max(1, typeUpdatesLimitCount);
	}

	public boolean isUseDxInput() {
		return useDxInput;
	}

	public void setUseDxInput(boolean useDxInput) {
		this.useDxInput = useDxInput;
	}

	public UseKotlinMethodsForVarNames getUseKotlinMethodsForVarNames() {
		return useKotlinMethodsForVarNames;
	}

	public void setUseKotlinMethodsForVarNames(UseKotlinMethodsForVarNames useKotlinMethodsForVarNames) {
		this.useKotlinMethodsForVarNames = useKotlinMethodsForVarNames;
	}

	public IJadxFilesGetter getFilesGetter() {
		return filesGetter;
	}

	public void setFilesGetter(IJadxFilesGetter filesGetter) {
		this.filesGetter = filesGetter;
	}

	public IJadxSecurity getSecurity() {
		return security;
	}

	public void setSecurity(IJadxSecurity security) {
		this.security = security;
	}

	public boolean isSkipFilesSave() {
		return skipFilesSave;
	}

	public void setSkipFilesSave(boolean skipFilesSave) {
		this.skipFilesSave = skipFilesSave;
	}

	public boolean isRunDebugChecks() {
		return runDebugChecks;
	}

	public void setRunDebugChecks(boolean runDebugChecks) {
		this.runDebugChecks = runDebugChecks;
	}

	public Map<String, String> getPluginOptions() {
		return pluginOptions;
	}

	public void setPluginOptions(Map<String, String> pluginOptions) {
		this.pluginOptions = pluginOptions;
	}

	public Set<String> getDisabledPlugins() {
		return disabledPlugins;
	}

	public void setDisabledPlugins(Set<String> disabledPlugins) {
		this.disabledPlugins = disabledPlugins;
	}

	public JadxPluginLoader getPluginLoader() {
		return pluginLoader;
	}

	public void setPluginLoader(JadxPluginLoader pluginLoader) {
		this.pluginLoader = pluginLoader;
	}

	public boolean isLoadJadxClsSetFile() {
		return loadJadxClsSetFile;
	}

	public void setLoadJadxClsSetFile(boolean loadJadxClsSetFile) {
		this.loadJadxClsSetFile = loadJadxClsSetFile;
	}

	public void setUseHeadersForDetectResourceExtensions(boolean useHeadersForDetectResourceExtensions) {
		this.useHeadersForDetectResourceExtensions = useHeadersForDetectResourceExtensions;
	}

	public boolean isUseHeadersForDetectResourceExtensions() {
		return useHeadersForDetectResourceExtensions;
	}

	/**
	 * Hash of all options that can change result code
	 */
	public String makeCodeArgsHash(@Nullable JadxDecompiler decompiler) {
		String argStr = "args:" + decompilationMode + useImports + showInconsistentCode
				+ inlineAnonymousClasses + inlineMethods + moveInnerClasses + allowInlineKotlinLambda
				+ deobfuscationOn + deobfuscationMinLength + deobfuscationMaxLength + deobfuscationWhitelist
				+ useSourceNameAsClassNameAlias + sourceNameRepeatLimit
				+ resourceNameSource + useHeadersForDetectResourceExtensions
				+ useKotlinMethodsForVarNames
				+ insertDebugLines + extractFinally
				+ debugInfo + escapeUnicode + replaceConsts + restoreSwitchOverString
				+ respectBytecodeAccModifiers + fsCaseSensitive + renameFlags
				+ commentsLevel + useDxInput + integerFormat + typeUpdatesLimitCount
				+ "|" + buildPluginsHash(decompiler);
		return FileUtils.md5Sum(argStr);
	}

	private static String buildPluginsHash(@Nullable JadxDecompiler decompiler) {
		if (decompiler == null) {
			return "";
		}
		return decompiler.getPluginManager().getResolvedPluginContexts()
				.stream()
				.map(PluginContext::getInputsHash)
				.collect(Collectors.joining(":"));
	}

	@Override
	public String toString() {
		return "JadxArgs{" + "inputFiles=" + inputFiles
				+ ", outDir=" + outDir
				+ ", outDirSrc=" + outDirSrc
				+ ", outDirRes=" + outDirRes
				+ ", threadsCount=" + threadsCount
				+ ", decompilationMode=" + decompilationMode
				+ ", showInconsistentCode=" + showInconsistentCode
				+ ", useImports=" + useImports
				+ ", skipResources=" + skipResources
				+ ", skipSources=" + skipSources
				+ ", includeDependencies=" + includeDependencies
				+ ", userRenamesMappingsPath=" + userRenamesMappingsPath
				+ ", userRenamesMappingsMode=" + userRenamesMappingsMode
				+ ", deobfuscationOn=" + deobfuscationOn
				+ ", generatedRenamesMappingFile=" + generatedRenamesMappingFile
				+ ", generatedRenamesMappingFileMode=" + generatedRenamesMappingFileMode
				+ ", resourceNameSource=" + resourceNameSource
				+ ", useSourceNameAsClassNameAlias=" + useSourceNameAsClassNameAlias
				+ ", sourceNameRepeatLimit=" + sourceNameRepeatLimit
				+ ", useKotlinMethodsForVarNames=" + useKotlinMethodsForVarNames
				+ ", insertDebugLines=" + insertDebugLines
				+ ", extractFinally=" + extractFinally
				+ ", deobfuscationMinLength=" + deobfuscationMinLength
				+ ", deobfuscationMaxLength=" + deobfuscationMaxLength
				+ ", deobfuscationWhitelist=" + deobfuscationWhitelist
				+ ", escapeUnicode=" + escapeUnicode
				+ ", replaceConsts=" + replaceConsts
				+ ", restoreSwitchOverString=" + restoreSwitchOverString
				+ ", respectBytecodeAccModifiers=" + respectBytecodeAccModifiers
				+ ", exportGradleType=" + exportGradleType
				+ ", skipXmlPrettyPrint=" + skipXmlPrettyPrint
				+ ", fsCaseSensitive=" + fsCaseSensitive
				+ ", renameFlags=" + renameFlags
				+ ", outputFormat=" + outputFormat
				+ ", commentsLevel=" + commentsLevel
				+ ", codeCache=" + codeCache
				+ ", codeWriter=" + codeWriterProvider.apply(this).getClass().getSimpleName()
				+ ", useDxInput=" + useDxInput
				+ ", pluginOptions=" + pluginOptions
				+ ", cfgOutput=" + cfgOutput
				+ ", rawCFGOutput=" + rawCFGOutput
				+ ", useHeadersForDetectResourceExtensions=" + useHeadersForDetectResourceExtensions
				+ ", typeUpdatesLimitCount=" + typeUpdatesLimitCount
				+ '}';
	}
}
