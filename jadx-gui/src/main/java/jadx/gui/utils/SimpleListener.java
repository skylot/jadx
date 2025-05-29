package jadx.gui.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SimpleListener<T> {
	private final List<Consumer<T>> listeners = new ArrayList<>();

	public void sendUpdate(T data) {
		for (Consumer<T> listener : listeners) {
			listener.accept(data);
		}
	}

	public void addListener(Consumer<T> listener) {
		listeners.add(listener);
	}

	public boolean removeListener(Consumer<T> listener) {
		return listeners.remove(listener);
	}
}
