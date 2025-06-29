package jadx.zip;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZipContent implements Closeable {
	private static final Logger LOG = LoggerFactory.getLogger(ZipContent.class);

	private final IZipParser zipParser;
	private final List<IZipEntry> entries;
	private final Map<String, IZipEntry> entriesMap;

	public ZipContent(IZipParser zipParser, List<IZipEntry> entries) {
		this.zipParser = zipParser;
		this.entries = entries;
		this.entriesMap = buildNameMap(zipParser, entries);
	}

	private static Map<String, IZipEntry> buildNameMap(IZipParser zipParser, List<IZipEntry> entries) {
		Map<String, IZipEntry> map = new HashMap<>(entries.size());
		for (IZipEntry entry : entries) {
			String name = entry.getName();
			IZipEntry prevEntry = map.put(name, entry);
			if (prevEntry != null) {
				LOG.warn("Found duplicate entry: {} in {}", name, zipParser);
			}
		}
		return map;
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
