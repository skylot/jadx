package jadx.core.utils.zip;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import org.jetbrains.annotations.Nullable;

public final class ZipContent implements Closeable {

	private final ZipReader zipReader;
	private final List<ZipFileEntry> entries;

	ZipContent(ZipReader zipReader, List<ZipFileEntry> entries) {
		this.zipReader = zipReader;
		this.entries = entries;
	}

	public List<ZipFileEntry> getFileEntries() {
		return entries;
	}

	public @Nullable ZipFileEntry getFile(String fileName) {
		for (ZipFileEntry entry : entries) {
			if (entry.getName().equals(fileName)) {
				return entry;
			}
		}
		return null;
	}

	@Override
	public void close() throws IOException {
		zipReader.close();
	}
}
