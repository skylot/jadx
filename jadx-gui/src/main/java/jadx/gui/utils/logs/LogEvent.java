package jadx.gui.utils.logs;

import ch.qos.logback.classic.Level;

final class LogEvent {
	private final Level level;
	private final String msg;

	LogEvent(Level level, String msg) {
		this.level = level;
		this.msg = msg;
	}

	public Level getLevel() {
		return level;
	}

	public String getMsg() {
		return msg;
	}

	@Override
	public String toString() {
		return level + ": " + msg;
	}
}
