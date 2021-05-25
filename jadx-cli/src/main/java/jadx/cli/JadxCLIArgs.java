package jadx.cli;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;

import jadx.api.JadxArgs;
import jadx.api.JadxArgs.RenameEnum;
import jadx.api.JadxDecompiler;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.files.FileUtils;

public class JadxCLIArgs {

	@Parameter(description = "<input files> (.apk, .dex, .jar, .class, .smali, .zip, .aar, .arsc, .aab)")
	protected List<String> files = new ArrayList<>(1);

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

	@Parameter(names = { "--single-class" }, description = "decompile a single class")
	protected String singleClass = null;

	@Parameter(names = { "--output-format" }, description = "can be 'java' or 'json'")
	protected String outputFormat = "java";

	@Parameter(names = { "-e", "--export-gradle" }, description = "save as android gradle project")
	protected boolean exportAsGradleProject = false;

	@Parameter(names = { "-j", "--threads-count" }, description = "processing threads count")
	protected int threadsCount = JadxArgs.DEFAULT_THREADS_COUNT;

	@Parameter(names = { "--show-bad-code" }, description = "show inconsistent code (incorrectly decompiled)")
	protected boolean showInconsistentCode = false;

	@Parameter(names = { "--no-imports" }, description = "disable use of imports, always write entire package name")
	protected boolean useImports = true;

	@Parameter(names = { "--no-debug-info" }, description = "disable debug info")
	protected boolean debugInfo = true;

	@Parameter(names = { "--add-debug-lines" }, description = "add comments with debug line numbers if available")
	protected boolean addDebugLines = false;

	@Parameter(names = { "--no-inline-anonymous" }, description = "disable anonymous classes inline")
	protected boolean inlineAnonymousClasses = true;

	@Parameter(names = { "--no-inline-methods" }, description = "disable methods inline")
	protected boolean inlineMethods = true;

	@Parameter(names = "--no-replace-consts", description = "don't replace constant value with matching constant field")
	protected boolean replaceConsts = true;

	@Parameter(names = { "--escape-unicode" }, description = "escape non latin characters in strings (with \\u)")
	protected boolean escapeUnicode = false;

	@Parameter(names = { "--respect-bytecode-access-modifiers" }, description = "don't change original access modifiers")
	protected boolean respectBytecodeAccessModifiers = false;

	@Parameter(names = { "--deobf" }, description = "activate deobfuscation")
	protected boolean deobfuscationOn = false;

	@Parameter(names = { "--deobf-min" }, description = "min length of name, renamed if shorter")
	protected int deobfuscationMinLength = 3;

	@Parameter(names = { "--deobf-max" }, description = "max length of name, renamed if longer")
	protected int deobfuscationMaxLength = 64;

	@Parameter(
			names = { "--deobf-cfg-file" },
			description = "deobfuscation map file, default: same dir and name as input file with '.jobf' extension"
	)
	protected String deobfuscationMapFile;

	@Parameter(names = { "--deobf-rewrite-cfg" }, description = "force to ignore and overwrite deobfuscation map file")
	protected boolean deobfuscationForceSave = false;

	@Parameter(names = { "--deobf-use-sourcename" }, description = "use source file name as class name alias")
	protected boolean deobfuscationUseSourceNameAsAlias = false;

	@Parameter(names = { "--deobf-parse-kotlin-metadata" }, description = "parse kotlin metadata to class and package names")
	protected boolean deobfuscationParseKotlinMetadata = false;

	@Parameter(
			names = { "--rename-flags" },
			description = "fix options (comma-separated list of): "
					+ "\n 'case' - fix case sensitivity issues (according to --fs-case-sensitive option),"
					+ "\n 'valid' - rename java identifiers to make them valid,"
					+ "\n 'printable' - remove non-printable chars from identifiers,"
					+ "\nor single 'none' - to disable all renames"
					+ "\nor single 'all' - to enable all (default)",
			converter = RenameConverter.class
	)
	protected Set<RenameEnum> renameFlags = EnumSet.allOf(RenameEnum.class);

	@Parameter(names = { "--fs-case-sensitive" }, description = "treat filesystem as case sensitive, false by default")
	protected boolean fsCaseSensitive = false;

	@Parameter(names = { "--cfg" }, description = "save methods control flow graph to dot file")
	protected boolean cfgOutput = false;

	@Parameter(names = { "--raw-cfg" }, description = "save methods control flow graph (use raw instructions)")
	protected boolean rawCfgOutput = false;

	@Parameter(names = { "-f", "--fallback" }, description = "make simple dump (using goto instead of 'if', 'for', etc)")
	protected boolean fallbackMode = false;

