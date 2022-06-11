package jadx.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.impl.NoOpCodeCache;
import jadx.api.impl.SimpleCodeWriter;
import jadx.cli.LogHelper.LogLevelEnum;
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
		} catch (Throwable e) {
			LOG.error("Process error:", e);
			result = 1;
		} finally {
			FileUtils.deleteTempRootDir();
			System.exit(result);
		}
	}

	public static int execute(String[] args) {
		JadxCLIArgs jadxArgs = new JadxCLIArgs();
		if (jadxArgs.processArgs(args)) {
			return processAndSave(jadxArgs);
		}
		return 0;
	}

	private static int processAndSave(JadxCLIArgs cliArgs) {
		LogHelper.initLogLevel(cliArgs);
		LogHelper.setLogLevelsForLoadingStage();
		JadxArgs jadxArgs = cliArgs.toJadxArgs();
		jadxArgs.setCodeCache(new NoOpCodeCache());
		jadxArgs.setCodeWriterProvider(SimpleCodeWriter::new);
		try (JadxDecompiler jadx = new JadxDecompiler(jadxArgs)) {
			jadx.load();
			if (checkForErrors(jadx)) {
				return 1;
			}
			LogHelper.setLogLevelsForDecompileStage();
			if (!SingleClassMode.process(jadx, cliArgs)) {
				save(jadx);
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

	private static boolean checkForErrors(JadxDecompiler jadx) {
		if (jadx.getRoot().getClasses().isEmpty()) {
			if (jadx.getArgs().isSkipResources()) {
				LOG.error("Load failed! No classes for decompile!");
				return true;
			}
			if (!jadx.getArgs().isSkipSources()) {
				LOG.warn("No classes to decompile; decoding resources only");
				jadx.getArgs().setSkipSources(true);
			}
		}
		if (jadx.getErrorsCount() > 0) {
			LOG.error("Load with errors! Check log for details");
			// continue processing
			return false;
		}
		return false;
	}

	private static void save(JadxDecompiler jadx) {
		if (LogHelper.getLogLevel() == LogLevelEnum.QUIET) {
			jadx.save();
		} else {
			jadx.save(500, (done, total) -> {
				int progress = (int) (done * 100.0 / total);
				System.out.printf("INFO  - progress: %d of %d (%d%%)\r", done, total, progress);
			});
			// dumb line clear :)
			System.out.print("                                                             \r");
		}
	}
}
