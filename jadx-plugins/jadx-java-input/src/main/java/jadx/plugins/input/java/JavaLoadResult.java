package jadx.plugins.input.java;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.input.data.IClassData;
import jadx.api.plugins.input.data.ILoadResult;
import jadx.api.plugins.input.data.IResourceData;

public class JavaLoadResult implements ILoadResult {
	private static final Logger LOG = LoggerFactory.getLogger(JavaLoadResult.class);

	private final List<JavaClassReader> readers;
	@Nullable
	private final Closeable closeable;

	public JavaLoadResult(List<JavaClassReader> readers, @Nullable Closeable closeable) {
		this.readers = readers;
		this.closeable = closeable;
	}

	@Override
	public void visitClasses(Consumer<IClassData> consumer) {
		for (JavaClassReader reader : readers) {
			try {
				consumer.accept(reader.loadClassData());
			} catch (Exception e) {
				LOG.error("Failed to load class data for file: " + reader.getFileName(), e);
			}
		}
	}

	@Override
	public void visitResources(Consumer<IResourceData> consumer) {
	}

	@Override
	public boolean isEmpty() {
		return readers.isEmpty();
	}

	@Override
	public void close() throws IOException {
		readers.clear();
		if (closeable != null) {
			closeable.close();
		}
	}
}
