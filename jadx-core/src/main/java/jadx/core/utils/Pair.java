package jadx.core.utils;

import java.util.Objects;

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
		return Objects.equals(first, other.first) && Objects.equals(second, other.second);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(first) + 31 * Objects.hashCode(second);
	}

	@Override
	public String toString() {
		return "(" + first + ", " + second + ')';
	}
}
