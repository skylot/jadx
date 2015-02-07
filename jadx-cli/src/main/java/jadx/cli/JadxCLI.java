package jadx.cli;

import jadx.api.JadxDecompiler;
import jadx.core.utils.exceptions.JadxException;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JadxCLI {
	private static final Logger LOG = LoggerFactory.getLogger(JadxCLI.class);

	public static void main(String[] args) throws JadxException {
		try {
			JadxCLIArgs jadxArgs = new JadxCLIArgs();
			if (processArgs(jadxArgs, args)) {
				processAndSave(jadxArgs);
			}
		} catch (Throwable e) {
			LOG.error("jadx error: {}", e.getMessage(), e);
			System.exit(1);
		}
	}

	static void processAndSave(JadxCLIArgs jadxArgs) throws JadxException {
		JadxDecompiler jadx = new JadxDecompiler(jadxArgs);
		jadx.setOutputDir(jadxArgs.getOutDir());
		jadx.loadFiles(jadxArgs.getInput());
		jadx.save();
		if (jadx.getErrorsCount() != 0) {
			jadx.printErrorsReport();
			LOG.error("finished with errors");
		} else {
			LOG.info("done");
		}
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
			LOG.info("output directory: {}", outDirName);
			outputDir = new File(outDirName);
			jadxArgs.setOutputDir(outputDir);
		}
		if (outputDir.exists() && !outputDir.isDirectory()) {
			throw new JadxException("Output directory exists as file " + outputDir);
		}
		return true;
	}
}
