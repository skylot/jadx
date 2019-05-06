package jadx.cli;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;

import jadx.api.JadxArgs;
import jadx.api.JadxArgs.RenameEnum;
import jadx.api.JadxDecompiler;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.files.FileUtils;

public class JadxCLIArgs {

	@Parameter(description = "<input file> (.apk, .dex, .jar, .class, .smali, .zip, .aar, .arsc)")
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

	@Parameter(names = { "--no-inline-anonymous" }, description = "disable anonymous classes inline")
	protected boolean inlineAnonymousClasses = true;

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

	@Parameter(names = { "--deobf-rewrite-cfg" }, description = "force to save deobfuscation map")
	protected boolean deobfuscationForceSave = false;

	@Parameter(names = { "--deobf-use-sourcename" }, description = "use source file name as class name alias")
	protected boolean deobfuscationUseSourceNameAsAlias = true;

	@Parameter(names = { "--cfg" }, description = "save methods control flow graph to dot file")
	protected boolean cfgOutput = false;

	@Parameter(names = { "--raw-cfg" }, description = "save methods control flow graph (use raw instructions)")
	protected boolean rawCfgOutput = false;

	@Parameter(names = { "-f", "--fallback" }, description = "make simple dump (using goto instead of 'if', 'for', etc)")
	protected boolean fallbackMode = false;

	@Parameter(
			names = { "--rename-flags" },
			description = "what to rename, comma-separated,"
					+ " 'case' for system case sensitivity,"
					+ " 'valid' for java identifiers,"
					+ " 'printable' characters,"
					+ " 'none' or 'all'",
			converter = RenameConverter.class
	)
	protected Set<RenameEnum> renameFlags = EnumSet.allOf(RenameEnum.class);

	@Parameter(names = { "-v", "--verbose" }, description = "verbose output")
	protected boolean verbose = false;

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
		JCommanderWrapper<JadxCLIArgs> jcw = new JCommanderWrapper<>(new JadxCLIArgs());
		if (!jcw.parse(args)) {
			return false;
		}
		jcw.overrideProvided(this);
		return process(jcw);
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
			if (verbose) {
				ch.qos.logback.classic.Logger rootLogger =
						(ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
				// remove INFO ThresholdFilter
				Appender<ILoggingEvent> appender = rootLogger.getAppender("STDOUT");
				if (appender != null) {
					appender.clearAllFilters();
				}
			}
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
		args.setThreadsCount(threadsCount);
		args.setSkipSources(skipSources);
		args.setSkipResources(skipResources);
		args.setFallbackMode(fallbackMode);
		args.setShowInconsistentCode(showInconsistentCode);
		args.setCfgOutput(cfgOutput);
		args.setRawCFGOutput(rawCfgOutput);
		args.setReplaceConsts(replaceConsts);
		args.setDeobfuscationOn(deobfuscationOn);
		args.setDeobfuscationForceSave(deobfuscationForceSave);
		args.setDeobfuscationMinLength(deobfuscationMinLength);
		args.setDeobfuscationMaxLength(deobfuscationMaxLength);
		args.setUseSourceNameAsClassAlias(deobfuscationUseSourceNameAsAlias);
		args.setEscapeUnicode(escapeUnicode);
		args.setRespectBytecodeAccModifiers(respectBytecodeAccessModifiers);
		args.setExportAsGradleProject(exportAsGradleProject);
		args.setUseImports(useImports);
		args.setDebugInfo(debugInfo);
		args.setInlineAnonymousClasses(inlineAnonymousClasses);
		args.setRenameCaseSensitive(isRenameCaseSensitive());
		args.setRenameValid(isRenameValid());
		args.setRenamePrintable(isRenamePrintable());
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

	public boolean isInlineAnonymousClasses() {
		return inlineAnonymousClasses;
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

	public boolean isDeobfuscationForceSave() {
		return deobfuscationForceSave;
	}

	public boolean isDeobfuscationUseSourceNameAsAlias() {
		return deobfuscationUseSourceNameAsAlias;
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

	public void setRenameCaseSensitive(boolean renameCase) {
		if (renameCase && !isRenameCaseSensitive()) {
			renameFlags.add(RenameEnum.CASE);
		} else if (!renameCase && isRenameCaseSensitive()) {
			renameFlags.remove(RenameEnum.CASE);
		}
	}

	public boolean isRenameValid() {
		return renameFlags.contains(RenameEnum.VALID);
	}

	public void setRenameValid(boolean renameValid) {
		if (renameValid && !isRenameValid()) {
			renameFlags.add(RenameEnum.VALID);
		} else if (!renameValid && isRenameValid()) {
			renameFlags.remove(RenameEnum.VALID);
		}
	}

	public boolean isRenamePrintable() {
		return renameFlags.contains(RenameEnum.PRINTABLE);
	}

	public void setRenamePrintable(boolean renamePrintable) {
		if (renamePrintable && !isRenamePrintable()) {
			renameFlags.add(RenameEnum.PRINTABLE);
		} else if (!renamePrintable && isRenamePrintable()) {
			renameFlags.remove(RenameEnum.PRINTABLE);
		}
	}

	static class RenameConverter implements IStringConverter<Set<RenameEnum>> {

		private final String paramName;

		RenameConverter(String paramName) {
			this.paramName = paramName;
		}

		@Override
		public Set<RenameEnum> convert(String value) {
			Set<RenameEnum> set = new HashSet<>();
			if (value.equalsIgnoreCase("ALL")) {
				set.add(RenameEnum.CASE);
				set.add(RenameEnum.VALID);
				set.add(RenameEnum.PRINTABLE);
			} else if (!value.equalsIgnoreCase("NONE")) {
				for (String s : value.split(",")) {
					try {
						set.add(RenameEnum.valueOf(s.toUpperCase(Locale.ROOT)));
					} catch (IllegalArgumentException e) {
						String values = "'" + RenameEnum.CASE
								+ "', '" + RenameEnum.VALID
								+ "' and '" + RenameEnum.PRINTABLE + '\'';
						throw new IllegalArgumentException(
								s + " is unknown for parameter " + paramName
										+ ", possible values are " + values.toLowerCase(Locale.ROOT));
					}
				}
			}
			return set;
		}
	}
}
