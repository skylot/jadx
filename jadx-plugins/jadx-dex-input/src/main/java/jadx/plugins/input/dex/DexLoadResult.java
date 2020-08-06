package jadx.plugins.input.dex;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.IClassData;
import jadx.api.plugins.input.data.ILoadResult;
import jadx.api.plugins.input.data.IResourceData;

public class DexLoadResult implements ILoadResult {
	private final List<DexReader> dexReaders;
	@Nullable
	private final Closeable closeable;

	public DexLoadResult(List<DexReader> dexReaders, @Nullable Closeable closeable) {
		this.dexReaders = dexReaders;
		this.closeable = closeable;
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
		dexReaders.clear();
		if (closeable != null) {
			closeable.close();
		}
	}

	@Override
	public boolean isEmpty() {
		return dexReaders.isEmpty();
	}
}
