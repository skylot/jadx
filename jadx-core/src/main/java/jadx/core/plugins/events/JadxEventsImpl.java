package jadx.core.plugins.events;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.events.IJadxEvent;
import jadx.api.plugins.events.IJadxEvents;
import jadx.api.plugins.events.JadxEventType;
import jadx.core.Consts;

public class JadxEventsImpl implements IJadxEvents {
	private static final Logger LOG = LoggerFactory.getLogger(JadxEventsImpl.class);

	private final JadxEventsManager manager = new JadxEventsManager();

	@Override
	public void send(IJadxEvent event) {
		if (Consts.DEBUG_EVENTS) {
			LOG.debug("Sending event: {}", event);
		}
		manager.send(event);
	}

	@Override
	public <E extends IJadxEvent> void addListener(JadxEventType<E> eventType, Consumer<E> listener) {
		manager.addListener(eventType, listener);
	}

	public void reset() {
		manager.reset();
	}
}
