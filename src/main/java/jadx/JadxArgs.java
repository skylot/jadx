package jadx;

import jadx.utils.exceptions.JadxException;
import jadx.utils.files.InputFile;

import java.io.File;
import java.io.IOException;
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

public class JadxArgs implements IJadxArgs {
	private static final Logger LOG = LoggerFactory.getLogger(JadxArgs.class);

	@Parameter(description = "<input files> (.dex, .apk, .jar or .class)", required = true)
	protected List<String> files;

	@Parameter(names = {"-d", "--output-dir"}, description = "output directory")
	protected String outDirName;

	@Parameter(names = {"-j", "--threads-count"}, description = "processing threads count")
	protected int threadsCount = Runtime.getRuntime().availableProcessors();

	@Parameter(names = {"-f", "--fallback"}, description = "make simple dump (using goto instead of 'if', 'for', etc)", help = true)
	protected boolean fallbackMode = false;

	@Parameter(names = {"--cfg"}, description = "save methods control flow graph")
	protected boolean cfgOutput = false;

	@Parameter(names = {"--raw-cfg"}, description = "save methods control flow graph (use raw instructions)")
	protected boolean rawCfgOutput = false;

	@Parameter(names = {"-v", "--verbose"}, description = "verbose output")
	protected boolean verbose = false;

	@Parameter(names = {"-h", "--help"}, description = "print this help", help = true)
	protected boolean printHelp = false;

	private final List<InputFile> input = new ArrayList<InputFile>();
	private File outputDir;

	public void parse(String[] args) {
		try {
			new JCommander(this, args);
		} catch (ParameterException e) {
			System.out.println("Arguments parse error: " + e.getMessage());
			System.out.println();
			printHelp = true;
		}
	}

	public void processArgs() throws JadxException {
		if (printHelp)
			return;

		if (files == null || files.isEmpty())
			throw new JadxException("Please specify at least one input file");

		for (String fileName : files) {
			File file = new File(fileName);
			if (!file.exists())
				throw new JadxException("File not found: " + file);

			try {
				input.add(new InputFile(file));
			} catch (IOException e) {
				throw new JadxException("File processing error: " + file, e);
			}
		}

		if (input.isEmpty())
			throw new JadxException("No files with correct extension (must be '.dex', '.class' or '.jar')");

		if (threadsCount <= 0)
			throw new JadxException("Threads count must be positive");

		if (outDirName == null) {
			File file = new File(files.get(0));
			String name = file.getName();
			int pos = name.lastIndexOf('.');
			if (pos != -1)
				outDirName = name.substring(0, pos);
			else
				outDirName = name + "-jadx-out";

			LOG.info("output directory: " + outDirName);
		}

		outputDir = new File(outDirName);
		if (!outputDir.exists() && !outputDir.mkdirs())
			throw new JadxException("Can't create directory " + outputDir);
		if (!outputDir.isDirectory())
			throw new JadxException("Output file exists as file " + outputDir);
	}

	public void printUsage() {
		JCommander jc = new JCommander(this);
		// print usage in not sorted fields order (by default its sorted by description)
		PrintStream out = System.out;
		out.println("jadx - dex to java decompiler, version: " + Consts.JADX_VERSION);
		out.println();
		out.println("usage: jadx [options] " + jc.getMainParameterDescription());
		out.println("options:");

		List<ParameterDescription> params = jc.getParameters();

		int maxNamesLen = 0;
		for (ParameterDescription p : params) {
			int len = p.getNames().length();
			if (len > maxNamesLen)
				maxNamesLen = len;
		}

		Field[] fields = this.getClass().getDeclaredFields();
		for (Field f : fields) {
			for (ParameterDescription p : params) {
				if (f.getName().equals(p.getParameterized().getName())) {
					StringBuilder opt = new StringBuilder();
					opt.append(' ').append(p.getNames());
					addSpaces(opt, maxNamesLen - opt.length() + 2);
					opt.append("- ").append(p.getDescription());
					if (p.getParameter().required())
						opt.append(" [required]");
					out.println(opt.toString());
					break;
				}
			}
		}
		out.println("Example:");
		out.println(" jadx -d out classes.dex");
	}

	private static void addSpaces(StringBuilder str, int count) {
		for (int i = 0; i < count; i++)
			str.append(' ');
	}

	@Override
	public File getOutDir() {
		return outputDir;
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
	public List<InputFile> getInput() {
		return input;
	}

	@Override
	public boolean isFallbackMode() {
		return fallbackMode;
	}

	@Override
	public boolean isVerbose() {
		return verbose;
	}

	@Override
	public boolean isPrintHelp() {
		return printHelp;
	}

}
