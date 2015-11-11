package jadx.core.clsp;

import jadx.api.JadxArgs;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.files.InputFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

		List<InputFile> inputFiles = new ArrayList<InputFile>(args.length - 1);
		for (int i = 1; i < args.length; i++) {
			File f = new File(args[i]);
			if (f.isDirectory()) {
				addFilesFromDirectory(f, inputFiles);
			} else {
				InputFile inputFile = new InputFile(f);
				inputFiles.add(inputFile);
				while (inputFile.nextDexIndex != -1) {
					inputFile = new InputFile(f, inputFile.nextDexIndex);
					inputFiles.add(inputFile);
				}
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

	private static void addFilesFromDirectory(File dir,
			List<InputFile> inputFiles) throws IOException, DecodeException {
		File[] files = dir.listFiles();
		if (files == null) {
			return;
		}
		for (File file : files) {
			if (file.isDirectory()) {
				addFilesFromDirectory(file, inputFiles);
			}
			String fileName = file.getName();
			if (fileName.endsWith(".dex")
					|| fileName.endsWith(".jar")
					|| fileName.endsWith(".apk")) {
				InputFile inputFile = new InputFile(file);
				inputFiles.add(inputFile);
				while (inputFile.nextDexIndex != -1) {
					inputFile = new InputFile(file, inputFile.nextDexIndex);
					inputFiles.add(inputFile);
				}
			}
		}
	}

}
