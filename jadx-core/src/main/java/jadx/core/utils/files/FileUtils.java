package jadx.core.utils.files;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.utils.exceptions.JadxRuntimeException;

public class FileUtils {
	private static final Logger LOG = LoggerFactory.getLogger(FileUtils.class);

	public static final int READ_BUFFER_SIZE = 8 * 1024;
	private static final int MAX_FILENAME_LENGTH = 128;

	private FileUtils() {
	}

	public static void addFileToJar(JarOutputStream jar, File source, String entryName) throws IOException {
		try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(source))) {
			JarEntry entry = new JarEntry(entryName);
			entry.setTime(source.lastModified());
			jar.putNextEntry(entry);

			copyStream(in, jar);
			jar.closeEntry();
		}
	}

	public static void makeDirsForFile(File file) {
		if (file != null) {
			makeDirs(file.getParentFile());
		}
	}

	private static final Object MKDIR_SYNC = new Object();

	public static void makeDirs(@Nullable File dir) {
		if (dir != null) {
			synchronized (MKDIR_SYNC) {
				if (!dir.exists() && !dir.mkdirs()) {
					throw new JadxRuntimeException("Can't create directory " + dir);
				}
			}
		}
	}

	public static File createTempFile(String suffix) {
		File temp;
		try {
			temp = File.createTempFile("jadx-tmp-", System.nanoTime() + "-" + suffix);
			temp.deleteOnExit();
		} catch (IOException e) {
			throw new JadxRuntimeException("Failed to create temp file with suffix: " + suffix);
		}
		return temp;
	}

	public static File createTempDir(String suffix) {
		try {
			Path path = Files.createTempDirectory("jadx-tmp-" + System.nanoTime() + "-" + suffix);
			path.toFile().deleteOnExit();
			return path.toFile();
		} catch (IOException e) {
			throw new JadxRuntimeException("Failed to create temp directory with suffix: " + suffix);
		}
	}

	public static void copyStream(InputStream input, OutputStream output) throws IOException {
		byte[] buffer = new byte[READ_BUFFER_SIZE];
		while (true) {
			int count = input.read(buffer);
			if (count == -1) {
				break;
			}
			output.write(buffer, 0, count);
		}
	}

	public static void close(Closeable c) {
		if (c == null) {
			return;
		}
		try {
			c.close();
		} catch (IOException e) {
			LOG.error("Close exception for {}", c, e);
		}
	}

	@NotNull
	public static File prepareFile(File file) {
		File saveFile = cutFileName(file);
		makeDirsForFile(saveFile);
		return saveFile;
	}

	private static File cutFileName(File file) {
		String name = file.getName();
		if (name.length() <= MAX_FILENAME_LENGTH) {
			return file;
		}
		int dotIndex = name.indexOf('.');
		int cutAt = MAX_FILENAME_LENGTH - name.length() + dotIndex - 1;
		if (cutAt <= 0) {
			name = name.substring(0, MAX_FILENAME_LENGTH - 1);
		} else {
			name = name.substring(0, cutAt) + name.substring(dotIndex);
		}
		return new File(file.getParentFile(), name);
	}

	private static String bytesToHex(byte[] bytes) {
		char[] hexArray = "0123456789abcdef".toCharArray();
		if (bytes == null || bytes.length <= 0) {
			return null;
		}
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	private static boolean isZipFile(File file) {
		try (InputStream is = new FileInputStream(file)) {
			byte[] headers = new byte[4];
			int read = is.read(headers, 0, 4);
			if (read == headers.length) {
				String headerString = bytesToHex(headers);
				if (Objects.equals(headerString, "504b0304")) {
					return true;
				}
			}
		} catch (Exception e) {
			LOG.error("Failed read zip file: {}", file.getAbsolutePath(), e);
		}
		return false;
	}

	private static List<String> getZipFileList(File file) {
		List<String> filesList = new ArrayList<>();
		try (ZipFile zipFile = new ZipFile(file)) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				filesList.add(entry.getName());
			}
		} catch (Exception e) {
			LOG.error("Error read zip file '{}'", file.getAbsolutePath(), e);
		}
		return filesList;
	}

	public static boolean isApkFile(File file) {
		if (!isZipFile(file)) {
			return false;
		}
		List<String> filesList = getZipFileList(file);
		return filesList.contains("AndroidManifest.xml")
				&& filesList.contains("classes.dex");
	}

	public static boolean isZipDexFile(File file) {
		if (!isZipFile(file) || !isZipFileCanBeOpen(file)) {
			return false;
		}
		List<String> filesList = getZipFileList(file);
		return filesList.contains("classes.dex");
	}

	private static boolean isZipFileCanBeOpen(File file) {
		try (ZipFile zipFile = new ZipFile(file)) {
			return zipFile.entries().hasMoreElements();
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean isCaseSensitiveFS(File testDir) {
		if (testDir != null) {
			File caseCheckUpper = new File(testDir, "CaseCheck");
			File caseCheckLow = new File(testDir, "casecheck");
			try {
				makeDirs(testDir);
				if (caseCheckUpper.createNewFile()) {
					boolean caseSensitive = !caseCheckLow.exists();
					LOG.debug("Filesystem at {} is {}case-sensitive", testDir.getAbsolutePath(),
							(caseSensitive ? "" : "NOT "));
					return caseSensitive;
				} else {
					LOG.debug("Failed to create file: {}", caseCheckUpper.getAbsolutePath());
				}
			} catch (Exception e) {
				LOG.debug("Failed to detect filesystem case-sensitivity by file creation", e);
			} finally {
				try {
					Files.deleteIfExists(caseCheckUpper.toPath());
					Files.deleteIfExists(caseCheckLow.toPath());
				} catch (Exception e) {
					// ignore
				}
			}
		}
		return IOCase.SYSTEM.isCaseSensitive();
	}

	public static File toFile(String path) {
		if (path == null) {
			return null;
		}
		return new File(path);
	}
}
