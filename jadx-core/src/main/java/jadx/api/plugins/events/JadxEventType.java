package jadx.api.plugins.events;

public abstract class JadxEventType<T extends IJadxEvent> {

	public static <E extends IJadxEvent> JadxEventType<E> create() {
		return new JadxEventType<>() {
		};
	}

	public static <E extends IJadxEvent> JadxEventType<E> create(String name) {
		return new JadxEventType<>() {
			@Override
			public String toString() {
				return name;
			}
		};
	}
}
