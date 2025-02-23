package jadx.api.plugins.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.zip.ZipEntry;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.ZipFile;
import jadx.core.utils.zip.ZipContent;
import jadx.core.utils.zip.ZipFileEntry;
import jadx.core.utils.zip.ZipReader;

public class ZipSecurity {
	private static final Logger LOG = LoggerFactory.getLogger(ZipSecurity.class);

	private static final boolean DISABLE_CHECKS = Utils.getEnvVarBool("JADX_DISABLE_ZIP_SECURITY", false);

	/**
	 * size of uncompressed zip entry shouldn't be bigger of compressed in
	 * {@link #ZIP_BOMB_DETECTION_FACTOR} times
	 */
	private static final int ZIP_BOMB_DETECTION_FACTOR = 100;

	/**
	 * Zip entries that have an uncompressed size of less than {@link #ZIP_BOMB_MIN_UNCOMPRESSED_SIZE}
	 * are considered safe
	 */
	private static final int ZIP_BOMB_MIN_UNCOMPRESSED_SIZE = 25 * 1024 * 1024;

	private static final int MAX_ENTRIES_COUNT = Utils.getEnvVarInt("JADX_ZIP_MAX_ENTRIES_COUNT", 100_000);

	private ZipSecurity() {
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

	public static boolean isInSubDirectory(File baseDir, File file) {
		if (DISABLE_CHECKS) {
			return true;
		}
		try {
			return isInSubDirectoryInternal(baseDir.getCanonicalFile(), file.getCanonicalFile());
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Checks that entry name contains no any traversals and prevents cases like "../classes.dex",
	 * to limit output only to the specified directory
	 */
	public static boolean isValidZipEntryName(String entryName) {
		if (DISABLE_CHECKS) {
			return true;
		}
		if (entryName.contains("..")) { // quick pre-check
			if (entryName.contains("../") || entryName.contains("..\\")) {
				LOG.error("Path traversal attack detected in entry: '{}'", entryName);
				return false;
			}
		}
		try {
			File currentPath = CommonFileUtils.CWD;
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

	public static boolean isZipBomb(ZipFileEntry entry) {
		if (DISABLE_CHECKS) {
			return false;
		}
		long compressedSize = entry.getCompressedSize();
		long uncompressedSize = entry.getUncompressedSize();
		boolean invalidSize = (compressedSize < 0) || (uncompressedSize < 0);
		boolean possibleZipBomb = (uncompressedSize >= ZIP_BOMB_MIN_UNCOMPRESSED_SIZE)
				&& (compressedSize * ZIP_BOMB_DETECTION_FACTOR < uncompressedSize);
		if (invalidSize || possibleZipBomb) {
			LOG.error("Potential zip bomb attack detected, invalid sizes: compressed {}, uncompressed {}, name {}",
					compressedSize, uncompressedSize, entry.getName());
			return true;
		}
		return false;
	}

	public static boolean isValidZipEntry(ZipFileEntry entry) {
		return isValidZipEntryName(entry.getName())
				&& !isZipBomb(entry);
	}

	public static InputStream getInputStreamForEntry(ZipFile zipFile, ZipEntry entry) throws IOException {
		if (DISABLE_CHECKS) {
			return new BufferedInputStream(zipFile.getInputStream(entry));
		}
		InputStream in = zipFile.getInputStream(entry);
		LimitedInputStream limited = new LimitedInputStream(in, entry.getSize());
		return new BufferedInputStream(limited);
	}

	/**
	 * Visit valid entries in zip file.
	 * Return not null value from visitor to stop iteration.
	 */
	@Nullable
	public static <R> R visitZipEntries(File file, Function<ZipFileEntry, R> visitor) {
		try (ZipContent content = ZipReader.open(file)) {
			int entriesProcessed = 0;
			for (ZipFileEntry entry : content.getFileEntries()) {
				if (isValidZipEntry(entry)) {
					R result = visitor.apply(entry);
					if (result != null) {
						return result;
					}
					entriesProcessed++;
					if (!DISABLE_CHECKS && entriesProcessed > MAX_ENTRIES_COUNT) {
						throw new JadxRuntimeException("Zip entries count limit exceeded: " + MAX_ENTRIES_COUNT
								+ ", last entry: " + entry.getName());
					}
				}
			}
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to process zip file: " + file.getAbsolutePath(), e);
		}
		return null;
	}

	public static void readZipEntries(File file, BiConsumer<ZipFileEntry, InputStream> visitor) {
		visitZipEntries(file, entry -> {
			if (!entry.isDirectory()) {
				try (InputStream in = entry.getInputStream()) {
					visitor.accept(entry, in);
				} catch (Exception e) {
					throw new JadxRuntimeException("Failed to process zip entry: " + entry.getName(), e);
				}
			}
			return null;
		});
	}
}
