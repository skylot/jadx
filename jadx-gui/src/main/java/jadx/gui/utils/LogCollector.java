package jadx.gui.utils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.read.CyclicBufferAppender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

public class LogCollector extends CyclicBufferAppender<ILoggingEvent> {
	private static LogCollector instance = new LogCollector();

	public static LogCollector getInstance() {
		return instance;
	}

	public static void register() {
		Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		LoggerContext loggerContext = rootLogger.getLoggerContext();

		PatternLayout layout = new PatternLayout();
		layout.setContext(loggerContext);
		layout.setPattern("%-5level: %msg%n");
		layout.start();

		instance.setContext(loggerContext);
		instance.setLayout(layout);
		instance.start();

		rootLogger.addAppender(instance);
	}

	public interface ILogListener {

		Level getFilterLevel();

		void onAppend(String logStr);
	}

	private Layout<ILoggingEvent> layout;

	@Nullable
	private ILogListener listener;

	public LogCollector() {
		setName("LogCollector");
		setMaxSize(5000);
	}

	@Override
	protected void append(ILoggingEvent event) {
		super.append(event);
		if (listener != null
				&& event.getLevel().isGreaterOrEqual(listener.getFilterLevel())) {
			synchronized (this) {
				listener.onAppend(layout.doLayout(event));
			}
		}
	}

	public void setLayout(Layout<ILoggingEvent> layout) {
		this.layout = layout;
	}

	public void registerListener(@NotNull ILogListener listener) {
		this.listener = listener;
		synchronized (this) {
			listener.onAppend(init(listener.getFilterLevel()));
		}
	}

	public void resetListener() {
		this.listener = null;
	}

	private String init(Level filterLevel) {
		StringBuilder sb = new StringBuilder();
		int length = getLength();
		for (int i = 0; i < length; i++) {
			ILoggingEvent event = get(i);
			if (event.getLevel().isGreaterOrEqual(filterLevel)) {
				sb.append(layout.doLayout(event));
			}
		}
		return sb.toString();
	}
}
