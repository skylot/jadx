package jadx.gui.logs;

public interface ILogListener {
	void onAppend(LogEvent logEvent);

	void onReload();
}
