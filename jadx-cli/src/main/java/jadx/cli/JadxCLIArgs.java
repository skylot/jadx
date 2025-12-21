package jadx.cli;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;

import jadx.api.CommentsLevel;
import jadx.api.DecompilationMode;
import jadx.api.JadxArgs;
import jadx.api.JadxArgs.RenameEnum;
import jadx.api.JadxArgs.UseKotlinMethodsForVarNames;
import jadx.api.JadxDecompiler;
import jadx.api.args.GeneratedRenamesMappingFileMode;
import jadx.api.args.IntegerFormat;
import jadx.api.args.ResourceNameSource;
import jadx.api.args.UseSourceNameAsClassNameAlias;
import jadx.api.args.UserRenamesMappingsMode;
import jadx.cli.config.IJadxConfig;
import jadx.cli.config.JadxConfigAdapter;
import jadx.cli.config.JadxConfigExclude;
import jadx.commons.app.JadxCommonFiles;
import jadx.commons.app.JadxTempFiles;
import jadx.core.deobf.conditions.DeobfWhitelist;
import jadx.core.export.ExportGradleType;
import jadx.core.utils.exceptions.JadxArgsValidateException;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;

public class JadxCLIArgs implements IJadxConfig {
	private static final Logger LOG = LoggerFactory.getLogger(JadxCLIArgs.class);

	@JadxConfigExclude
	@Parameter(description = "<input files> (.apk, .dex, .jar, .class, .smali, .zip, .aar, .arsc, .aab, .xapk, .apkm, .jadx.kts)")
	protected List<String> files = Collections.emptyList();

	@JadxConfigExclude
	@Parameter(names = { "-d", "--output-dir" }, description = "output directory")
	protected String outDir;

	@JadxConfigExclude
	@Parameter(names = { "-ds", "--output-dir-src" }, description = "output directory for sources")
	protected String outDirSrc;

	@JadxConfigExclude
	@Parameter(names = { "-dr", "--output-dir-res" }, description = "output directory for resources")
	protected String outDirRes;

	@Parameter(names = { "-r", "--no-res" }, description = "do not decode resources")
	protected boolean skipResources = false;

	@Parameter(names = { "-s", "--no-src" }, description = "do not decompile source code")
	protected boolean skipSources = false;

	@Parameter(names = { "-j", "--threads-count" }, description = "processing threads count")
	protected int threadsCount = JadxArgs.DEFAULT_THREADS_COUNT;

	@JadxConfigExclude
	@Parameter(names = { "--single-class" }, description = "decompile a single class, full name, raw or alias")
	protected String singleClass = null;

	@JadxConfigExclude
	@Parameter(names = { "--single-class-output" }, description = "file or dir for write if decompile a single class")
	protected String singleClassOutput = null;

	@Parameter(names = { "--output-format" }, description = "can be 'java' or 'json'")
	protected String outputFormat = "java";

	@Parameter(names = { "-e", "--export-gradle" }, description = "save as gradle project (set '--export-gradle-type' to 'auto')")
	protected boolean exportAsGradleProject = false;

	@Parameter(
			names = { "--export-gradle-type" },
			description = "Gradle project template for export:"
					+ "\n 'auto' - detect automatically"
					+ "\n 'android-app' - Android Application (apk)"
					+ "\n 'android-library' - Android Library (aar)"
					+ "\n 'simple-java' - simple Java",
			converter = ExportGradleTypeConverter.class
	)
	protected @Nullable ExportGradleType exportGradleType = null;

	@Parameter(
			names = { "-m", "--decompilation-mode" },
			description = "code output mode:"
					+ "\n 'auto' - trying best options (default)"
					+ "\n 'restructure' - restore code structure (normal java code)"
					+ "\n 'simple' - simplified instructions (linear, with goto's)"
					+ "\n 'fallback' - raw instructions without modifications",
			converter = DecompilationModeConverter.class
	)
	protected DecompilationMode decompilationMode = DecompilationMode.AUTO;

	@Parameter(names = { "--show-bad-code" }, description = "show inconsistent code (incorrectly decompiled)")
	protected boolean showInconsistentCode = false;

	@Parameter(names = { "--no-xml-pretty-print" }, description = "do not prettify XML")
	protected boolean skipXmlPrettyPrint = false;

	@Parameter(names = { "--no-imports" }, description = "disable use of imports, always write entire package name")
	protected boolean useImports = true;

	@Parameter(names = { "--no-debug-info" }, description = "disable debug info parsing and processing")
	protected boolean debugInfo = true;

