package jadx.api.plugins.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZipSecurity {
	private static final Logger LOG = LoggerFactory.getLogger(ZipSecurity.class);

	// size of uncompressed zip entry shouldn't be bigger of compressed in MAX_SIZE_DIFF times
	private static final int MAX_SIZE_DIFF = 100;
	private static final int MAX_ENTRIES_COUNT = 100_000;

	private ZipSecurity() {
	}

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
			File currentPath = CommonFileUtils.CWD;
			File canonical = new File(currentPath, entryName).getCanonicalFile();
			if (isInSubDirectoryInternal(currentPath, canonical)) {
				return true;
			}
			LOG.error("Invalid file name or path traversal attack detected: {}", entryName);
			return false;
		} catch (Exception e) {
			LOG.error("Invalid file name or path traversal attack detected: {}", entryName);
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

	public static InputStream getInputStreamForEntry(ZipFile zipFile, ZipEntry entry) throws IOException {
		InputStream in = zipFile.getInputStream(entry);
		LimitedInputStream limited = new LimitedInputStream(in, entry.getSize());
		return new BufferedInputStream(limited);
	}

	/**
	 * Visit valid entries in zip file.
	 * Return not null value from visitor to stop iteration.
	 */
	@Nullable
	public static <R> R visitZipEntries(File file, BiFunction<ZipFile, ZipEntry, R> visitor) {
		try (ZipFile zip = new ZipFile(file)) {
			Enumeration<? extends ZipEntry> entries = zip.entries();
			int entriesProcessed = 0;
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (isValidZipEntry(entry)) {
					R result = visitor.apply(zip, entry);
					if (result != null) {
						return result;
					}
					entriesProcessed++;
					if (entriesProcessed > MAX_ENTRIES_COUNT) {
						throw new IllegalStateException("Zip entries count limit exceeded: " + MAX_ENTRIES_COUNT
								+ ", last entry: " + entry.getName());
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to process zip file: " + file.getAbsolutePath(), e);
		}
		return null;
	}

	public static void readZipEntries(File file, BiConsumer<ZipEntry, InputStream> visitor) {
		visitZipEntries(file, (zip, entry) -> {
			if (!entry.isDirectory()) {
				try (InputStream in = getInputStreamForEntry(zip, entry)) {
					visitor.accept(entry, in);
				} catch (Exception e) {
					throw new RuntimeException("Error process zip entry: " + entry.getName());
				}
			}
			return null;
		});
	}
}
