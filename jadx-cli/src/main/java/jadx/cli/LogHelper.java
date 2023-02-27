package jadx.cli;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import jadx.api.JadxDecompiler;

public class LogHelper {
	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(LogHelper.class);

	public enum LogLevelEnum {
		QUIET(Level.OFF),
		PROGRESS(Level.OFF),
		ERROR(Level.ERROR),
		WARN(Level.WARN),
		INFO(Level.INFO),
		DEBUG(Level.DEBUG);

		private final Level level;

		LogLevelEnum(Level level) {
			this.level = level;
		}

		public Level getLevel() {
			return level;
		}
	}

	@Nullable("For disable log level control")
	private static LogLevelEnum logLevelValue;

	public static void initLogLevel(JadxCLIArgs args) {
		logLevelValue = getLogLevelFromArgs(args);
	}

	private static LogLevelEnum getLogLevelFromArgs(JadxCLIArgs args) {
		if (isCustomLogConfig()) {
			return null;
		}
		if (args.quiet) {
			return LogLevelEnum.QUIET;
		}
		if (args.verbose) {
			return LogLevelEnum.DEBUG;
		}
		return args.logLevel;
	}

	public static void setLogLevelsForLoadingStage() {
		if (logLevelValue == null) {
			return;
		}
		if (logLevelValue == LogLevelEnum.PROGRESS) {
			// show load errors
			LogHelper.applyLogLevel(LogLevelEnum.ERROR);
			fixForShowProgress();
			return;
		}
		applyLogLevel(logLevelValue);
	}

	public static void setLogLevelsForDecompileStage() {
		if (logLevelValue == null) {
			return;
		}
		applyLogLevel(logLevelValue);
		if (logLevelValue == LogLevelEnum.PROGRESS) {
			fixForShowProgress();
		}
	}

	/**
	 * Show progress: change to 'INFO' for control classes
	 */
	private static void fixForShowProgress() {
		setLevelForClass(JadxCLI.class, Level.INFO);
		setLevelForClass(JadxDecompiler.class, Level.INFO);
		setLevelForClass(SingleClassMode.class, Level.INFO);
	}

	private static void applyLogLevel(@NotNull LogLevelEnum logLevel) {
		Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		rootLogger.setLevel(logLevel.getLevel());
	}

	@Nullable
	public static LogLevelEnum getLogLevel() {
		return logLevelValue;
	}

	public static void setLevelForClass(Class<?> cls, Level level) {
		((Logger) LoggerFactory.getLogger(cls)).setLevel(level);
	}

	public static void setLevelForPackage(String pkgName, Level level) {
		((Logger) LoggerFactory.getLogger(pkgName)).setLevel(level);
	}

	/**
	 * Try to detect if user provide custom logback config via -Dlogback.configurationFile=
	 */
	private static boolean isCustomLogConfig() {
		try {
			String logbackConfig = System.getProperty("logback.configurationFile");
			if (logbackConfig == null) {
				return false;
			}
			LOG.debug("Use custom log config: {}", logbackConfig);
			return true;
		} catch (Exception e) {
			LOG.error("Failed to detect custom log config", e);
		}
		return false;
	}
}
