package jadx.api.plugins.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonFileUtils {

	private static final Logger LOG = LoggerFactory.getLogger(CommonFileUtils.class);
	public static final File CWD = getCWD();
	public static final Path CWD_PATH = CWD.toPath();

	private static File getCWD() {
		try {
			return new File(".").getCanonicalFile();
		} catch (IOException e) {
			throw new RuntimeException("Failed to init current working dir constant", e);
		}
	}

	public static Path saveToTempFile(InputStream in, String suffix) throws IOException {
		return saveToTempFile(null, in, suffix);
	}

	public static Path saveToTempFile(byte[] dataPrefix, InputStream in, String suffix) throws IOException {
		Path tempFile = Files.createTempFile("jadx-temp-", suffix);
		try (OutputStream out = Files.newOutputStream(tempFile)) {
			if (dataPrefix != null) {
				out.write(dataPrefix);
			}
			copyStream(in, out);
		} catch (Exception e) {
			throw new IOException("Failed to save temp file", e);
		}
		return tempFile;
	}

	public static boolean safeDeleteFile(File file) {
		try {
			return file.delete();
		} catch (Exception e) {
			LOG.warn("Failed to delete file: {}", file, e);
			return false;
		}
	}

	public static byte[] loadBytes(InputStream input) throws IOException {
		return loadBytes(null, input);
	}

	public static byte[] loadBytes(byte[] dataPrefix, InputStream in) throws IOException {
		int estimateSize = dataPrefix == null ? in.available() : dataPrefix.length + in.available();
		try (ByteArrayOutputStream out = new ByteArrayOutputStream(estimateSize)) {
			if (dataPrefix != null) {
				out.write(dataPrefix);
			}
			copyStream(in, out);
			return out.toByteArray();
		} catch (Exception e) {
			throw new IOException("Failed to read input stream to bytes array", e);
		}
	}

	public static void copyStream(InputStream input, OutputStream output) throws IOException {
		byte[] buffer = new byte[8192];
		while (true) {
			int count = input.read(buffer);
			if (count == -1) {
				break;
			}
			output.write(buffer, 0, count);
		}
	}

	@Nullable
	public static String getFileExtension(String fileName) {
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex == -1) {
			return null;
		}
		return fileName.substring(dotIndex + 1);
	}

	public static String removeFileExtension(String fileName) {
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex == -1) {
			return fileName;
		}
		return fileName.substring(0, dotIndex);
	}

	private static final Set<String> ZIP_FILE_EXTS = Utils.constSet("zip", "jar", "apk");

	public static boolean isZipFileExt(String fileName) {
		String ext = getFileExtension(fileName);
		if (ext == null) {
			return false;
		}
		return ZIP_FILE_EXTS.contains(ext);
	}
}
