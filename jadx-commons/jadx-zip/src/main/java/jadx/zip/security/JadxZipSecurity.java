package jadx.zip.security;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.zip.IZipEntry;

public class JadxZipSecurity implements IJadxZipSecurity {
	private static final Logger LOG = LoggerFactory.getLogger(JadxZipSecurity.class);

	private static final File CWD = getCWD();

	/**
	 * The size of uncompressed zip entry shouldn't be bigger of compressed in zipBombDetectionFactor
	 * times
	 */
	private int zipBombDetectionFactor = 100;

	/**
	 * Zip entries that have an uncompressed size of less than zipBombMinUncompressedSize are considered
	 * safe
	 */
	private int zipBombMinUncompressedSize = 25 * 1024 * 1024;

	private int maxEntriesCount = 100_000;

	private boolean useLimitedDataStream = true;

	@Override
	public boolean isValidEntry(IZipEntry entry) {
		return isValidEntryName(entry.getName()) && !isZipBomb(entry);
	}

	@Override
	public boolean useLimitedDataStream() {
		return useLimitedDataStream;
	}

	@Override
	public int getMaxEntriesCount() {
		return maxEntriesCount;
	}

	/**
	 * Checks that entry name contains no any traversals and prevents cases like "../classes.dex",
	 * to limit output only to the specified directory
	 */
	@Override
	public boolean isValidEntryName(String entryName) {
		if (entryName.contains("..")) { // quick pre-check
			if (entryName.contains("../") || entryName.contains("..\\")) {
				LOG.error("Path traversal attack detected in entry: '{}'", entryName);
				return false;
			}
		}
		try {
			File currentPath = CWD;
			File canonical = new File(currentPath, entryName).getCanonicalFile();
			if (isInSubDirectoryInternal(currentPath, canonical)) {
				return true;
			}
		} catch (Exception e) {
			// check failed
		}
		LOG.error("Invalid file name or path traversal attack detected: {}", entryName);
		return false;
	}

	@Override
	public boolean isInSubDirectory(File baseDir, File file) {
		try {
			return isInSubDirectoryInternal(baseDir.getCanonicalFile(), file.getCanonicalFile());
		} catch (IOException e) {
			return false;
		}
	}

	public boolean isZipBomb(IZipEntry entry) {
		long compressedSize = entry.getCompressedSize();
		long uncompressedSize = entry.getUncompressedSize();
		boolean invalidSize = compressedSize < 0 || uncompressedSize < 0;
		boolean possibleZipBomb = uncompressedSize >= zipBombMinUncompressedSize
				&& compressedSize * zipBombDetectionFactor < uncompressedSize;
		if (invalidSize || possibleZipBomb) {
			LOG.error("Potential zip bomb attack detected, invalid sizes: compressed {}, uncompressed {}, name {}",
					compressedSize, uncompressedSize, entry.getName());
			return true;
		}
		return false;
	}

	private static boolean isInSubDirectoryInternal(File baseDir, File file) {
		File current = file;
		while (true) {
			if (current == null) {
				return false;
			}
			if (current.equals(baseDir)) {
				return true;
			}
			current = current.getParentFile();
		}
	}

	public void setMaxEntriesCount(int maxEntriesCount) {
		this.maxEntriesCount = maxEntriesCount;
	}

	public void setZipBombDetectionFactor(int zipBombDetectionFactor) {
		this.zipBombDetectionFactor = zipBombDetectionFactor;
	}

	public void setZipBombMinUncompressedSize(int zipBombMinUncompressedSize) {
		this.zipBombMinUncompressedSize = zipBombMinUncompressedSize;
	}

	public void setUseLimitedDataStream(boolean useLimitedDataStream) {
		this.useLimitedDataStream = useLimitedDataStream;
	}

	private static File getCWD() {
		try {
			return new File(".").getCanonicalFile();
		} catch (IOException e) {
			throw new RuntimeException("Failed to init current working dir constant", e);
		}
	}

}
