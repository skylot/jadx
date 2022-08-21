package jadx.api.plugins.input.data;

import java.io.Closeable;
import java.util.function.Consumer;

public interface ILoadResult extends Closeable {
	void visitClasses(Consumer<IClassData> consumer);

	void visitResources(Consumer<IResourceData> consumer);

	boolean isEmpty();
}
