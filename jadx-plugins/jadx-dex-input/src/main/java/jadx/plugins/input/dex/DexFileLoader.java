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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.utils.ZipSecurity;
import jadx.plugins.input.dex.sections.DexConsts;

public class DexFileLoader {
	private static final Logger LOG = LoggerFactory.getLogger(DexFileLoader.class);

	private static int dexUniqId = 1;

	public static List<DexReader> collectDexFiles(List<Path> pathsList) {
		return pathsList.stream()
				.map(Path::toFile)
				.map(DexFileLoader::loadDexFromFile)
				.filter(list -> !list.isEmpty())
				.flatMap(Collection::stream)
				.peek(dr -> LOG.debug("Loading dex: {}", dr))
				.collect(Collectors.toList());
	}

	private static List<DexReader> loadDexFromFile(File file) {
		try (InputStream inputStream = new FileInputStream(file)) {
			return checkFileMagic(file, inputStream, file.getAbsolutePath());
		} catch (Exception e) {
			LOG.error("File open error: {}", file.getAbsolutePath(), e);
			return Collections.emptyList();
		}
	}

	private static List<DexReader> checkFileMagic(File file, InputStream inputStream, String inputFileName) throws IOException {
		try (InputStream in = inputStream.markSupported() ? inputStream : new BufferedInputStream(inputStream)) {
			byte[] magic = new byte[DexConsts.MAX_MAGIC_SIZE];
			in.mark(magic.length);
			if (in.read(magic) != magic.length) {
				return Collections.emptyList();
			}
			if (isStartWithBytes(magic, DexConsts.DEX_FILE_MAGIC)) {
				in.reset();
				DexReader dexReader = new DexReader(getNextUniqId(), inputFileName, readAllBytes(in));
				return Collections.singletonList(dexReader);
			}
			if (file != null && isStartWithBytes(magic, DexConsts.ZIP_FILE_MAGIC)) {
				return collectDexFromZip(file);
			}
			return Collections.emptyList();
		}
	}

	private static List<DexReader> collectDexFromZip(File file) {
		List<DexReader> result = new ArrayList<>();
		try {
			ZipSecurity.readZipEntries(file, (entry, in) -> {
				try {
					result.addAll(checkFileMagic(null, in, entry.getName()));
				} catch (Exception e) {
					LOG.error("Failed to read zip entry: {}", entry, e);
				}
			});
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

	private static int getNextUniqId() {
		dexUniqId++;
		if (dexUniqId >= 0xFFFF) {
			resetDexUniqId();
		}
		return dexUniqId;
	}

	public static void resetDexUniqId() {
		dexUniqId = 1;
	}
}
