package jadx.zip.fallback;

import java.io.File;
import java.io.InputStream;
import java.util.zip.ZipEntry;

import jadx.zip.IZipEntry;

public class FallbackZipEntry implements IZipEntry {
	private final FallbackZipParser parser;
	private final ZipEntry zipEntry;

	public FallbackZipEntry(FallbackZipParser parser, ZipEntry zipEntry) {
		this.parser = parser;
		this.zipEntry = zipEntry;
	}

	public ZipEntry getZipEntry() {
		return zipEntry;
	}

	@Override
	public String getName() {
		return zipEntry.getName();
	}

	@Override
	public boolean preferBytes() {
		return false;
	}

	@Override
	public byte[] getBytes() {
		return parser.getBytes(this);
	}

	@Override
	public InputStream getInputStream() {
		return parser.getInputStream(this);
	}

	@Override
	public long getCompressedSize() {
		return zipEntry.getCompressedSize();
	}

	@Override
	public long getUncompressedSize() {
		return zipEntry.getSize();
	}

	@Override
	public boolean isDirectory() {
		return zipEntry.isDirectory();
	}

	@Override
	public File getZipFile() {
		return parser.getZipFile();
	}
}
