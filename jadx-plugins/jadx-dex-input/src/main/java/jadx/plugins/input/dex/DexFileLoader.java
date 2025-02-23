package jadx.plugins.input.dex;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.utils.CommonFileUtils;
import jadx.plugins.input.dex.sections.DexConsts;
import jadx.plugins.input.dex.sections.DexHeaderV41;
import jadx.plugins.input.dex.utils.DexCheckSum;
import jadx.zip.IZipEntry;
import jadx.zip.ZipContent;
import jadx.zip.ZipReader;

public class DexFileLoader {
	private static final Logger LOG = LoggerFactory.getLogger(DexFileLoader.class);

	// sharing between all instances (can be used in other plugins) // TODO:
	private static int dexUniqId = 1;

	private final DexInputOptions options;

	private ZipReader zipReader = new ZipReader();

	public DexFileLoader(DexInputOptions options) {
		this.options = options;
	}

	public void setZipReader(ZipReader zipReader) {
		this.zipReader = zipReader;
	}

	public List<DexReader> collectDexFiles(List<Path> pathsList) {
		return pathsList.stream()
				.map(Path::toFile)
				.map(this::loadDexFromFile)
				.filter(list -> !list.isEmpty())
				.flatMap(Collection::stream)
				.peek(dr -> LOG.debug("Loading dex: {}", dr))
				.collect(Collectors.toList());
	}

	private List<DexReader> loadDexFromFile(File file) {
		try (InputStream inputStream = new FileInputStream(file)) {
			return load(file, inputStream, file.getAbsolutePath());
		} catch (Exception e) {
			LOG.error("File open error: {}", file.getAbsolutePath(), e);
			return Collections.emptyList();
		}
	}

	private List<DexReader> load(@Nullable File file, InputStream inputStream, String fileName) throws IOException {
		try (InputStream in = inputStream.markSupported() ? inputStream : new BufferedInputStream(inputStream)) {
			byte[] magic = new byte[DexConsts.MAX_MAGIC_SIZE];
			in.mark(magic.length);
			if (in.read(magic) != magic.length) {
				return Collections.emptyList();
			}
			if (isStartWithBytes(magic, DexConsts.DEX_FILE_MAGIC) || fileName.endsWith(".dex")) {
				in.reset();
				byte[] content = readAllBytes(in);
				return loadDexReaders(fileName, content);
			}
			if (file != null) {
				// allow only top level zip files
				if (isStartWithBytes(magic, DexConsts.ZIP_FILE_MAGIC) || CommonFileUtils.isZipFileExt(fileName)) {
					return collectDexFromZip(file);
				}
			}
			return Collections.emptyList();
		}
	}

	private List<DexReader> loadFromZipEntry(byte[] content, String fileName) {
		if (isStartWithBytes(content, DexConsts.DEX_FILE_MAGIC) || fileName.endsWith(".dex")) {
			return loadDexReaders(fileName, content);
		}
		return Collections.emptyList();
	}

	public List<DexReader> loadDexReaders(String fileName, byte[] content) {
		DexHeaderV41 dexHeaderV41 = DexHeaderV41.readIfPresent(content);
		if (dexHeaderV41 != null) {
			return DexHeaderV41.readSubDexOffsets(content, dexHeaderV41)
					.stream()
					.map(offset -> loadSingleDex(fileName, content, offset))
					.collect(Collectors.toList());
		}
		DexReader dexReader = loadSingleDex(fileName, content, 0);
		return Collections.singletonList(dexReader);
	}

	private DexReader loadSingleDex(String fileName, byte[] content, int offset) {
		if (options.isVerifyChecksum()) {
			DexCheckSum.verify(fileName, content, offset);
		}
		return new DexReader(getNextUniqId(), fileName, content, offset);
	}

	/**
	 * Since DEX v41, several sub DEX structures can be stored inside container of a single DEX file
	 * Use {@link DexFileLoader#loadDexReaders(String, byte[])} instead.
	 */
	@Deprecated
	public DexReader loadDexReader(String fileName, byte[] content) {
		return loadSingleDex(fileName, content, 0);
	}

	private List<DexReader> collectDexFromZip(File file) {
		List<DexReader> result = new ArrayList<>();
		try (ZipContent zip = zipReader.open(file)) {
			for (IZipEntry entry : zip.getEntries()) {
				if (entry.isDirectory()) {
					continue;
				}
				try {
					List<DexReader> readers;
					if (entry.preferBytes()) {
						readers = loadFromZipEntry(entry.getBytes(), entry.getName());
					} else {
						readers = load(null, entry.getInputStream(), entry.getName());
					}
					if (!readers.isEmpty()) {
						result.addAll(readers);
					}
				} catch (Exception e) {
					LOG.error("Failed to read zip entry: {}", entry, e);
				}
			}
		} catch (Exception e) {
			LOG.error("Failed to process zip file: {}", file.getAbsolutePath(), e);
		}
		return result;
	}

	private static boolean isStartWithBytes(byte[] fileMagic, byte[] expectedBytes) {
		int len = expectedBytes.length;
		if (fileMagic.length < len) {
			return false;
		}
		for (int i = 0; i < len; i++) {
			if (fileMagic[i] != expectedBytes[i]) {
				return false;
			}
		}
		return true;
	}

	private static byte[] readAllBytes(InputStream in) throws IOException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		byte[] data = new byte[8192];
		while (true) {
			int read = in.read(data);
			if (read == -1) {
				break;
			}
			buf.write(data, 0, read);
		}
		return buf.toByteArray();
	}

	private static synchronized int getNextUniqId() {
		dexUniqId++;
		if (dexUniqId >= 0xFFFF) {
			dexUniqId = 1;
		}
		return dexUniqId;
	}

	private static synchronized void resetDexUniqId() {
		dexUniqId = 1;
	}
}
