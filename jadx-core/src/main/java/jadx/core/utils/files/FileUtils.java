package jadx.core.utils.files;

import jadx.core.utils.exceptions.JadxRuntimeException;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtils {
	private static final Logger LOG = LoggerFactory.getLogger(FileUtils.class);

	public static final int READ_BUFFER_SIZE = 8 * 1024;
	private static final int MAX_FILENAME_LENGTH = 128;

	private FileUtils() {
	}

	public static void addFileToJar(JarOutputStream jar, File source, String entryName) throws IOException {
		BufferedInputStream in = null;
		try {
			JarEntry entry = new JarEntry(entryName);
			entry.setTime(source.lastModified());
			jar.putNextEntry(entry);

			in = new BufferedInputStream(new FileInputStream(source));
			copyStream(in, jar);
			jar.closeEntry();
		} finally {
			close(in);
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
		String name = file.getName();
		if (name.length() > MAX_FILENAME_LENGTH) {
			int dotIndex = name.indexOf('.');
			int cutAt = MAX_FILENAME_LENGTH - name.length() + dotIndex - 1;
			if (cutAt <= 0) {
				name = name.substring(0, MAX_FILENAME_LENGTH - 1);
			} else {
				name = name.substring(0, cutAt) + name.substring(dotIndex);
			}
			file = new File(file.getParentFile(), name);
		}
		makeDirsForFile(file);
		return file;
	}

	public static String bytesToHex(byte[] bytes) {
		char[] hexArray = "0123456789abcdef".toCharArray();
		if (bytes == null || bytes.length <= 0) {
			return null;
		}
		char[] hexChars = new char[bytes.length * 2];
		for ( int j = 0; j < bytes.length; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	public static boolean isZipfile(File file) {
		boolean isZipfile = false;
		InputStream is = null;
		try {
			byte[] headers = new byte[4];
			is = new FileInputStream(file);
			is.read(headers, 0, 4);
			System.out.println(bytesToHex(headers));
			String headerString = bytesToHex(headers);
			if (headerString.equals("504b0304")) {
				isZipfile = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return isZipfile;
	}

	public static List<String> getZipfileList(File file) {
		List<String> filelist = new ArrayList<String>();
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(file);
			Enumeration<? extends ZipEntry> entries = zipFile.entries();

			while(entries.hasMoreElements()){
				ZipEntry entry = entries.nextElement();
				filelist.add(entry.getName());
				System.out.println(entry.getName());
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}

		return filelist;
	}

	public static boolean isApkfile(File file) {
		boolean isApkfile = false;
		if (isZipfile(file)) {
			List<String> filelist = getZipfileList(file);
			if (filelist.contains("AndroidManifest.xml") && filelist.contains("classes.dex")) {
				isApkfile = true;
			}
		}
		return isApkfile;
	}

	public static boolean isZipDexfile(File file) {
		boolean isZipDexFile = false;
		if (isZipfile(file)) {
			List<String> filelist = getZipfileList(file);
			if (filelist.contains("classes.dex")) {
				isZipDexFile = true;
			}
		}

		return isZipDexFile;
	}
}
