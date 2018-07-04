package jadx.gui.utils.logs;

import ch.qos.logback.classic.Level;

public interface ILogListener {
	Level getFilterLevel();

	void onAppend(String logStr);
}
