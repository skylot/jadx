package jadx.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.impl.NoOpCodeCache;
import jadx.api.impl.SimpleCodeWriter;
import jadx.core.utils.exceptions.JadxArgsValidateException;
import jadx.core.utils.files.FileUtils;

public class JadxCLI {
	private static final Logger LOG = LoggerFactory.getLogger(JadxCLI.class);

	public static void main(String[] args) {
		int result = 0;
		try {
			result = execute(args);
		} catch (JadxArgsValidateException e) {
			LOG.error("Incorrect arguments: {}", e.getMessage());
			result = 1;
		} catch (Exception e) {
			LOG.error("jadx error: {}", e.getMessage(), e);
			result = 1;
		} finally {
			FileUtils.deleteTempRootDir();
			System.exit(result);
		}
	}

	public static int execute(String[] args) {
		JadxCLIArgs jadxArgs = new JadxCLIArgs();
		if (jadxArgs.processArgs(args)) {
			return processAndSave(jadxArgs.toJadxArgs());
		}
		return 0;
	}

	private static int processAndSave(JadxArgs jadxArgs) {
		jadxArgs.setCodeCache(new NoOpCodeCache());
		jadxArgs.setCodeWriterProvider(SimpleCodeWriter::new);
		try (JadxDecompiler jadx = new JadxDecompiler(jadxArgs)) {
			jadx.load();
			if (LogHelper.getLogLevel() == LogHelper.LogLevelEnum.QUIET) {
				jadx.save();
			} else {
				jadx.save(500, (done, total) -> {
					int progress = (int) (done * 100.0 / total);
					System.out.printf("INFO  - progress: %d of %d (%d%%)\r", done, total, progress);
				});
			}
			int errorsCount = jadx.getErrorsCount();
			if (errorsCount != 0) {
				jadx.printErrorsReport();
				LOG.error("finished with errors, count: {}", errorsCount);
			} else {
				LOG.info("done");
			}
		}
		return 0;
	}
}
