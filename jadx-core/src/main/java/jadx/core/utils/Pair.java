package jadx.core.utils;

public class Pair<T> {

	private final T first;
	private final T second;

	public Pair(T first, T second) {
		this.first = first;
		this.second = second;
	}

	public T getFirst() {
		return first;
	}

	public T getSecond() {
		return second;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Pair)) {
			return false;
		}
		Pair<?> other = (Pair<?>) o;
		return first.equals(other.first) && second.equals(other.second);
	}

	@Override
	public int hashCode() {
		return first.hashCode() + 31 * second.hashCode();
	}

	@Override
	public String toString() {
		return "(" + first + ", " + second + ')';
	}
}
