package jadx.gui.utils.logs;

import java.util.Deque;
import java.util.LinkedList;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

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

	private Layout<ILoggingEvent> layout;

	@Nullable
	private ILogListener listener;

	private final Deque<LogEvent> buffer = new LinkedList<>();

	public LogCollector() {
		setName("LogCollector");
	}

	@Override
	protected void append(ILoggingEvent event) {
		synchronized (this) {
			Level level = event.getLevel();
			String msg = layout.doLayout(event);
			store(level, msg);
			if (listener != null && level.isGreaterOrEqual(listener.getFilterLevel())) {
				listener.onAppend(msg);
			}
		}
	}

	private void store(Level level, String msg) {
		buffer.addLast(new LogEvent(level, msg));
		if (buffer.size() > BUFFER_SIZE) {
			buffer.pollFirst();
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
		for (LogEvent event : buffer) {
			if (event.getLevel().isGreaterOrEqual(filterLevel)) {
				sb.append(event.getMsg());
			}
		}
		return sb.toString();
	}
}
