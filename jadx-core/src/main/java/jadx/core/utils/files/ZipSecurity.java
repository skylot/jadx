package jadx.core.utils.files;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZipSecurity {
	private static final Logger LOG = LoggerFactory.getLogger(ZipSecurity.class);

	// size of uncompressed zip entry shouldn't be bigger of compressed in MAX_SIZE_DIFF times
	private static final int MAX_SIZE_DIFF = 100;

	private ZipSecurity() {}

	private static boolean isInSubDirectoryInternal(File baseDir, File canonFile) {
		if (canonFile == null) {
			return false;
		}
		if (canonFile.equals(baseDir)) {
			return true;
		}
		return isInSubDirectoryInternal(baseDir, canonFile.getParentFile());
	}

	public static boolean isInSubDirectory(File baseDir, File file) {
		try {
			file = file.getCanonicalFile();
			baseDir = baseDir.getCanonicalFile();
		} catch (IOException e) {
			return false;
		}
		return isInSubDirectoryInternal(baseDir, file);
	}

	// checks that entry name contains no any traversals
	// and prevents cases like "../classes.dex", to limit output only to the specified directory
	public static boolean isValidZipEntryName(String entryName) {
		try {
			File currentPath = new File(".").getCanonicalFile();
			File canonical = new File(currentPath, entryName).getCanonicalFile();
			if (isInSubDirectoryInternal(currentPath, canonical)) {
				return true;
			}
			LOG.error("Path traversal attack detected, invalid name: {}", entryName);
			return false;
		} catch (Exception e) {
			LOG.error("Path traversal attack detected, invalid name: {}", entryName);
			return false;
		}
	}

	public static boolean isZipBomb(ZipEntry entry) {
		long compressedSize = entry.getCompressedSize();
		long uncompressedSize = entry.getSize();
		if (compressedSize < 0 || uncompressedSize < 0) {
			LOG.error("Zip bomb attack detected, invalid sizes: compressed {}, uncompressed {}, name {}",
					compressedSize, uncompressedSize, entry.getName());
			return true;
		}
		if (compressedSize * MAX_SIZE_DIFF < uncompressedSize) {
			LOG.error("Zip bomb attack detected, invalid sizes: compressed {}, uncompressed {}, name {}",
					compressedSize, uncompressedSize, entry.getName());
			return true;
		}
		return false;
	}

	public static boolean isValidZipEntry(ZipEntry entry) {
		return isValidZipEntryName(entry.getName())
				&& !isZipBomb(entry);
	}
}
