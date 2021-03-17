package jadx.gui.utils;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ObjectPool<T> {

	private final ConcurrentLinkedQueue<WeakReference<T>> pool = new ConcurrentLinkedQueue<>();
	private final Creator<T> creator;

	public interface Creator<T> {
		T create();
	}

	public ObjectPool(Creator<T> creator) {
		this.creator = creator;
	}

	public T get() {
		T node;
		do {
			WeakReference<T> wNode = pool.poll();
			if (wNode == null) {
				return creator.create();
			}
			node = wNode.get();
		} while (node == null);
		return node;
	}

	public void put(T node) {
		pool.add(new WeakReference<>(node));
	}
}
