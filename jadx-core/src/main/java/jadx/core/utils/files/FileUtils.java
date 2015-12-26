package jadx.core.utils.files;

import jadx.core.utils.exceptions.JadxRuntimeException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class FileUtils {

	private FileUtils() {
	}

	public static void addFileToJar(JarOutputStream jar, File source, String entryName) throws IOException {
		BufferedInputStream in = null;
		try {
			JarEntry entry = new JarEntry(entryName);
			entry.setTime(source.lastModified());
			jar.putNextEntry(entry);
			in = new BufferedInputStream(new FileInputStream(source));

			byte[] buffer = new byte[8192];
			while (true) {
				int count = in.read(buffer);
				if (count == -1) {
					break;
				}
				jar.write(buffer, 0, count);
			}
			jar.closeEntry();
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	public static void makeDirsForFile(File file) {
		File dir = file.getParentFile();
		if (dir != null && !dir.exists()) {
			// if directory already created in other thread mkdirs will return false,
			// so check dir existence again
			if (!dir.mkdirs() && !dir.exists()) {
				throw new JadxRuntimeException("Can't create directory " + dir);
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
}
