package jadx.zip;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import org.jetbrains.annotations.Nullable;

public class ZipContent implements Closeable {
	private final IZipParser zipParser;
	private final List<IZipEntry> entries;

	public ZipContent(IZipParser zipParser, List<IZipEntry> entries) {
		this.zipParser = zipParser;
		this.entries = entries;
	}

	public List<IZipEntry> getEntries() {
		return entries;
	}

	public @Nullable IZipEntry searchEntry(String fileName) {
		for (IZipEntry entry : entries) {
			if (entry.getName().equals(fileName)) {
				return entry;
			}
		}
		return null;
	}

	@Override
	public void close() throws IOException {
		zipParser.close();
	}
}
