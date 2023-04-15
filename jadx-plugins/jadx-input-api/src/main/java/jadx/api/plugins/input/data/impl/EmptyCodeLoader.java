package jadx.api.plugins.input.data.impl;

import java.io.IOException;
import java.util.function.Consumer;

import jadx.api.plugins.input.ICodeLoader;
import jadx.api.plugins.input.data.IClassData;

public class EmptyCodeLoader implements ICodeLoader {

	public static final EmptyCodeLoader INSTANCE = new EmptyCodeLoader();

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public void visitClasses(Consumer<IClassData> consumer) {
	}

	@Override
	public void close() throws IOException {
	}
}
