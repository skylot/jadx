package jadx.cli;

import jadx.api.Decompiler;
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.exceptions.JadxException;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JadxCLI {
	private static final Logger LOG = LoggerFactory.getLogger(JadxCLI.class);

	public static void main(String[] args) {
		try {
			JadxCLIArgs jadxArgs = new JadxCLIArgs();
			if (processArgs(jadxArgs, args)) {
				processAndSave(jadxArgs);
			}
		} catch (JadxException e) {
			LOG.error(e.getMessage());
			System.exit(1);
		}
	}

	static void processAndSave(JadxCLIArgs jadxArgs) throws JadxException {
		try {
			Decompiler jadx = new Decompiler(jadxArgs);
			jadx.loadFiles(jadxArgs.getInput());
			jadx.setOutputDir(jadxArgs.getOutDir());
			jadx.save();
		} catch (Throwable e) {
			throw new JadxException("jadx error: " + e.getMessage(), e);
		}
		if (ErrorsCounter.getErrorCount() != 0) {
			ErrorsCounter.printReport();
			throw new JadxException("finished with errors");
		}
		LOG.info("done");

	}

	static boolean processArgs(JadxCLIArgs jadxArgs, String[] args) throws JadxException {
		if (!jadxArgs.processArgs(args)) {
			return false;
		}
		if (jadxArgs.getInput().isEmpty()) {
			LOG.error("Please specify input file");
			jadxArgs.printUsage();
			return false;
		}
		File outputDir = jadxArgs.getOutDir();
		if (outputDir == null) {
			String outDirName;
			File file = jadxArgs.getInput().get(0);
			String name = file.getName();
			int pos = name.lastIndexOf('.');
			if (pos != -1) {
				outDirName = name.substring(0, pos);
			} else {
				outDirName = name + "-jadx-out";
			}
			LOG.info("output directory: " + outDirName);
			outputDir = new File(outDirName);
			jadxArgs.setOutputDir(outputDir);
		}
		if (outputDir.exists() && !outputDir.isDirectory()) {
			throw new JadxException("Output directory exists as file " + outputDir);
		}
		return true;
	}
}
