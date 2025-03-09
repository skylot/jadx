package jadx.zip;

import java.io.InputStream;

public interface IZipEntry {

	/**
	 * Zip entry name
	 */
	String getName();

	/**
	 * Uncompressed bytes
	 */
	byte[] getBytes();

	/**
	 * Stream of uncompressed bytes.
	 */
	InputStream getInputStream();

	long getCompressedSize();

	long getUncompressedSize();

	boolean isDirectory();
}