	@Parameter(names = { "--add-debug-lines" }, description = "add comments with debug line numbers if available")
	protected boolean addDebugLines = false;

	@Parameter(names = { "--no-inline-anonymous" }, description = "disable anonymous classes inline")
	protected boolean inlineAnonymousClasses = true;

	@Parameter(names = { "--no-inline-methods" }, description = "disable methods inline")
	protected boolean inlineMethods = true;

	@Parameter(names = { "--no-move-inner-classes" }, description = "disable move inner classes into parent")
	protected boolean moveInnerClasses = true;

	@Parameter(names = { "--no-inline-kotlin-lambda" }, description = "disable inline for Kotlin lambdas")
	protected boolean allowInlineKotlinLambda = true;

	@Parameter(names = "--no-finally", description = "don't extract finally block")
	protected boolean extractFinally = true;

	@Parameter(names = "--no-restore-switch-over-string", description = "don't restore switch over string")
	protected boolean restoreSwitchOverString = true;

	@Parameter(names = "--no-replace-consts", description = "don't replace constant value with matching constant field")
	protected boolean replaceConsts = true;

	@Parameter(names = { "--escape-unicode" }, description = "escape non latin characters in strings (with \\u)")
	protected boolean escapeUnicode = false;

	@Parameter(names = { "--respect-bytecode-access-modifiers" }, description = "don't change original access modifiers")
	protected boolean respectBytecodeAccessModifiers = false;

	@Parameter(
			names = { "--mappings-path" },
			description = "deobfuscation mappings file or directory. Allowed formats: Tiny and Tiny v2 (both '.tiny'), Enigma (.mapping) or Enigma directory"
	)
	protected Path userRenamesMappingsPath;

	@Parameter(
			names = { "--mappings-mode" },
			description = "set mode for handling the deobfuscation mapping file:"
					+ "\n 'read' - just read, user can always save manually (default)"
					+ "\n 'read-and-autosave-every-change' - read and autosave after every change"
					+ "\n 'read-and-autosave-before-closing' - read and autosave before exiting the app or closing the project"
					+ "\n 'ignore' - don't read or save (can be used to skip loading mapping files referenced in the project file)"
	)
	protected UserRenamesMappingsMode userRenamesMappingsMode = UserRenamesMappingsMode.getDefault();

	@Parameter(names = { "--deobf" }, description = "activate deobfuscation")
	protected boolean deobfuscationOn = false;

	@Parameter(names = { "--deobf-min" }, description = "min length of name, renamed if shorter")
	protected int deobfuscationMinLength = 3;

	@Parameter(names = { "--deobf-max" }, description = "max length of name, renamed if longer")
	protected int deobfuscationMaxLength = 64;

	@Parameter(
			names = { "--deobf-whitelist" },
			description = "space separated list of classes (full name) and packages (ends with '.*') to exclude from deobfuscation"
	)
	protected String deobfuscationWhitelistStr = DeobfWhitelist.DEFAULT_STR;

	@JadxConfigExclude
	@Parameter(
			names = { "--deobf-cfg-file" },
			description = "deobfuscation mappings file used for JADX auto-generated names (in the JOBF file format),"
					+ " default: same dir and name as input file with '.jobf' extension"
	)
	protected String generatedRenamesMappingFile;

	@Parameter(
			names = { "--deobf-cfg-file-mode" },
			description = "set mode for handling the JADX auto-generated names' deobfuscation map file:"
					+ "\n 'read' - read if found, don't save (default)"
					+ "\n 'read-or-save' - read if found, save otherwise (don't overwrite)"
					+ "\n 'overwrite' - don't read, always save"
					+ "\n 'ignore' - don't read and don't save",
			converter = DeobfuscationMapFileModeConverter.class
	)
	protected GeneratedRenamesMappingFileMode generatedRenamesMappingFileMode = GeneratedRenamesMappingFileMode.getDefault();

	@SuppressWarnings("DeprecatedIsStillUsed")
	@Parameter(
			names = { "--deobf-use-sourcename" },
			description = "use source file name as class name alias."
					+ "\nDEPRECATED, use \"--use-source-name-as-class-name-alias\" instead",
			hidden = true
	)
	@Deprecated
	protected Boolean deobfuscationUseSourceNameAsAlias = null;

	@Parameter(
			names = { "--deobf-res-name-source" },
			description = "better name source for resources:"
					+ "\n 'auto' - automatically select best name (default)"
					+ "\n 'resources' - use resources names"
					+ "\n 'code' - use R class fields names",
			converter = ResourceNameSourceConverter.class
	)
	protected ResourceNameSource resourceNameSource = ResourceNameSource.AUTO;

