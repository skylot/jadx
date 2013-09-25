package jadx.cli;

import jadx.api.Decompiler;
import jadx.core.utils.ErrorsCounter;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JadxCLI {
	private static final Logger LOG = LoggerFactory.getLogger(JadxCLI.class);

	public static void main(String[] args) {
		try {
			JadxCLIArgs jadxArgs = new JadxCLIArgs(args);
			checkArgs(jadxArgs);
			processAndSave(jadxArgs);
		} catch (Exception e) {
			LOG.error(e.getMessage());
			System.exit(1);
		}
	}

	private static void processAndSave(JadxCLIArgs jadxArgs) {
		try {
			Decompiler jadx = new Decompiler(jadxArgs);
			jadx.loadFiles(jadxArgs.getInput());
			jadx.setOutputDir(jadxArgs.getOutDir());
			jadx.save();
			LOG.info("done");
		} catch (Throwable e) {
			LOG.error("jadx error:", e);
		}
		int errorsCount = ErrorsCounter.getErrorCount();
		if (errorsCount != 0) {
			ErrorsCounter.printReport();
		}
		System.exit(errorsCount);
	}

	private static void checkArgs(JadxCLIArgs jadxArgs) throws Exception {
		if (jadxArgs.getInput().isEmpty()) {
			LOG.error("Please specify input file");
			jadxArgs.printUsage();
			System.exit(1);
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
			throw new Exception("Output directory exists as file " + outputDir);
		}
	}
}
