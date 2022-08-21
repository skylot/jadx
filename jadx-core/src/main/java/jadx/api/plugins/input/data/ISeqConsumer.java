package jadx.api.plugins.input.data;

import java.util.function.Consumer;

/**
 * "Sequence consumer" allows getting count of elements available
 */
public interface ISeqConsumer<T> extends Consumer<T> {

	default void init(int count) {
		// no-op implementation
	}
}
