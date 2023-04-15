package jadx.api.plugins.input;

import java.io.Closeable;
import java.util.function.Consumer;

import jadx.api.plugins.input.data.IClassData;

public interface ICodeLoader extends Closeable {

	void visitClasses(Consumer<IClassData> consumer);

	boolean isEmpty();
}
