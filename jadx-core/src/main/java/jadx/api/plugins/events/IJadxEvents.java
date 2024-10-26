package jadx.api.plugins.events;

import java.util.function.Consumer;

public interface IJadxEvents {

	/**
	 * Send an event object.
	 * For public event types check {@link JadxEvents} class.
	 */
	void send(IJadxEvent event);

	/**
	 * Register listener for specific event.
	 * For public event types check {@link JadxEvents} class.
	 */
	<E extends IJadxEvent> void addListener(JadxEventType<E> eventType, Consumer<E> listener);

	/**
	 * Remove listener for specific event.
	 * Listener should be same or equal object.
	 */
	<E extends IJadxEvent> void removeListener(JadxEventType<E> eventType, Consumer<E> listener);

	/**
	 * Clear all listeners.
	 */
	void reset();
}
