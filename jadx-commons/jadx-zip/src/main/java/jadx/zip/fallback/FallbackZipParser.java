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

import jadx.zip.IZipEntry;
import jadx.zip.IZipParser;
import jadx.zip.ZipContent;

public class FallbackZipParser implements IZipParser {
	private final File file;

	private ZipFile zipFile;

	public FallbackZipParser(File file) {
		this.file = file;
	}

	@Override
	public ZipContent open() throws IOException {
		zipFile = new ZipFile(file);
		List<IZipEntry> list = new ArrayList<>();
		Enumeration<? extends ZipEntry> entries = zipFile.entries();
		while (entries.hasMoreElements()) {
			list.add(new FallbackZipEntry(this, entries.nextElement()));
		}
		return new ZipContent(this, list);
	}

	public byte[] getBytes(FallbackZipEntry entry) {
		// TODO: add checks from ZipSecurity?
		try (InputStream is = zipFile.getInputStream(entry.getZipEntry())) {
			return is.readAllBytes();
		} catch (Exception e) {
			throw new RuntimeException("Failed to read bytes for entry: " + entry.getName(), e);
		}
	}

	public InputStream getInputStream(FallbackZipEntry entry) {
		try {
			return new BufferedInputStream(zipFile.getInputStream(entry.getZipEntry()));
		} catch (Exception e) {
			throw new RuntimeException("Failed to open input stream for entry: " + entry.getName(), e);
		}
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
