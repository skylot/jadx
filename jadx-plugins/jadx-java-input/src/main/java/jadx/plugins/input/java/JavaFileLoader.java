package jadx.plugins.input.java;

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

public class JavaFileLoader {
	private static final Logger LOG = LoggerFactory.getLogger(JavaFileLoader.class);

	private static final int MAX_MAGIC_SIZE = 4;
	private static final byte[] JAVA_CLASS_FILE_MAGIC = { (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE };
	private static final byte[] ZIP_FILE_MAGIC = { 0x50, 0x4B, 0x03, 0x04 };

	private int classUniqId = 1;

	public List<JavaClassReader> collectFiles(List<Path> inputFiles) {
		return inputFiles.stream()
				.map(Path::toFile)
				.map(this::loadFromFile)
				.filter(list -> !list.isEmpty())
				.flatMap(Collection::stream)
				.collect(Collectors.toList());
	}

	private List<JavaClassReader> loadFromFile(File file) {
		try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
			return loadReader(file, inputStream, file.getAbsolutePath());
		} catch (Exception e) {
			LOG.error("File open error: {}", file.getAbsolutePath(), e);
			return Collections.emptyList();
		}
	}

	private List<JavaClassReader> loadReader(File file, InputStream in, String inputFileName) throws IOException {
		byte[] magic = new byte[MAX_MAGIC_SIZE];
		if (in.read(magic) != magic.length) {
			return Collections.emptyList();
		}
		if (isStartWithBytes(magic, JAVA_CLASS_FILE_MAGIC)) {
			byte[] data = loadBytes(magic, in);
			JavaClassReader reader = new JavaClassReader(getNextUniqId(), inputFileName, data);
			return Collections.singletonList(reader);
		}
		if (file != null && isStartWithBytes(magic, ZIP_FILE_MAGIC)) {
			return collectFromZip(file);
		}
		return Collections.emptyList();
	}

	private List<JavaClassReader> collectFromZip(File file) {
		List<JavaClassReader> result = new ArrayList<>();
		try {
			ZipSecurity.readZipEntries(file, (entry, in) -> {
				try {
					result.addAll(loadReader(null, in, entry.getName()));
				} catch (Exception e) {
					LOG.error("Failed to read zip entry: {}", entry, e);
				}
			});
		} catch (Exception e) {
			LOG.error("Failed to process zip file: {}", file.getAbsolutePath(), e);
		}
		return result;
	}

	public static boolean isStartWithBytes(byte[] fileMagic, byte[] expectedBytes) {
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

	public static byte[] loadBytes(byte[] prefix, InputStream in) throws IOException {
		int estimateSize = prefix.length + in.available();
		ByteArrayOutputStream out = new ByteArrayOutputStream(estimateSize);
		out.write(prefix);
		byte[] buffer = new byte[8 * 1024];
		while (true) {
			int len = in.read(buffer);
			if (len == -1) {
				break;
			}
			out.write(buffer, 0, len);
		}
		return out.toByteArray();
	}

	private int getNextUniqId() {
		return classUniqId++;
	}
}
