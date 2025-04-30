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
import jadx.core.deobf.conditions.DeobfWhitelist;
import jadx.core.export.ExportGradleType;
import jadx.core.utils.exceptions.JadxArgsValidateException;
import jadx.core.utils.files.FileUtils;

public class JadxCLIArgs {

	@Parameter(description = "<input files> (.apk, .dex, .jar, .class, .smali, .zip, .aar, .arsc, .aab, .xapk, .apkm, .jadx.kts)")
	protected List<String> files = Collections.emptyList();

	@Parameter(names = { "-d", "--output-dir" }, description = "output directory")
	protected String outDir;

	@Parameter(names = { "-ds", "--output-dir-src" }, description = "output directory for sources")
	protected String outDirSrc;

	@Parameter(names = { "-dr", "--output-dir-res" }, description = "output directory for resources")
	protected String outDirRes;

	@Parameter(names = { "-r", "--no-res" }, description = "do not decode resources")
	protected boolean skipResources = false;

	@Parameter(names = { "-s", "--no-src" }, description = "do not decompile source code")
	protected boolean skipSources = false;

	@Parameter(names = { "-j", "--threads-count" }, description = "processing threads count")
	protected int threadsCount = JadxArgs.DEFAULT_THREADS_COUNT;

	@Parameter(names = { "--single-class" }, description = "decompile a single class, full name, raw or alias")
	protected String singleClass = null;

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
			names = { "--rename-flags" },
			description = "fix options (comma-separated list of):"
					+ "\n 'case' - fix case sensitivity issues (according to --fs-case-sensitive option),"
					+ "\n 'valid' - rename java identifiers to make them valid,"
					+ "\n 'printable' - remove non-printable chars from identifiers,"
					+ "\nor single 'none' - to disable all renames"
					+ "\nor single 'all' - to enable all (default)",
			converter = RenameConverter.class
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

	@Parameter(names = { "-v", "--verbose" }, description = "verbose output (set --log-level to DEBUG)")
	protected boolean verbose = false;

	@Parameter(names = { "-q", "--quiet" }, description = "turn off output (set --log-level to QUIET)")
	protected boolean quiet = false;

	@Parameter(names = { "--disable-plugins" }, description = "comma separated list of plugin ids to disable")
	protected String disablePlugins = "";

	@Parameter(names = { "--version" }, description = "print jadx version")
	protected boolean printVersion = false;

	@Parameter(names = { "-h", "--help" }, description = "print this help", help = true)
	protected boolean printHelp = false;

	@DynamicParameter(names = "-P", description = "Plugin options", hidden = true)
	protected Map<String, String> pluginOptions = new HashMap<>();

	public boolean processArgs(String[] args) {
		JCommanderWrapper jcw = new JCommanderWrapper(this);
		return jcw.parse(args) && process(jcw);
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
		if (threadsCount <= 0) {
			throw new JadxArgsValidateException("Threads count must be positive, got: " + threadsCount);
		}
		for (String fileName : files) {
			if (fileName.startsWith("-")) {
				throw new JadxArgsValidateException("Unknown option: " + fileName);
			}
		}
		return true;
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
		args.setRenameFlags(renameFlags);
		args.setFsCaseSensitive(fsCaseSensitive);
		args.setCommentsLevel(commentsLevel);
		args.setIntegerFormat(integerFormat);
		args.setUseDxInput(useDx);
		args.setPluginOptions(pluginOptions);
		args.setDisabledPlugins(Arrays.stream(disablePlugins.split(",")).map(String::trim).collect(Collectors.toSet()));
		return args;
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

	public boolean isSkipSources() {
		return skipSources;
	}

	public int getThreadsCount() {
		return threadsCount;
	}

	public boolean isFallbackMode() {
		return fallbackMode;
	}

	public boolean isUseDx() {
		return useDx;
	}

	public DecompilationMode getDecompilationMode() {
		return decompilationMode;
	}

	public boolean isShowInconsistentCode() {
		return showInconsistentCode;
	}

	public boolean isUseImports() {
		return useImports;
	}

	public boolean isDebugInfo() {
		return debugInfo;
	}

	public boolean isAddDebugLines() {
		return addDebugLines;
	}

	public boolean isInlineAnonymousClasses() {
		return inlineAnonymousClasses;
	}

	public boolean isInlineMethods() {
		return inlineMethods;
	}

	public boolean isMoveInnerClasses() {
		return moveInnerClasses;
	}

	public boolean isAllowInlineKotlinLambda() {
		return allowInlineKotlinLambda;
	}

	public boolean isExtractFinally() {
		return extractFinally;
	}

	public boolean isRestoreSwitchOverString() {
		return restoreSwitchOverString;
	}

	public Path getUserRenamesMappingsPath() {
		return userRenamesMappingsPath;
	}

	public UserRenamesMappingsMode getUserRenamesMappingsMode() {
		return userRenamesMappingsMode;
	}

	public boolean isDeobfuscationOn() {
		return deobfuscationOn;
	}

	public int getDeobfuscationMinLength() {
		return deobfuscationMinLength;
	}

	public int getDeobfuscationMaxLength() {
		return deobfuscationMaxLength;
	}

	public String getDeobfuscationWhitelistStr() {
		return deobfuscationWhitelistStr;
	}

	public String getGeneratedRenamesMappingFile() {
		return generatedRenamesMappingFile;
	}

	public GeneratedRenamesMappingFileMode getGeneratedRenamesMappingFileMode() {
		return generatedRenamesMappingFileMode;
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

	public int getSourceNameRepeatLimit() {
		return sourceNameRepeatLimit;
	}

	/**
	 * @deprecated Use {@link #getUseSourceNameAsClassNameAlias()} instead.
	 */
	@Deprecated
	public boolean isDeobfuscationUseSourceNameAsAlias() {
		return getUseSourceNameAsClassNameAlias().toBoolean();
	}

	public ResourceNameSource getResourceNameSource() {
		return resourceNameSource;
	}

	public UseKotlinMethodsForVarNames getUseKotlinMethodsForVarNames() {
		return useKotlinMethodsForVarNames;
	}

	public IntegerFormat getIntegerFormat() {
		return integerFormat;
	}

	public boolean isEscapeUnicode() {
		return escapeUnicode;
	}

	public boolean isCfgOutput() {
		return cfgOutput;
	}

	public boolean isRawCfgOutput() {
		return rawCfgOutput;
	}

	public boolean isReplaceConsts() {
		return replaceConsts;
	}

	public boolean isRespectBytecodeAccessModifiers() {
		return respectBytecodeAccessModifiers;
	}

	public boolean isExportAsGradleProject() {
		return exportAsGradleProject;
	}

	public boolean isSkipXmlPrettyPrint() {
		return skipXmlPrettyPrint;
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

	public CommentsLevel getCommentsLevel() {
		return commentsLevel;
	}

	public LogHelper.LogLevelEnum getLogLevel() {
		return logLevel;
	}

	public Map<String, String> getPluginOptions() {
		return pluginOptions;
	}

	public String getDisablePlugins() {
		return disablePlugins;
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
