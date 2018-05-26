package jadx.cli;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.files.FileUtils;

public class JadxCLIArgs {

	@Parameter(description = "<input file> (.apk, .dex, .jar or .class)")
	protected List<String> files = new ArrayList<>(1);

	@Parameter(names = {"-d", "--output-dir"}, description = "output directory")
	protected String outDir;

	@Parameter(names = {"-ds", "--output-dir-src"}, description = "output directory for sources")
	protected String outDirSrc;

	@Parameter(names = {"-dr", "--output-dir-res"}, description = "output directory for resources")
	protected String outDirRes;

	@Parameter(names = {"-r", "--no-res"}, description = "do not decode resources")
	protected boolean skipResources = false;

	@Parameter(names = {"-s", "--no-src"}, description = "do not decompile source code")
	protected boolean skipSources = false;

	@Parameter(names = {"-e", "--export-gradle"}, description = "save as android gradle project")
	protected boolean exportAsGradleProject = false;

	@Parameter(names = {"-j", "--threads-count"}, description = "processing threads count")
	protected int threadsCount = JadxArgs.DEFAULT_THREADS_COUNT;

	@Parameter(names = {"--show-bad-code"}, description = "show inconsistent code (incorrectly decompiled)")
	protected boolean showInconsistentCode = false;

	@Parameter(names = {"--no-imports"}, description = "disable use of imports, always write entire package name")
	protected boolean useImports = true;

	@Parameter(names = "--no-replace-consts", description = "don't replace constant value with matching constant field")
	protected boolean replaceConsts = true;

	@Parameter(names = {"--escape-unicode"}, description = "escape non latin characters in strings (with \\u)")
	protected boolean escapeUnicode = false;

	@Parameter(names = {"--deobf"}, description = "activate deobfuscation")
	protected boolean deobfuscationOn = false;

	@Parameter(names = {"--deobf-min"}, description = "min length of name, renamed if shorter")
	protected int deobfuscationMinLength = 4;

	@Parameter(names = {"--deobf-max"}, description = "max length of name, renamed if longer")
	protected int deobfuscationMaxLength = 64;

	@Parameter(names = {"--deobf-rewrite-cfg"}, description = "force to save deobfuscation map")
	protected boolean deobfuscationForceSave = false;

	@Parameter(names = {"--deobf-use-sourcename"}, description = "use source file name as class name alias")
	protected boolean deobfuscationUseSourceNameAsAlias = true;

	@Parameter(names = {"--cfg"}, description = "save methods control flow graph to dot file")
	protected boolean cfgOutput = false;

	@Parameter(names = {"--raw-cfg"}, description = "save methods control flow graph (use raw instructions)")
	protected boolean rawCfgOutput = false;

	@Parameter(names = {"-f", "--fallback"}, description = "make simple dump (using goto instead of 'if', 'for', etc)")
	protected boolean fallbackMode = false;

	@Parameter(names = {"-v", "--verbose"}, description = "verbose output")
	protected boolean verbose = false;

	@Parameter(names = {"--version"}, description = "print jadx version")
	protected boolean printVersion = false;

	@Parameter(names = {"-h", "--help"}, description = "print this help", help = true)
	protected boolean printHelp = false;

	public boolean processArgs(String[] args) {
		return parse(args) && process();
	}

	private boolean parse(String[] args) {
		try {
			makeJCommander().parse(args);
			return true;
		} catch (ParameterException e) {
			System.err.println("Arguments parse error: " + e.getMessage());
			printUsage();
			return false;
		}
	}

	private JCommander makeJCommander() {
		return JCommander.newBuilder().addObject(this).build();
	}

	private boolean process() {
		if (printHelp) {
			printUsage();
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
			printUsage();
			return false;
		}
		return true;
	}

	public void printUsage() {
		JCommander jc = makeJCommander();
		// print usage in not sorted fields order (by default its sorted by description)
		PrintStream out = System.out;
		out.println();
		out.println("jadx - dex to java decompiler, version: " + JadxDecompiler.getVersion());
		out.println();
		out.println("usage: jadx [options] " + jc.getMainParameterDescription());
		out.println("options:");

		List<ParameterDescription> params = jc.getParameters();
		Map<String, ParameterDescription> paramsMap = new LinkedHashMap<>(params.size());
		int maxNamesLen = 0;
		for (ParameterDescription p : params) {
			paramsMap.put(p.getParameterized().getName(), p);
			int len = p.getNames().length();
			if (len > maxNamesLen) {
				maxNamesLen = len;
			}
		}
		JadxCLIArgs args = new JadxCLIArgs();
		Field[] fields = args.getClass().getDeclaredFields();
		for (Field f : fields) {
			String name = f.getName();
			ParameterDescription p = paramsMap.get(name);
			if (p == null) {
				continue;
			}
			StringBuilder opt = new StringBuilder();
			opt.append("  ").append(p.getNames());
			addSpaces(opt, maxNamesLen - opt.length() + 3);
			opt.append("- ").append(p.getDescription());
			addDefaultValue(args, f, opt);
			out.println(opt);
		}
		out.println("Example:");
		out.println("  jadx -d out classes.dex");
	}

	private void addDefaultValue(JadxCLIArgs args, Field f, StringBuilder opt) {
		Class<?> fieldType = f.getType();
		if (fieldType == int.class) {
			try {
				int val = f.getInt(args);
				opt.append(" (default: ").append(val).append(")");
			} catch (Exception e) {
				// ignore
			}
		}
	}

	private static void addSpaces(StringBuilder str, int count) {
		for (int i = 0; i < count; i++) {
			str.append(' ');
		}
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
		args.setExportAsGradleProject(exportAsGradleProject);
		args.setUseImports(useImports);
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

	public boolean escapeUnicode() {
		return escapeUnicode;
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

	public boolean isExportAsGradleProject() {
		return exportAsGradleProject;
	}
}
