package jadx.core.utils.zip;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public final class ZipFileEntry {
	private final ZipReader reader;
	private final String fileName;
	private final int compressMethod;
	private final int dataStart;
	private final int compressedSize;
	private final int uncompressedSize;
	private final boolean isDirectory;

	ZipFileEntry(ZipReader reader, String fileName, int start,
			int compressMethod, int compressedSize, int uncompressedSize) {
		this.reader = reader;
		this.fileName = fileName;
		this.dataStart = start;
		this.compressMethod = compressMethod;
		this.compressedSize = compressedSize;
		this.uncompressedSize = uncompressedSize;
		this.isDirectory = fileName.endsWith("/");
	}

	public String getName() {
		return fileName;
	}

	public int getCompressedSize() {
		return compressedSize;
	}

	public int getUncompressedSize() {
		return uncompressedSize;
	}

	public boolean isDirectory() {
		return isDirectory;
	}

	public byte[] getBytes() {
		return reader.getEntryBytes(this);
	}

	public InputStream getInputStream() {
		// TODO: use lazy loading
		return new ByteArrayInputStream(getBytes());
	}

	int getCompressMethod() {
		return compressMethod;
	}

	int getDataStart() {
		return dataStart;
	}

	@Override
	public String toString() {
		return "ZipFileEntry{" + fileName + "}";
	}
}