	@Parameter(
			names = { "--use-source-name-as-class-name-alias" },
			description = "use source name as class name alias:"
					+ "\n 'always' - always use source name if it's available"
					+ "\n 'if-better' - use source name if it seems better than the current one"
					+ "\n 'never' - never use source name, even if it's available",
			converter = UseSourceNameAsClassNameConverter.class
	)
	protected UseSourceNameAsClassNameAlias useSourceNameAsClassNameAlias = null;

	@Parameter(
			names = { "--source-name-repeat-limit" },
			description = "allow using source name if it appears less than a limit number"
	)
	protected int sourceNameRepeatLimit = 10;

	@Parameter(
			names = { "--use-kotlin-methods-for-var-names" },
			description = "use kotlin intrinsic methods to rename variables, values: disable, apply, apply-and-hide",
			converter = UseKotlinMethodsForVarNamesConverter.class
	)
	protected UseKotlinMethodsForVarNames useKotlinMethodsForVarNames = UseKotlinMethodsForVarNames.APPLY;

	@Parameter(
			names = { "--use-headers-for-detect-resource-extensions" },
			description = "Use headers for detect resource extensions if resource obfuscated"
	)
	protected boolean useHeadersForDetectResourceExtensions = false;

	@Parameter(
			names = { "--rename-flags" },
			description = "fix options (comma-separated list of):"
					+ "\n 'case' - fix case sensitivity issues (according to --fs-case-sensitive option),"
					+ "\n 'valid' - rename java identifiers to make them valid,"
					+ "\n 'printable' - remove non-printable chars from identifiers,"
					+ "\nor single 'none' - to disable all renames"
					+ "\nor single 'all' - to enable all (default)",
			listConverter = RenameConverter.class
	)
	protected Set<RenameEnum> renameFlags = EnumSet.allOf(RenameEnum.class);

	@Parameter(
			names = { "--integer-format" },
			description = "how integers are displayed:"
					+ "\n 'auto' - automatically select (default)"
					+ "\n 'decimal' - use decimal"
					+ "\n 'hexadecimal' - use hexadecimal",
			converter = IntegerFormatConverter.class
	)
	protected IntegerFormat integerFormat = IntegerFormat.AUTO;

	@Parameter(names = { "--type-update-limit" }, description = "type update limit count (per one instruction)")
	protected int typeUpdatesLimitCount = 10;

	@Parameter(names = { "--fs-case-sensitive" }, description = "treat filesystem as case sensitive, false by default")
	protected boolean fsCaseSensitive = false;

	@Parameter(names = { "--cfg" }, description = "save methods control flow graph to dot file")
	protected boolean cfgOutput = false;

	@Parameter(names = { "--raw-cfg" }, description = "save methods control flow graph (use raw instructions)")
	protected boolean rawCfgOutput = false;

	@Parameter(names = { "-f", "--fallback" }, description = "set '--decompilation-mode' to 'fallback' (deprecated)")
	protected boolean fallbackMode = false;

	@Parameter(names = { "--use-dx" }, description = "use dx/d8 to convert java bytecode")
	protected boolean useDx = false;

	@Parameter(
			names = { "--comments-level" },
			description = "set code comments level, values: error, warn, info, debug, user-only, none",
			converter = CommentsLevelConverter.class
	)
	protected CommentsLevel commentsLevel = CommentsLevel.INFO;

	@Parameter(
			names = { "--log-level" },
			description = "set log level, values: quiet, progress, error, warn, info, debug",
			converter = LogLevelConverter.class
	)
	protected LogHelper.LogLevelEnum logLevel = LogHelper.LogLevelEnum.PROGRESS;

	@JadxConfigExclude
	@Parameter(names = { "-v", "--verbose" }, description = "verbose output (set --log-level to DEBUG)")
	protected boolean verbose = false;

	@JadxConfigExclude
	@Parameter(names = { "-q", "--quiet" }, description = "turn off output (set --log-level to QUIET)")
	protected boolean quiet = false;

	@JadxConfigExclude
	@Parameter(names = { "--disable-plugins" }, description = "comma separated list of plugin ids to disable")
	protected String disablePlugins = "";

	@JadxConfigExclude
	@Parameter(
			names = { "--config" },
			defaultValueDescription = "<config-ref>",
			description = "load configuration from file, <config-ref> can be:"
					+ "\n path to '.json' file"
					+ "\n short name - uses file with this name from config directory"
					+ "\n 'none' - to disable config loading"
	)
	protected String config = "";

