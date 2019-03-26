package jadx.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.core.utils.exceptions.JadxArgsValidateException;
import jadx.core.utils.files.FileUtils;

public class JadxCLI {
	private static final Logger LOG = LoggerFactory.getLogger(JadxCLI.class);

	public static void main(String[] args) {
		int result = 0;
		try {
			JadxCLIArgs jadxArgs = new JadxCLIArgs();
			if (jadxArgs.processArgs(args)) {
				result = processAndSave(jadxArgs);
			}
		} catch (Exception e) {
			LOG.error("jadx error: {}", e.getMessage(), e);
			result = 1;
		} finally {
			System.exit(result);
		}
	}

	static int processAndSave(JadxCLIArgs inputArgs) {
		JadxArgs args = inputArgs.toJadxArgs();
		args.setFsCaseSensitive(getFsCaseSensitivity(args));
		JadxDecompiler jadx = new JadxDecompiler(args);
		try {
			jadx.load();
		} catch (JadxArgsValidateException e) {
			LOG.error("Incorrect arguments: {}", e.getMessage());
			return 1;
		}
		jadx.save();
		int errorsCount = jadx.getErrorsCount();
		if (errorsCount != 0) {
			jadx.printErrorsReport();
			LOG.error("finished with errors");
		} else {
			LOG.info("done");
		}
		return errorsCount;
	}

	private static boolean getFsCaseSensitivity(JadxArgs args) {
		List<File> testDirList = new ArrayList<>(3);
		testDirList.add(args.getOutDir());
		testDirList.add(args.getOutDirSrc());
		if (!args.getInputFiles().isEmpty()) {
			testDirList.add(args.getInputFiles().get(0));
		}
		return FileUtils.isCaseSensitiveFS(testDirList);
	}
}
