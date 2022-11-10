package jadx.gui.logs;

import org.jetbrains.annotations.Nullable;

import ch.qos.logback.classic.Level;

import jadx.core.utils.Utils;

import static jadx.plugins.script.runtime.JadxScriptTemplateKt.JADX_SCRIPT_LOG_PREFIX;

public class LogOptions {

	/**
	 * Store latest requested log options
	 */
	private static LogOptions current = new LogOptions(LogMode.ALL, Level.INFO, null);

	public static LogOptions allWithLevel(@Nullable Level logLevel) {
		Level level = Utils.getOrElse(logLevel, current.getLogLevel());
		return store(new LogOptions(LogMode.ALL, level, null));
	}

	public static LogOptions forLevel(@Nullable Level logLevel) {
		Level level = Utils.getOrElse(logLevel, current.getLogLevel());
		return store(new LogOptions(current.getMode(), level, current.getFilter()));
	}

	public static LogOptions forMode(LogMode mode) {
		return store(new LogOptions(mode, current.getLogLevel(), current.getFilter()));
	}

	public static LogOptions forScript(String scriptName) {
		String filter = JADX_SCRIPT_LOG_PREFIX + scriptName;
		return store(new LogOptions(LogMode.CURRENT_SCRIPT, current.getLogLevel(), filter));
	}

	public static LogOptions current() {
		return current;
	}

	private static LogOptions store(LogOptions logOptions) {
		current = logOptions;
		return logOptions;
	}

	private final LogMode mode;
	private final Level logLevel;
	private final @Nullable String filter;

	private LogOptions(LogMode mode, Level logLevel, @Nullable String filter) {
		this.mode = mode;
		this.logLevel = logLevel;
		this.filter = filter;
	}

	public LogMode getMode() {
		return mode;
	}

	public Level getLogLevel() {
		return logLevel;
	}

	public @Nullable String getFilter() {
		return filter;
	}

	@Override
	public String toString() {
		return "LogOptions{mode=" + mode + ", logLevel=" + logLevel + ", filter='" + filter + '\'' + '}';
	}
}
