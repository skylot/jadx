package jadx.zip.security;

import java.io.File;

import jadx.zip.IZipEntry;

public interface IJadxZipSecurity {

	/**
	 * Check if zip entry is valid and safe to process
	 */
	boolean isValidEntry(IZipEntry entry);

	/**
	 * Check if the zip entry name is valid.
	 * This check should be part of {@link #isValidEntry(IZipEntry)} method.
	 */
	boolean isValidEntryName(String entryName);

	/**
	 * Use limited InputStream for entry uncompressed data
	 */
	boolean useLimitedDataStream();

	/**
	 * Max entries count expected in a zip file, fail zip open if the limit exceeds.
	 * Return -1 to disable entries count check.
	 */
	int getMaxEntriesCount();

	/**
	 * Check if a file will be inside baseDir after a system resolves its path
	 */
	boolean isInSubDirectory(File baseDir, File file);
}
