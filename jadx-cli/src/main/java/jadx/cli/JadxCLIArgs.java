package jadx.cli;

import jadx.api.IJadxArgs;
import jadx.api.JadxDecompiler;
import jadx.core.utils.exceptions.JadxException;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;

public final class JadxCLIArgs implements IJadxArgs {

	@Parameter(description = "<input file> (.dex, .apk, .jar or .class)")
	protected List<String> files;

	@Parameter(names = {"-d", "--output-dir"}, description = "output directory")
	protected String outDirName;

	@Parameter(names = {"-j", "--threads-count"}, description = "processing threads count")
	protected int threadsCount = Runtime.getRuntime().availableProcessors();

	@Parameter(names = {"-f", "--fallback"}, description = "make simple dump (using goto instead of 'if', 'for', etc)")
	protected boolean fallbackMode = false;

	@Parameter(names = {"-r", "--no-res"}, description = "do not decode resources")
	protected boolean skipResources = false;

	@Parameter(names = {"-s", "--no-src"}, description = "do not decompile source code")
	protected boolean skipSources = false;

	@Parameter(names = {"--show-bad-code"}, description = "show inconsistent code (incorrectly decompiled)")
	protected boolean showInconsistentCode = false;

	@Parameter(names = {"--cfg"}, description = "save methods control flow graph to dot file")
	protected boolean cfgOutput = false;

	@Parameter(names = {"--raw-cfg"}, description = "save methods control flow graph (use raw instructions)")
	protected boolean rawCfgOutput = false;

	@Parameter(names = {"-v", "--verbose"}, description = "verbose output")
	protected boolean verbose = false;

	@Parameter(names = {"-h", "--help"}, description = "print this help", help = true)
	protected boolean printHelp = false;

	private final List<File> input = new ArrayList<File>(1);
	private File outputDir;

	public boolean processArgs(String[] args) {
		return parse(args) && process();
	}

	private boolean parse(String[] args) {
		try {
			new JCommander(this, args);
			return true;
		} catch (ParameterException e) {
			System.err.println("Arguments parse error: " + e.getMessage());
			printUsage();
			return false;
		}
	}

	private boolean process() {
		if (isPrintHelp()) {
			printUsage();
			return false;
		}
		try {
			if (threadsCount <= 0) {
				throw new JadxException("Threads count must be positive");
			}
			if (files != null) {
				for (String fileName : files) {
					File file = new File(fileName);
					if (file.exists()) {
						input.add(file);
					} else {
						throw new JadxException("File not found: " + file);
					}
				}
			}
			if (input.size() > 1) {
				throw new JadxException("Only one input file is supported");
			}
			if (outDirName != null) {
				outputDir = new File(outDirName);
			}
			if (isVerbose()) {
				ch.qos.logback.classic.Logger rootLogger =
						(ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
				rootLogger.setLevel(ch.qos.logback.classic.Level.DEBUG);
			}
		} catch (JadxException e) {
			System.err.println("ERROR: " + e.getMessage());
			printUsage();
			return false;
		}
		return true;
	}

	public void printUsage() {
		JCommander jc = new JCommander(this);
		// print usage in not sorted fields order (by default its sorted by description)
		PrintStream out = System.out;
		out.println();
		out.println("jadx - dex to java decompiler, version: " + JadxDecompiler.getVersion());
		out.println();
		out.println("usage: jadx [options] " + jc.getMainParameterDescription());
		out.println("options:");

		List<ParameterDescription> params = jc.getParameters();

		int maxNamesLen = 0;
		for (ParameterDescription p : params) {
			int len = p.getNames().length();
			if (len > maxNamesLen) {
				maxNamesLen = len;
			}
		}

		Field[] fields = this.getClass().getDeclaredFields();
		for (Field f : fields) {
			for (ParameterDescription p : params) {
				String name = f.getName();
				if (name.equals(p.getParameterized().getName())) {
					StringBuilder opt = new StringBuilder();
					opt.append(' ').append(p.getNames());
					addSpaces(opt, maxNamesLen - opt.length() + 2);
					opt.append("- ").append(p.getDescription());
					out.println(opt.toString());
					break;
				}
			}
		}
		out.println("Example:");
		out.println(" jadx -d out classes.dex");
	}

	private static void addSpaces(StringBuilder str, int count) {
		for (int i = 0; i < count; i++) {
			str.append(' ');
		}
	}

	public List<File> getInput() {
		return input;
	}

	@Override
	public File getOutDir() {
		return outputDir;
	}

	public void setOutputDir(File outputDir) {
		this.outputDir = outputDir;
	}

	public boolean isPrintHelp() {
		return printHelp;
	}

	@Override
	public boolean isSkipResources() {
		return skipResources;
	}

	@Override
	public boolean isSkipSources() {
		return skipSources;
	}

	@Override
	public int getThreadsCount() {
		return threadsCount;
	}

	@Override
	public boolean isCFGOutput() {
		return cfgOutput;
	}

	@Override
	public boolean isRawCFGOutput() {
		return rawCfgOutput;
	}

	@Override
	public boolean isFallbackMode() {
		return fallbackMode;
	}

	@Override
	public boolean isShowInconsistentCode() {
		return showInconsistentCode;
	}

	@Override
	public boolean isVerbose() {
		return verbose;
	}
}
