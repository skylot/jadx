package jadx.zip;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

public class ZipContent implements Closeable {
	private final IZipParser zipParser;
	private final List<IZipEntry> entries;
	private final Map<String, IZipEntry> entriesMap;

	public ZipContent(IZipParser zipParser, List<IZipEntry> entries) {
		this.zipParser = zipParser;
		this.entries = entries;
		this.entriesMap = entries.stream().collect(Collectors.toMap(IZipEntry::getName, Function.identity()));
	}

	public List<IZipEntry> getEntries() {
		return entries;
	}

	public @Nullable IZipEntry searchEntry(String fileName) {
		return entriesMap.get(fileName);
	}

	@Override
	public void close() throws IOException {
		zipParser.close();
	}
}