	@JadxConfigExclude
	@Parameter(
			names = { "--save-config" },
			defaultValueDescription = "<config-ref>",
			description = "save current options into configuration file and exit, <config-ref> can be:"
					+ "\n empty - for default config"
					+ "\n path to '.json' file"
					+ "\n short name - file will be saved in config directory"
	)
	protected String saveConfig = null;

	@JadxConfigExclude
	@Parameter(names = { "--print-files" }, description = "print files and directories used by jadx (config, cache, temp)")
	protected boolean printFiles = false;

	@JadxConfigExclude
	@Parameter(names = { "--version" }, description = "print jadx version")
	protected boolean printVersion = false;

	@JadxConfigExclude
	@Parameter(names = { "-h", "--help" }, description = "print this help", help = true)
	protected boolean printHelp = false;

	@DynamicParameter(names = "-P", description = "Plugin options", hidden = true)
	protected Map<String, String> pluginOptions = new HashMap<>();

	/**
	 * Obsolete method without config support,
	 * prefer {@link #processArgs(String[], JadxCLIArgs, JadxConfigAdapter)}
	 */
	public boolean processArgs(String[] args) {
		return processArgs(args, this, null) != null;
	}

	public static <T extends JadxCLIArgs> @Nullable T processArgs(
			String[] args, T argsObj, @Nullable JadxConfigAdapter<T> configAdapter) {
		JCommanderWrapper jcw = new JCommanderWrapper(argsObj);
		if (!jcw.parse(args)) {
			return null;
		}
		applyArgs(argsObj);

		// process commands and early exit flags
		if (!argsObj.process(jcw)) {
			return null;
		}
		if (configAdapter != null) {
			if (argsObj.printFiles) {
				printFilesAndDirs(configAdapter.getDefaultConfigFileName());
				return null;
			}
			if (!argsObj.config.equalsIgnoreCase("none")) {
				// load config file and merge with command line args
				try {
					configAdapter.useConfigRef(argsObj.config);
					T configObj = configAdapter.load();
					if (configObj != null) {
						jcw.overrideProvided(configObj);
						argsObj = configObj;
					}
				} catch (Exception e) {
					LOG.error("Config load failed, continue with default values", e);
				}
			}
		}
		// verify result object
		argsObj.verify();
		applyArgs(argsObj);

		// save config if requested
		if (argsObj.saveConfig != null) {
			saveConfig(argsObj, configAdapter);
			return null;
		}
		return argsObj;
	}

	private static <T extends JadxCLIArgs> void applyArgs(T argsObj) {
		// apply log levels
		LogHelper.initLogLevel(argsObj);
		LogHelper.applyLogLevels();
	}

	public boolean process(JCommanderWrapper jcw) {
		if (jcw.processCommands()) {
			return false;
		}
		if (printHelp) {
			jcw.printUsage();
			return false;
		}
		if (printVersion) {
			System.out.println(JadxDecompiler.getVersion());
			return false;
		}
		// unknown options added to 'files', run checks
		for (String fileName : files) {
			if (fileName.startsWith("-")) {
				throw new JadxArgsValidateException("Unknown option: " + fileName);
			}
		}
		return true;
	}

	private static void printFilesAndDirs(String defaultConfigFileName) {
		System.out.println("Files and directories used by jadx:");
		System.out.println(" - default config file: " + JadxCommonFiles.getConfigDir().resolve(defaultConfigFileName).toAbsolutePath());
		System.out.println(" - config directory:    " + JadxCommonFiles.getConfigDir().toAbsolutePath());
		System.out.println(" - cache directory:     " + JadxCommonFiles.getCacheDir().toAbsolutePath());
		System.out.println(" - temp directory:      " + JadxTempFiles.getTempRootDir().getParent().toAbsolutePath());
	}

	public void verify() {
		if (threadsCount <= 0) {
			throw new JadxArgsValidateException("Threads count must be positive, got: " + threadsCount);
		}
	}

	private static <T extends JadxCLIArgs> void saveConfig(T argsObj, @Nullable JadxConfigAdapter<T> configAdapter) {
		if (configAdapter == null) {
			throw new JadxRuntimeException("Config adapter set to null, can't save config");
		}
		configAdapter.useConfigRef(argsObj.saveConfig);
		configAdapter.save(argsObj);
		System.out.println("Config saved to " + configAdapter.getConfigPath().toAbsolutePath());
	}

