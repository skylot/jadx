package jadx.api.plugins.input.data.impl;

import java.io.IOException;
import java.util.function.Consumer;

import jadx.api.plugins.input.data.IClassData;
import jadx.api.plugins.input.data.ILoadResult;
import jadx.api.plugins.input.data.IResourceData;

public class EmptyLoadResult implements ILoadResult {

	public static final EmptyLoadResult INSTANCE = new EmptyLoadResult();

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public void visitClasses(Consumer<IClassData> consumer) {
	}

	@Override
	public void visitResources(Consumer<IResourceData> consumer) {
	}

	@Override
	public void close() throws IOException {
	}
}
