package jadx.gui.events.types;

import java.util.function.Consumer;

import jadx.api.plugins.events.IJadxEvent;
import jadx.api.plugins.events.IJadxEvents;
import jadx.api.plugins.events.JadxEventType;
import jadx.core.plugins.events.JadxEventsImpl;

/**
 * Special events implementation to operate on both: global UI and project events.
 * Project events hold listeners only while a project opened and reset them on close.
 */
public class JadxGuiEventsImpl implements IJadxEvents {

	private final IJadxEvents global = new JadxEventsImpl();
	private final IJadxEvents project = new JadxEventsImpl();

	public IJadxEvents global() {
		return global;
	}

	@Override
	public void send(IJadxEvent event) {
		global.send(event);
		project.send(event);
	}

	@Override
	public <E extends IJadxEvent> void addListener(JadxEventType<E> eventType, Consumer<E> listener) {
		project.addListener(eventType, listener);
	}

	@Override
	public <E extends IJadxEvent> void removeListener(JadxEventType<E> eventType, Consumer<E> listener) {
		project.removeListener(eventType, listener);
	}

	@Override
	public void reset() {
		project.reset();
	}
}
