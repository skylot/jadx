package jadx.gui.logs;

import java.util.AbstractQueue;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public class LimitedQueue<T> extends AbstractQueue<T> {

	private final Deque<T> deque = new ArrayDeque<>();
	private final int limit;

	public LimitedQueue(int limit) {
		this.limit = limit;
	}

	@Override
	public Iterator<T> iterator() {
		return deque.iterator();
	}

	@Override
	public int size() {
		return deque.size();
	}

	@Override
	public boolean offer(T t) {
		deque.addLast(t);
		if (deque.size() > limit) {
			deque.removeFirst();
		}
		return true;
	}

	@Override
	public T poll() {
		return deque.poll();
	}

	@Override
	public T peek() {
		return deque.peek();
	}

	@Override
	public void clear() {
		deque.clear();
	}
}