	public JadxArgs toJadxArgs() {
		JadxArgs args = new JadxArgs();
		args.setInputFiles(files.stream().map(FileUtils::toFile).collect(Collectors.toList()));
		args.setOutDir(FileUtils.toFile(outDir));
		args.setOutDirSrc(FileUtils.toFile(outDirSrc));
		args.setOutDirRes(FileUtils.toFile(outDirRes));
		args.setOutputFormat(JadxArgs.OutputFormatEnum.valueOf(outputFormat.toUpperCase()));
		args.setThreadsCount(threadsCount);
		args.setSkipSources(skipSources);
		args.setSkipResources(skipResources);
		if (fallbackMode) {
			args.setDecompilationMode(DecompilationMode.FALLBACK);
		} else {
			args.setDecompilationMode(decompilationMode);
		}
		args.setShowInconsistentCode(showInconsistentCode);
		args.setCfgOutput(cfgOutput);
		args.setRawCFGOutput(rawCfgOutput);
		args.setReplaceConsts(replaceConsts);
		if (userRenamesMappingsPath != null) {
			args.setUserRenamesMappingsPath(userRenamesMappingsPath);
		}
		args.setUserRenamesMappingsMode(userRenamesMappingsMode);
		args.setDeobfuscationOn(deobfuscationOn);
		args.setGeneratedRenamesMappingFile(FileUtils.toFile(generatedRenamesMappingFile));
		args.setGeneratedRenamesMappingFileMode(generatedRenamesMappingFileMode);
		args.setDeobfuscationMinLength(deobfuscationMinLength);
		args.setDeobfuscationMaxLength(deobfuscationMaxLength);
		args.setDeobfuscationWhitelist(Arrays.asList(deobfuscationWhitelistStr.split(" ")));
		args.setUseSourceNameAsClassNameAlias(getUseSourceNameAsClassNameAlias());
		args.setUseHeadersForDetectResourceExtensions(useHeadersForDetectResourceExtensions);
		args.setSourceNameRepeatLimit(sourceNameRepeatLimit);
		args.setUseKotlinMethodsForVarNames(useKotlinMethodsForVarNames);
		args.setResourceNameSource(resourceNameSource);
		args.setEscapeUnicode(escapeUnicode);
		args.setRespectBytecodeAccModifiers(respectBytecodeAccessModifiers);
		args.setExportGradleType(exportGradleType);
		if (exportAsGradleProject && exportGradleType == null) {
			args.setExportGradleType(ExportGradleType.AUTO);
		}
		args.setSkipXmlPrettyPrint(skipXmlPrettyPrint);
		args.setUseImports(useImports);
		args.setDebugInfo(debugInfo);
		args.setInsertDebugLines(addDebugLines);
		args.setInlineAnonymousClasses(inlineAnonymousClasses);
		args.setInlineMethods(inlineMethods);
		args.setMoveInnerClasses(moveInnerClasses);
		args.setAllowInlineKotlinLambda(allowInlineKotlinLambda);
		args.setExtractFinally(extractFinally);
		args.setRestoreSwitchOverString(restoreSwitchOverString);
		args.setRenameFlags(buildEnumSetForRenameFlags());
		args.setFsCaseSensitive(fsCaseSensitive);
		args.setCommentsLevel(commentsLevel);
		args.setIntegerFormat(integerFormat);
		args.setTypeUpdatesLimitCount(typeUpdatesLimitCount);
		args.setUseDxInput(useDx);
		args.setPluginOptions(pluginOptions);
		args.setDisabledPlugins(Arrays.stream(disablePlugins.split(",")).map(String::trim).collect(Collectors.toSet()));
		return args;
	}

	private EnumSet<RenameEnum> buildEnumSetForRenameFlags() {
		EnumSet<RenameEnum> set = EnumSet.noneOf(RenameEnum.class);
		set.addAll(renameFlags);
		return set;
	}

	public List<String> getFiles() {
		return files;
	}

	public void setFiles(List<String> files) {
		this.files = files;
	}

	public String getOutDir() {
		return outDir;
	}

	public String getOutDirSrc() {
		return outDirSrc;
	}

	public String getOutDirRes() {
		return outDirRes;
	}

	public String getSingleClass() {
		return singleClass;
	}

