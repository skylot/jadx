package jadx.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.impl.NoOpCodeCache;
import jadx.core.utils.exceptions.JadxArgsValidateException;

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
		args.setCodeCache(new NoOpCodeCache());
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
			LOG.error("finished with errors, count: {}", errorsCount);
		} else {
			LOG.info("done");
		}
		return 0;
	}
}
