package jadx.gui.logs;

import org.apache.commons.lang3.StringUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.utils.UiUtils;

import static jadx.plugins.script.runtime.ScriptRuntime.JADX_SCRIPT_LOG_PREFIX;

class LogAppender implements ILogListener {
	private final LogOptions options;
	private final RSyntaxTextArea textArea;

	public LogAppender(LogOptions options, RSyntaxTextArea textArea) {
		this.options = options;
		this.textArea = textArea;
	}

	@Override
	public void onAppend(LogEvent logEvent) {
		if (accept(logEvent)) {
			UiUtils.uiRun(() -> textArea.append(logEvent.getMsg()));
		}
	}

	@Override
	public void onReload() {
		UiUtils.uiRunAndWait(() -> textArea.append(StringUtils.repeat('=', 100) + '\n'));
	}

	private boolean accept(LogEvent logEvent) {
		boolean byLevel = logEvent.getLevel().isGreaterOrEqual(options.getLogLevel());
		if (!byLevel) {
			return false;
		}
		switch (options.getMode()) {
			case ALL:
				return true;

			case ALL_SCRIPTS:
				return logEvent.getLoggerName().startsWith(JADX_SCRIPT_LOG_PREFIX);

			case CURRENT_SCRIPT:
				return logEvent.getLoggerName().equals(options.getFilter());

			default:
				throw new JadxRuntimeException("Unexpected log mode: " + options.getMode());
		}
	}
}
