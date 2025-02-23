package jadx.zip.fallback;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.zip.IZipEntry;
import jadx.zip.IZipParser;
import jadx.zip.ZipContent;
import jadx.zip.ZipReaderOptions;
import jadx.zip.security.IJadxZipSecurity;
import jadx.zip.security.LimitedInputStream;

public class FallbackZipParser implements IZipParser {
	private static final Logger LOG = LoggerFactory.getLogger(FallbackZipParser.class);
	private final File file;
	private final IJadxZipSecurity zipSecurity;
	private final boolean useLimitedDataStream;

	private ZipFile zipFile;

	public FallbackZipParser(File file, ZipReaderOptions options) {
		this.file = file;
		this.zipSecurity = options.getZipSecurity();
		this.useLimitedDataStream = zipSecurity.useLimitedDataStream();
	}

	@Override
	public ZipContent open() throws IOException {
		zipFile = new ZipFile(file);

		int maxEntriesCount = zipSecurity.getMaxEntriesCount();
		if (maxEntriesCount == -1) {
			maxEntriesCount = Integer.MAX_VALUE;
		}

		List<IZipEntry> list = new ArrayList<>();
		Enumeration<? extends ZipEntry> entries = zipFile.entries();
		while (entries.hasMoreElements()) {
			FallbackZipEntry zipEntry = new FallbackZipEntry(this, entries.nextElement());
			if (isValidEntry(zipEntry)) {
				list.add(zipEntry);
				if (list.size() > maxEntriesCount) {
					throw new IllegalStateException("Max entries count limit exceeded: " + list.size());
				}
			}
		}
		return new ZipContent(this, list);
	}

	private boolean isValidEntry(IZipEntry zipEntry) {
		boolean validEntry = zipSecurity.isValidEntry(zipEntry);
		if (!validEntry) {
			LOG.warn("Zip entry '{}' is invalid and excluded from processing", zipEntry);
		}
		return validEntry;
	}

	public byte[] getBytes(FallbackZipEntry entry) {
		try (InputStream is = getEntryStream(entry)) {
			return is.readAllBytes();
		} catch (Exception e) {
			throw new RuntimeException("Failed to read bytes for entry: " + entry.getName(), e);
		}
	}

	public InputStream getInputStream(FallbackZipEntry entry) {
		try {
			return getEntryStream(entry);
		} catch (Exception e) {
			throw new RuntimeException("Failed to open input stream for entry: " + entry.getName(), e);
		}
	}

	private InputStream getEntryStream(FallbackZipEntry entry) throws IOException {
		InputStream entryStream = zipFile.getInputStream(entry.getZipEntry());
		InputStream stream;
		if (useLimitedDataStream) {
			stream = new LimitedInputStream(entryStream, entry.getUncompressedSize());
		} else {
			stream = entryStream;
		}
		return new BufferedInputStream(stream);
	}

	public File getZipFile() {
		return file;
	}

	@Override
	public void close() throws IOException {
		try {
			if (zipFile != null) {
				zipFile.close();
			}
		} finally {
			zipFile = null;
		}
	}
}
