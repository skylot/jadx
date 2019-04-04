package jadx.core.clsp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxArgs;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.files.InputFile;

/**
 * Utility class for convert dex or jar to jadx classes set (.jcst)
 */
public class ConvertToClsSet {
	private static final Logger LOG = LoggerFactory.getLogger(ConvertToClsSet.class);

	public static void usage() {
		LOG.info("<output .jcst or .jar file> <several input dex or jar files> ");
	}

	public static void main(String[] args) throws IOException, DecodeException {
		if (args.length < 2) {
			usage();
			System.exit(1);
		}
		File output = new File(args[0]);

		List<InputFile> inputFiles = new ArrayList<>(args.length - 1);
		for (int i = 1; i < args.length; i++) {
			File f = new File(args[i]);
			if (f.isDirectory()) {
				addFilesFromDirectory(f, inputFiles);
			} else {
				InputFile.addFilesFrom(f, inputFiles);
			}
		}
		for (InputFile inputFile : inputFiles) {
			LOG.info("Loaded: {}", inputFile.getFile());
		}

		RootNode root = new RootNode(new JadxArgs());
		root.load(inputFiles);

		ClsSet set = new ClsSet();
		set.load(root);
		set.save(output);
		LOG.info("Output: {}", output);
		LOG.info("done");
	}

	private static void addFilesFromDirectory(File dir, List<InputFile> inputFiles) {
		File[] files = dir.listFiles();
		if (files == null) {
			return;
		}
		for (File file : files) {
			if (file.isDirectory()) {
				addFilesFromDirectory(file, inputFiles);
			} else {
				try {
					InputFile.addFilesFrom(file, inputFiles);
				} catch (Exception e) {
					LOG.warn("Skip file: {}, load error: {}", file, e.getMessage());
				}
			}
		}
	}
}
