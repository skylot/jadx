package jadx.cli;

import jadx.api.Decompiler;
import jadx.core.utils.exceptions.JadxException;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JadxCLI {
	private static final Logger LOG = LoggerFactory.getLogger(JadxCLI.class);

	public static void main(String[] args) {
		try {
			JadxCLIArgs jadxArgs = new JadxCLIArgs(args);
			checkArgs(jadxArgs);
			Decompiler jadx = new Decompiler(jadxArgs);
			jadx.processAndSaveAll();
			System.exit(jadx.getErrorsCount());
		} catch (Throwable e) {
			LOG.error(e.getMessage());
			System.exit(1);
		}
	}

	private static void checkArgs(JadxCLIArgs jadxArgs) throws JadxException {
		if (jadxArgs.getInput().isEmpty())
			throw new JadxException("Please specify input file");

		File outputDir = jadxArgs.getOutDir();
		if (outputDir == null) {
			String outDirName;
			File file = jadxArgs.getInput().get(0);
			String name = file.getName();
			int pos = name.lastIndexOf('.');
			if (pos != -1)
				outDirName = name.substring(0, pos);
			else
				outDirName = name + "-jadx-out";

			LOG.info("output directory: " + outDirName);
			outputDir = new File(outDirName);
			jadxArgs.setOutputDir(outputDir);
		}
		if (outputDir.exists() && !outputDir.isDirectory())
			throw new JadxException("Output directory exists as file " + outputDir);
	}
}
