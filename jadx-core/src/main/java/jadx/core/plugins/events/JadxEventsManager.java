package jadx.core.plugins.events;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import jadx.api.plugins.events.IJadxEvent;
import jadx.api.plugins.events.JadxEventType;

/**
 * Handle events sending and receiving
 */
public class JadxEventsManager {

	private final Map<JadxEventType<?>, List<Consumer<IJadxEvent>>> listeners = new IdentityHashMap<>();

	private final ExecutorService eventsThreadPool;

	public JadxEventsManager() {
		// TODO: allow to change threading strategy
		this.eventsThreadPool = Executors.newSingleThreadExecutor(makeThreadFactory());
	}

	@SuppressWarnings("unchecked")
	public synchronized <E extends IJadxEvent> void addListener(JadxEventType<E> eventType, Consumer<E> listener) {
		listeners.computeIfAbsent(eventType, et -> new ArrayList<>())
				.add((Consumer<IJadxEvent>) listener);
	}

	public synchronized void send(IJadxEvent event) {
		List<Consumer<IJadxEvent>> consumers = listeners.get(event.getType());
		if (consumers != null) {
			for (Consumer<IJadxEvent> consumer : consumers) {
				eventsThreadPool.execute(() -> consumer.accept(event));
			}
		}
	}

	public synchronized void reset() {
		listeners.clear();
	}

	private static ThreadFactory makeThreadFactory() {
		return new ThreadFactory() {
			private final AtomicInteger threadNumber = new AtomicInteger(0);

			@Override
			public Thread newThread(@NotNull Runnable r) {
				return new Thread(r, "jadx-events-thread-" + threadNumber.incrementAndGet());
			}
		};
	}
}
