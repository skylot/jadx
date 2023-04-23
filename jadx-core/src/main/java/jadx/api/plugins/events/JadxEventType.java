package jadx.api.plugins.events;

public abstract class JadxEventType<T extends IJadxEvent> {

	public static <E extends IJadxEvent> JadxEventType<E> create() {
		return new JadxEventType<>() {
		};
	}
}