	public String getSingleClassOutput() {
		return singleClassOutput;
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

	public int getThreadsCount() {
		return threadsCount;
	}

	public void setThreadsCount(int threadsCount) {
		this.threadsCount = threadsCount;
	}

	public boolean isFallbackMode() {
		return fallbackMode;
	}

	public boolean isUseDx() {
		return useDx;
	}

	public void setUseDx(boolean useDx) {
		this.useDx = useDx;
	}

	public DecompilationMode getDecompilationMode() {
		return decompilationMode;
	}

	public void setDecompilationMode(DecompilationMode decompilationMode) {
		this.decompilationMode = decompilationMode;
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

	public boolean isAddDebugLines() {
		return addDebugLines;
	}

	public void setAddDebugLines(boolean addDebugLines) {
		this.addDebugLines = addDebugLines;
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

	public boolean isMoveInnerClasses() {
		return moveInnerClasses;
	}

	public void setMoveInnerClasses(boolean moveInnerClasses) {
		this.moveInnerClasses = moveInnerClasses;
	}

	public boolean isAllowInlineKotlinLambda() {
		return allowInlineKotlinLambda;
	}

	public void setAllowInlineKotlinLambda(boolean allowInlineKotlinLambda) {
		this.allowInlineKotlinLambda = allowInlineKotlinLambda;
	}

	public boolean isExtractFinally() {
		return extractFinally;
	}

	public void setExtractFinally(boolean extractFinally) {
		this.extractFinally = extractFinally;
	}

	public boolean isRestoreSwitchOverString() {
		return restoreSwitchOverString;
	}

	public void setRestoreSwitchOverString(boolean restoreSwitchOverString) {
		this.restoreSwitchOverString = restoreSwitchOverString;
	}

	public Path getUserRenamesMappingsPath() {
		return userRenamesMappingsPath;
	}

	public void setUserRenamesMappingsPath(Path userRenamesMappingsPath) {
		this.userRenamesMappingsPath = userRenamesMappingsPath;
	}

	public UserRenamesMappingsMode getUserRenamesMappingsMode() {
		return userRenamesMappingsMode;
	}

	public void setUserRenamesMappingsMode(UserRenamesMappingsMode userRenamesMappingsMode) {
		this.userRenamesMappingsMode = userRenamesMappingsMode;
	}

	public boolean isDeobfuscationOn() {
		return deobfuscationOn;
	}

	public void setDeobfuscationOn(boolean deobfuscationOn) {
		this.deobfuscationOn = deobfuscationOn;
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

	public String getDeobfuscationWhitelistStr() {
		return deobfuscationWhitelistStr;
	}

	public void setDeobfuscationWhitelistStr(String deobfuscationWhitelistStr) {
		this.deobfuscationWhitelistStr = deobfuscationWhitelistStr;
	}

	public String getGeneratedRenamesMappingFile() {
		return generatedRenamesMappingFile;
	}

	public void setGeneratedRenamesMappingFile(String generatedRenamesMappingFile) {
		this.generatedRenamesMappingFile = generatedRenamesMappingFile;
	}

	public GeneratedRenamesMappingFileMode getGeneratedRenamesMappingFileMode() {
		return generatedRenamesMappingFileMode;
	}

	public void setGeneratedRenamesMappingFileMode(GeneratedRenamesMappingFileMode generatedRenamesMappingFileMode) {
		this.generatedRenamesMappingFileMode = generatedRenamesMappingFileMode;
	}

	public int getSourceNameRepeatLimit() {
		return sourceNameRepeatLimit;
	}

	public void setSourceNameRepeatLimit(int sourceNameRepeatLimit) {
		this.sourceNameRepeatLimit = sourceNameRepeatLimit;
	}

	public UseSourceNameAsClassNameAlias getUseSourceNameAsClassNameAlias() {
		if (useSourceNameAsClassNameAlias != null) {
			return useSourceNameAsClassNameAlias;
		} else if (deobfuscationUseSourceNameAsAlias != null) {
			// noinspection deprecation
			return UseSourceNameAsClassNameAlias.create(deobfuscationUseSourceNameAsAlias);
		} else {
			return UseSourceNameAsClassNameAlias.getDefault();
		}
	}

	public void setUseSourceNameAsClassNameAlias(UseSourceNameAsClassNameAlias useSourceNameAsClassNameAlias) {
		this.useSourceNameAsClassNameAlias = useSourceNameAsClassNameAlias;
	}

	/**
	 * @deprecated Use {@link #getUseSourceNameAsClassNameAlias()} instead.
	 */
	@Deprecated
	public boolean isDeobfuscationUseSourceNameAsAlias() {
		return getUseSourceNameAsClassNameAlias().toBoolean();
	}

	public void setDeobfuscationUseSourceNameAsAlias(Boolean deobfuscationUseSourceNameAsAlias) {
		this.deobfuscationUseSourceNameAsAlias = deobfuscationUseSourceNameAsAlias;
	}

	public ResourceNameSource getResourceNameSource() {
		return resourceNameSource;
	}

	public void setResourceNameSource(ResourceNameSource resourceNameSource) {
		this.resourceNameSource = resourceNameSource;
	}

	public UseKotlinMethodsForVarNames getUseKotlinMethodsForVarNames() {
		return useKotlinMethodsForVarNames;
	}

	public void setUseKotlinMethodsForVarNames(UseKotlinMethodsForVarNames useKotlinMethodsForVarNames) {
		this.useKotlinMethodsForVarNames = useKotlinMethodsForVarNames;
	}

	public IntegerFormat getIntegerFormat() {
		return integerFormat;
	}

	public void setIntegerFormat(IntegerFormat integerFormat) {
		this.integerFormat = integerFormat;
	}

	public int getTypeUpdatesLimitCount() {
		return typeUpdatesLimitCount;
	}

	public void setTypeUpdatesLimitCount(int typeUpdatesLimitCount) {
		this.typeUpdatesLimitCount = typeUpdatesLimitCount;
	}

	public boolean isEscapeUnicode() {
		return escapeUnicode;
	}

	public void setEscapeUnicode(boolean escapeUnicode) {
		this.escapeUnicode = escapeUnicode;
	}

	public boolean isCfgOutput() {
		return cfgOutput;
	}

	public void setCfgOutput(boolean cfgOutput) {
		this.cfgOutput = cfgOutput;
	}

	public boolean isRawCfgOutput() {
		return rawCfgOutput;
	}

	public void setRawCfgOutput(boolean rawCfgOutput) {
		this.rawCfgOutput = rawCfgOutput;
	}

	public boolean isReplaceConsts() {
		return replaceConsts;
	}

	public void setReplaceConsts(boolean replaceConsts) {
		this.replaceConsts = replaceConsts;
	}

	public boolean isRespectBytecodeAccessModifiers() {
		return respectBytecodeAccessModifiers;
	}

	public void setRespectBytecodeAccessModifiers(boolean respectBytecodeAccessModifiers) {
		this.respectBytecodeAccessModifiers = respectBytecodeAccessModifiers;
	}

	public boolean isExportAsGradleProject() {
		return exportAsGradleProject;
	}

	public void setExportAsGradleProject(boolean exportAsGradleProject) {
		this.exportAsGradleProject = exportAsGradleProject;
	}

	public boolean isSkipXmlPrettyPrint() {
		return skipXmlPrettyPrint;
	}

	public void setSkipXmlPrettyPrint(boolean skipXmlPrettyPrint) {
		this.skipXmlPrettyPrint = skipXmlPrettyPrint;
	}

	public boolean isRenameCaseSensitive() {
		return renameFlags.contains(RenameEnum.CASE);
	}

	public boolean isRenameValid() {
		return renameFlags.contains(RenameEnum.VALID);
	}

	public boolean isRenamePrintable() {
		return renameFlags.contains(RenameEnum.PRINTABLE);
	}

	public boolean isFsCaseSensitive() {
		return fsCaseSensitive;
	}

	public void setFsCaseSensitive(boolean fsCaseSensitive) {
		this.fsCaseSensitive = fsCaseSensitive;
	}

	public boolean isUseHeadersForDetectResourceExtensions() {
		return useHeadersForDetectResourceExtensions;
	}

	public void setUseHeadersForDetectResourceExtensions(boolean useHeadersForDetectResourceExtensions) {
		this.useHeadersForDetectResourceExtensions = useHeadersForDetectResourceExtensions;
	}

	public CommentsLevel getCommentsLevel() {
		return commentsLevel;
	}

	public void setCommentsLevel(CommentsLevel commentsLevel) {
		this.commentsLevel = commentsLevel;
	}

	public LogHelper.LogLevelEnum getLogLevel() {
		return logLevel;
	}

	public void setLogLevel(LogHelper.LogLevelEnum logLevel) {
		this.logLevel = logLevel;
	}

	public Map<String, String> getPluginOptions() {
		return pluginOptions;
	}

	public void setPluginOptions(Map<String, String> pluginOptions) {
		this.pluginOptions = pluginOptions;
	}

	public String getDisablePlugins() {
		return disablePlugins;
	}

	public void setDisablePlugins(String disablePlugins) {
		this.disablePlugins = disablePlugins;
	}

	public void setExportGradleType(@Nullable ExportGradleType exportGradleType) {
		this.exportGradleType = exportGradleType;
	}

	public void setOutputFormat(String outputFormat) {
		this.outputFormat = outputFormat;
	}

	public Set<RenameEnum> getRenameFlags() {
		return renameFlags;
	}

	public void setRenameFlags(Set<RenameEnum> renameFlags) {
		this.renameFlags = renameFlags;
	}

	public String getConfig() {
		return config;
	}

	static class RenameConverter implements IStringConverter<Set<RenameEnum>> {
		private final String paramName;

		RenameConverter(String paramName) {
			this.paramName = paramName;
		}

		@Override
		public Set<RenameEnum> convert(String value) {
			if (value.equalsIgnoreCase("NONE")) {
				return EnumSet.noneOf(RenameEnum.class);
			}
			if (value.equalsIgnoreCase("ALL")) {
				return EnumSet.allOf(RenameEnum.class);
			}
			Set<RenameEnum> set = EnumSet.noneOf(RenameEnum.class);
			for (String s : value.split(",")) {
				try {
					set.add(RenameEnum.valueOf(s.trim().toUpperCase(Locale.ROOT)));
				} catch (Exception e) {
					throw new JadxArgsValidateException(
							'\'' + s + "' is unknown for parameter " + paramName
									+ ", possible values are " + enumValuesString(RenameEnum.values()));
				}
			}
			return set;
		}
	}

	public static class CommentsLevelConverter extends BaseEnumConverter<CommentsLevel> {
		public CommentsLevelConverter() {
			super(CommentsLevel::valueOf, CommentsLevel::values);
		}
	}

	public static class UseKotlinMethodsForVarNamesConverter extends BaseEnumConverter<UseKotlinMethodsForVarNames> {
		public UseKotlinMethodsForVarNamesConverter() {
			super(UseKotlinMethodsForVarNames::valueOf, UseKotlinMethodsForVarNames::values);
		}
	}

	public static class DeobfuscationMapFileModeConverter extends BaseEnumConverter<GeneratedRenamesMappingFileMode> {
		public DeobfuscationMapFileModeConverter() {
			super(GeneratedRenamesMappingFileMode::valueOf, GeneratedRenamesMappingFileMode::values);
		}
	}

	public static class ResourceNameSourceConverter extends BaseEnumConverter<ResourceNameSource> {
		public ResourceNameSourceConverter() {
			super(ResourceNameSource::valueOf, ResourceNameSource::values);
		}
	}

	public static class UseSourceNameAsClassNameConverter extends BaseEnumConverter<UseSourceNameAsClassNameAlias> {
		public UseSourceNameAsClassNameConverter() {
			super(UseSourceNameAsClassNameAlias::valueOf, UseSourceNameAsClassNameAlias::values);
		}
	}

	public static class DecompilationModeConverter extends BaseEnumConverter<DecompilationMode> {
		public DecompilationModeConverter() {
			super(DecompilationMode::valueOf, DecompilationMode::values);
		}
	}

	public static class ExportGradleTypeConverter extends BaseEnumConverter<ExportGradleType> {
		public ExportGradleTypeConverter() {
			super(ExportGradleType::valueOf, ExportGradleType::values);
		}
	}

	public static class LogLevelConverter extends BaseEnumConverter<LogHelper.LogLevelEnum> {
		public LogLevelConverter() {
			super(LogHelper.LogLevelEnum::valueOf, LogHelper.LogLevelEnum::values);
		}
	}

	public static class IntegerFormatConverter extends BaseEnumConverter<IntegerFormat> {
		public IntegerFormatConverter() {
			super(IntegerFormat::valueOf, IntegerFormat::values);
		}
	}

	public abstract static class BaseEnumConverter<E extends Enum<E>> implements IStringConverter<E> {
		private final Function<String, E> parse;
		private final Supplier<E[]> values;

		public BaseEnumConverter(Function<String, E> parse, Supplier<E[]> values) {
			this.parse = parse;
			this.values = values;
		}

		@Override
		public E convert(String value) {
			try {
				return parse.apply(stringAsEnumName(value));
			} catch (Exception e) {
				throw new JadxArgsValidateException(
						'\'' + value + "' is unknown, possible values are: " + enumValuesString(values.get()));
			}
		}
	}

	public static String enumValuesString(Enum<?>[] values) {
		return Stream.of(values)
				.map(v -> v.name().replace('_', '-').toLowerCase(Locale.ROOT))
				.collect(Collectors.joining(", "));
	}

	private static String stringAsEnumName(String value) {
		// inverse of enumValuesString conversion
		return value.replace('-', '_').toUpperCase(Locale.ROOT);
	}
}
