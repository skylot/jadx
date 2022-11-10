package jadx.gui.logs;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;

public class LogCollector extends AppenderBase<ILoggingEvent> {
	public static final int BUFFER_SIZE = 5000;

	private static final LogCollector INSTANCE = new LogCollector();

	public static LogCollector getInstance() {
		return INSTANCE;
	}

	public static void register() {
		Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		LoggerContext loggerContext = rootLogger.getLoggerContext();

		PatternLayout layout = new PatternLayout();
		layout.setContext(loggerContext);
		layout.setPattern("%-5level: %msg%n");
		layout.start();

		INSTANCE.setContext(loggerContext);
		INSTANCE.setLayout(layout);
		INSTANCE.start();

		rootLogger.addAppender(INSTANCE);
	}

	private final List<ILogListener> listeners = new ArrayList<>();
	private final Queue<LogEvent> buffer = new LimitedQueue<>(BUFFER_SIZE);

	private Layout<ILoggingEvent> layout;

	public LogCollector() {
		setName("LogCollector");
	}

	@Override
	protected synchronized void append(ILoggingEvent event) {
		String msg = layout.doLayout(event);
		LogEvent logEvent = new LogEvent(event.getLevel(), event.getLoggerName(), msg);
		buffer.offer(logEvent);
		listeners.forEach(l -> l.onAppend(logEvent));
	}

	private void setLayout(Layout<ILoggingEvent> layout) {
		this.layout = layout;
	}

	public synchronized void registerListener(ILogListener listener) {
		listeners.add(listener);
		buffer.forEach(listener::onAppend);
	}

	public synchronized boolean removeListener(@Nullable ILogListener listener) {
		if (listener == null) {
			return false;
		}
		return this.listeners.removeIf(l -> l == listener);
	}

	public synchronized boolean removeListenerByClass(Class<?> listenerCls) {
		return this.listeners.removeIf(l -> l.getClass().equals(listenerCls));
	}

	public synchronized void reset() {
		buffer.clear();
		listeners.forEach(ILogListener::onReload);
	}
}
