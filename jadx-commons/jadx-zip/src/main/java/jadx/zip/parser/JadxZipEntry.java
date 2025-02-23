package jadx.zip.parser;

import java.io.File;
import java.io.InputStream;

import jadx.zip.IZipEntry;

public final class JadxZipEntry implements IZipEntry {
	private final JadxZipParser parser;
	private final String fileName;
	private final int compressMethod;
	private final int entryStart;
	private final int dataStart;
	private final long compressedSize;
	private final long uncompressedSize;

	JadxZipEntry(JadxZipParser parser, String fileName, int entryStart, int dataStart,
			int compressMethod, long compressedSize, long uncompressedSize) {
		this.parser = parser;
		this.fileName = fileName;
		this.entryStart = entryStart;
		this.dataStart = dataStart;
		this.compressMethod = compressMethod;
		this.compressedSize = compressedSize;
		this.uncompressedSize = uncompressedSize;
	}

	public boolean isSizesValid() {
		if (compressedSize <= 0) {
			return false;
		}
		if (uncompressedSize <= 0) {
			return false;
		}
		return compressedSize <= uncompressedSize;
	}

	public String getName() {
		return fileName;
	}

	@Override
	public long getCompressedSize() {
		return compressedSize;
	}

	@Override
	public long getUncompressedSize() {
		return uncompressedSize;
	}

	@Override
	public boolean isDirectory() {
		return fileName.endsWith("/");
	}

	@Override
	public boolean preferBytes() {
		return true;
	}

	@Override
	public byte[] getBytes() {
		return parser.getBytes(this);
	}

	@Override
	public InputStream getInputStream() {
		return parser.getInputStream(this);
	}

	public int getEntryStart() {
		return entryStart;
	}

	public int getDataStart() {
		return dataStart;
	}

	public int getCompressMethod() {
		return compressMethod;
	}

	@Override
	public File getZipFile() {
		return parser.getZipFile();
	}

	@Override
	public String toString() {
		return parser.getZipFile().getName() + ':' + fileName;
	}
}