	@Parameter(
			names = { "--log-level" },
			description = "set log level, values: QUIET, PROGRESS, ERROR, WARN, INFO, DEBUG",
			converter = LogHelper.LogLevelConverter.class
	)
	protected LogHelper.LogLevelEnum logLevel = LogHelper.LogLevelEnum.PROGRESS;

	@Parameter(names = { "-v", "--verbose" }, description = "verbose output (set --log-level to DEBUG)")
	protected boolean verbose = false;

	@Parameter(names = { "-q", "--quiet" }, description = "turn off output (set --log-level to QUIET)")
	protected boolean quiet = false;

	@Parameter(names = { "--version" }, description = "print jadx version")
	protected boolean printVersion = false;

	@Parameter(names = { "-h", "--help" }, description = "print this help", help = true)
	protected boolean printHelp = false;

	public boolean processArgs(String[] args) {
		JCommanderWrapper<JadxCLIArgs> jcw = new JCommanderWrapper<>(this);
		return jcw.parse(args) && process(jcw);
	}

	/**
	 * Set values only for options provided in cmd.
	 * Used to merge saved options and options passed in command line.
	 */
	public boolean overrideProvided(String[] args) {
		JCommanderWrapper<JadxCLIArgs> jcw = new JCommanderWrapper<>(newInstance());
		if (!jcw.parse(args)) {
			return false;
		}
		jcw.overrideProvided(this);
		return process(jcw);
	}

	protected JadxCLIArgs newInstance() {
		return new JadxCLIArgs();
	}

	private boolean process(JCommanderWrapper<JadxCLIArgs> jcw) {
		if (printHelp) {
			jcw.printUsage();
			return false;
		}
		if (printVersion) {
			System.out.println(JadxDecompiler.getVersion());
			return false;
		}
		try {
			if (threadsCount <= 0) {
				throw new JadxException("Threads count must be positive, got: " + threadsCount);
			}
			LogHelper.setLogLevelFromArgs(this);
		} catch (JadxException e) {
			System.err.println("ERROR: " + e.getMessage());
			jcw.printUsage();
			return false;
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
		if (singleClass != null) {
			args.setClassFilter(className -> singleClass.equals(className));
		}
		args.setSkipResources(skipResources);
		args.setFallbackMode(fallbackMode);
		args.setShowInconsistentCode(showInconsistentCode);
		args.setCfgOutput(cfgOutput);
		args.setRawCFGOutput(rawCfgOutput);
		args.setReplaceConsts(replaceConsts);
		args.setDeobfuscationOn(deobfuscationOn);
		args.setDeobfuscationMapFile(FileUtils.toFile(deobfuscationMapFile));
		args.setDeobfuscationForceSave(deobfuscationForceSave);
		args.setDeobfuscationMinLength(deobfuscationMinLength);
		args.setDeobfuscationMaxLength(deobfuscationMaxLength);
		args.setUseSourceNameAsClassAlias(deobfuscationUseSourceNameAsAlias);
		args.setParseKotlinMetadata(deobfuscationParseKotlinMetadata);
		args.setEscapeUnicode(escapeUnicode);
		args.setRespectBytecodeAccModifiers(respectBytecodeAccessModifiers);
		args.setExportAsGradleProject(exportAsGradleProject);
		args.setUseImports(useImports);
		args.setDebugInfo(debugInfo);
		args.setInsertDebugLines(addDebugLines);
		args.setInlineAnonymousClasses(inlineAnonymousClasses);
		args.setInlineMethods(inlineMethods);
		args.setRenameFlags(renameFlags);
		args.setFsCaseSensitive(fsCaseSensitive);
		return args;
	}

	public List<String> getFiles() {
		return files;
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

	public boolean isDeobfuscationOn() {
		return deobfuscationOn;
	}

	public int getDeobfuscationMinLength() {
		return deobfuscationMinLength;
	}

	public int getDeobfuscationMaxLength() {
		return deobfuscationMaxLength;
	}

	public String getDeobfuscationMapFile() {
		return deobfuscationMapFile;
	}

	public boolean isDeobfuscationForceSave() {
		return deobfuscationForceSave;
	}

	public boolean isDeobfuscationUseSourceNameAsAlias() {
		return deobfuscationUseSourceNameAsAlias;
	}

	public boolean isDeobfuscationParseKotlinMetadata() {
		return deobfuscationParseKotlinMetadata;
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
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException(
							'\'' + s + "' is unknown for parameter " + paramName
									+ ", possible values are " + enumValuesString(RenameEnum.values()));
				}
			}
			return set;
		}
	}

	public static String enumValuesString(Enum<?>[] values) {
		return Stream.of(values)
				.map(v -> '\'' + v.name().toLowerCase(Locale.ROOT) + '\'')
				.collect(Collectors.joining(", "));
	}
}
