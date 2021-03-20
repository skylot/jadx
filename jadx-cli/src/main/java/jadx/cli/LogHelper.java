package jadx.cli;

import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.IStringConverter;

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

	private static LogLevelEnum logLevelValue;

	public static void setLogLevelFromArgs(JadxCLIArgs args) {
		if (isCustomLogConfig()) {
			return;
		}
		LogLevelEnum logLevel = args.logLevel;
		if (args.quiet) {
			logLevel = LogLevelEnum.QUIET;
		} else if (args.verbose) {
			logLevel = LogLevelEnum.DEBUG;
		}

		applyLogLevel(logLevel);
	}

	public static void applyLogLevel(LogLevelEnum logLevel) {
		logLevelValue = logLevel;

		Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		rootLogger.setLevel(logLevel.getLevel());

		if (logLevel != LogLevelEnum.QUIET) {
			// show progress for all levels except quiet
			setLevelForClass(JadxCLI.class, Level.INFO);
			setLevelForClass(JadxDecompiler.class, Level.INFO);
		}
	}

	@Nullable
	public static LogLevelEnum getLogLevel() {
		return logLevelValue;
	}

	private static void setLevelForClass(Class<?> cls, Level level) {
		((Logger) LoggerFactory.getLogger(cls)).setLevel(level);
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

	public static class LogLevelConverter implements IStringConverter<LogLevelEnum> {

		@Override
		public LogLevelEnum convert(String value) {
			try {
				return LogLevelEnum.valueOf(value.toUpperCase());
			} catch (Exception e) {
				throw new IllegalArgumentException(
						'\'' + value + "' is unknown log level, possible values are "
								+ JadxCLIArgs.enumValuesString(LogLevelEnum.values()));
			}
		}
	}
}
