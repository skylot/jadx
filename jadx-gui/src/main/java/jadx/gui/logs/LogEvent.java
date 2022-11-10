package jadx.gui.logs;

import ch.qos.logback.classic.Level;

public final class LogEvent {
	private final Level level;
	private final String loggerName;
	private final String msg;

	LogEvent(Level level, String loggerName, String msg) {
		this.level = level;
		this.loggerName = loggerName;
		this.msg = msg;
	}

	public Level getLevel() {
		return level;
	}

	public String getLoggerName() {
		return loggerName;
	}

	public String getMsg() {
		return msg;
	}

	@Override
	public String toString() {
		return level + ": " + loggerName + " - " + msg;
	}
}
