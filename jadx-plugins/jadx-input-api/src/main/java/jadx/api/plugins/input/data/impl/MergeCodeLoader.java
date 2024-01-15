package jadx.api.plugins.input.data.impl;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.ICodeLoader;
import jadx.api.plugins.input.data.IClassData;

public class MergeCodeLoader implements ICodeLoader {

	private final List<ICodeLoader> codeLoaders;
	private final @Nullable Closeable closeable;

	public MergeCodeLoader(List<ICodeLoader> codeLoaders) {
		this(codeLoaders, null);
	}

	public MergeCodeLoader(List<ICodeLoader> codeLoaders, @Nullable Closeable closeable) {
		this.codeLoaders = codeLoaders;
		this.closeable = closeable;
	}

	@Override
	public void visitClasses(Consumer<IClassData> consumer) {
		for (ICodeLoader codeLoader : codeLoaders) {
			codeLoader.visitClasses(consumer);
		}
	}

	@Override
	public boolean isEmpty() {
		for (ICodeLoader codeLoader : codeLoaders) {
			if (!codeLoader.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void close() throws IOException {
		for (ICodeLoader codeLoader : codeLoaders) {
			codeLoader.close();
		}
		if (closeable != null) {
			closeable.close();
		}
	}
}
