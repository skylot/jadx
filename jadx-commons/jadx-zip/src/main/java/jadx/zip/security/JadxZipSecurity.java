package jadx.zip.security;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.zip.IZipEntry;

public class JadxZipSecurity implements IJadxZipSecurity {
	private static final Logger LOG = LoggerFactory.getLogger(JadxZipSecurity.class);

	private static final Path CWD = Paths.get(".").toAbsolutePath().normalize();

	/**
	 * Entries with a compression ratio better than this factor are treated as suspicious:
	 * such entries are rejected if they are also bigger than {@link #maxUncompressedEntrySize}
	 * and unpacked (with a warning) otherwise, see {@link #isZipBomb(IZipEntry)} for details.
	 */
	private int zipBombDetectionFactor = 100;

	/**
	 * Uncompressed size below which the compression ratio is not even checked.
	 */
	private int zipBombMinUncompressedSize = 25 * 1024 * 1024;

	/**
	 * Limit on the declared uncompressed size of an entry, applied only to entries which are
	 * also highly compressible.
	 * <p>
	 * A plain size limit can't be used here: split APK files inside APKS/XAPK archives are often
	 * bigger than this, but they are not compressed that well, so they are safe to unpack.
	 */
	private long maxUncompressedEntrySize = 512L * 1024 * 1024;

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
		// Path traversal check as presented on
		// https://www.heise.de/en/background/Secure-Coding-Best-practices-for-using-Java-NIO-against-path-traversal-9996787.html
		try {
			Path entryPathPart = Paths.get(entryName).normalize();
			if (entryPathPart.startsWith(CWD) || entryPathPart.isAbsolute()) {
				// reject entry name if it is already a full path to CWD, otherwise next check will always pass
				// reject absolute path as well
				LOG.error("Path traversal attack detected (absolute path) in entry: {}", entryName);
				return false;
			}
			Path entryPath = CWD.resolve(entryPathPart).normalize();
			if (entryPath.startsWith(CWD)) {
				return true;
			}
		} catch (Exception e) {
			// check failed
			LOG.error("Invalid file name or path traversal attack detected: {} - error: {}", entryName, e.getMessage());
			return false;
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
		if (compressedSize < 0 || uncompressedSize < 0) {
			LOG.error("Potential zip bomb attack detected, invalid sizes: compressed {}, uncompressed {}, name {}",
					compressedSize, uncompressedSize, entry.getName());
			return true;
		}
		boolean highCompressionRatio = uncompressedSize >= zipBombMinUncompressedSize
				&& compressedSize * zipBombDetectionFactor < uncompressedSize;
		if (!highCompressionRatio) {
			return false;
		}
		if (uncompressedSize > maxUncompressedEntrySize) {
			// Reject only an entry which is big AND highly compressible. Size alone is not enough:
			// split APK files inside APKS/XAPK archives often exceed any reasonable limit.
			LOG.error("Potential zip bomb attack detected, entry is too big: uncompressed {}, limit {}, name {}",
					uncompressedSize, maxUncompressedEntrySize, entry.getName());
			return true;
		}
		// A high ratio alone is not a reliable bomb indicator: protected APKs often pad binary XML
		// (e.g. AndroidManifest.xml) with huge zero filled areas, which are very compressible but
		// harmless. Rejecting such entries silently drops required files (like the manifest) from
		// the APK, so such entries are unpacked and only reported.
		LOG.warn("High compression ratio detected (compressed {}, uncompressed {}), entry will be unpacked: {}",
				compressedSize, uncompressedSize, entry.getName());
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

	public void setMaxUncompressedEntrySize(long maxUncompressedEntrySize) {
		this.maxUncompressedEntrySize = maxUncompressedEntrySize;
	}

	public void setUseLimitedDataStream(boolean useLimitedDataStream) {
		this.useLimitedDataStream = useLimitedDataStream;
	}

}
