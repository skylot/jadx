package jadx.api;

import java.io.File;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.utils.exceptions.JadxArgsValidateException;

public class JadxArgsValidator {

	private static final Logger LOG = LoggerFactory.getLogger(JadxArgsValidator.class);

	public static void validate(JadxArgs args) {
		checkInputFiles(args);
		validateOutDirs(args);

		if (LOG.isDebugEnabled()) {
			LOG.debug("Effective jadx args: {}", args);
		}
	}

	private static void checkInputFiles(JadxArgs args) {
		List<File> inputFiles = args.getInputFiles();
		if (inputFiles.isEmpty()) {
			throw new JadxArgsValidateException("Please specify input file");
		}
		if (inputFiles.size() > 1) {
			for (File inputFile : inputFiles) {
				String fileName = inputFile.getName();
				if (fileName.startsWith("--")) {
					throw new JadxArgsValidateException("Unknown argument: " + fileName);
				}
			}
			throw new JadxArgsValidateException("Only one input file supported");
		}
		for (File file : inputFiles) {
			checkFile(file);
		}
	}

	private static void validateOutDirs(JadxArgs args) {
		File outDir = args.getOutDir();
		File srcDir = args.getOutDirSrc();
		File resDir = args.getOutDirRes();
		if (outDir == null) {
			if (srcDir != null) {
				outDir = srcDir;
			} else if (resDir != null) {
				outDir = resDir;
			} else {
				outDir = makeDirFromInput(args);
			}
		}
		args.setOutDir(outDir);
		setFromOut(args);

		checkDir(args.getOutDir());
		checkDir(args.getOutDirSrc());
		checkDir(args.getOutDirRes());
	}

	@NotNull
	private static File makeDirFromInput(JadxArgs args) {
		File outDir;
		String outDirName;
		File file = args.getInputFiles().get(0);
		String name = file.getName();
		int pos = name.lastIndexOf('.');
		if (pos != -1) {
			outDirName = name.substring(0, pos);
		} else {
			outDirName = name + "-" + JadxArgs.DEFAULT_OUT_DIR;
		}
		LOG.info("output directory: {}", outDirName);
		outDir = new File(outDirName);
		return outDir;
	}

	private static void setFromOut(JadxArgs args) {
		if (args.getOutDirSrc() == null) {
			args.setOutDirSrc(new File(args.getOutDir(), JadxArgs.DEFAULT_SRC_DIR));
		}
		if (args.getOutDirRes() == null) {
			args.setOutDirRes(new File(args.getOutDir(), JadxArgs.DEFAULT_RES_DIR));
		}
	}

	private static void checkFile(File file) {
		if (!file.exists()) {
			throw new JadxArgsValidateException("File not found " + file.getAbsolutePath());
		}
		if (file.isDirectory()) {
			throw new JadxArgsValidateException("Expected file but found directory instead: " + file.getAbsolutePath());
		}
	}

	private static void checkDir(File dir) {
		if (dir != null && dir.exists() && !dir.isDirectory()) {
			throw new JadxArgsValidateException("Output directory exists as file " + dir);
		}
	}

	private JadxArgsValidator() {
	}
}
