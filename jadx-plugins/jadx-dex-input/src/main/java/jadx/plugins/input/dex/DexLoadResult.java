package jadx.plugins.input.dex;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import jadx.api.plugins.input.data.IClassData;
import jadx.api.plugins.input.data.ILoadResult;
import jadx.api.plugins.input.data.IResourceData;

public class DexLoadResult implements ILoadResult {
	private final List<DexReader> dexReaders;

	public DexLoadResult(List<DexReader> dexReaders) {
		this.dexReaders = dexReaders;
	}

	@Override
	public void visitClasses(Consumer<IClassData> consumer) {
		for (DexReader dexReader : dexReaders) {
			dexReader.visitClasses(consumer);
		}
	}

	@Override
	public void visitResources(Consumer<IResourceData> consumer) {
	}

	@Override
	public void close() throws IOException {
		for (DexReader dexReader : dexReaders) {
			dexReader.close();
		}
	}
}
