package jadx.plugins.input.java;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
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
import jadx.core.plugins.files.TempFilesGetter;
import jadx.core.utils.files.FileUtils;
import jadx.zip.IZipEntry;
import jadx.zip.ZipContent;
import jadx.zip.ZipReader;

public class JavaInputLoader {
	private static final Logger LOG = LoggerFactory.getLogger(JavaInputLoader.class);

	private static final int MAX_MAGIC_SIZE = 4;
	private static final byte[] JAVA_CLASS_FILE_MAGIC = { (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE };
	private static final byte[] ZIP_FILE_MAGIC = { 0x50, 0x4B, 0x03, 0x04 };

	private final ZipReader zipReader;
	private final Path tempPath;

	private int classUniqId = 1;

	public JavaInputLoader(ZipReader zipReader, Path tempPath) {
		this.zipReader = zipReader;
		this.tempPath = tempPath;
	}

	/**
	 * This will use zip reader with default options and ignore provided in jadx args
	 */
	@Deprecated
	public JavaInputLoader() {
		this(new ZipReader(), TempFilesGetter.INSTANCE.getTempDir());
	}

	public List<JavaClassReader> collectFiles(List<Path> inputFiles) {
		return inputFiles.stream()
				.map(Path::toFile)
				.map(this::loadFromFile)
				.filter(list -> !list.isEmpty())
				.flatMap(Collection::stream)
				.collect(Collectors.toList());
	}

	public List<JavaClassReader> loadInputStream(InputStream in, String name) throws IOException {
		return loadReader(in, name, null, null);
	}

	public JavaClassReader loadClass(byte[] content, String fileName) {
		return new JavaClassReader(getNextUniqId(), fileName, content);
	}

	private List<JavaClassReader> loadFromFile(File file) {
		try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
			return loadReader(inputStream, file.getName(), file, null);
		} catch (Exception e) {
			LOG.error("File open error: {}", file.getAbsolutePath(), e);
			return Collections.emptyList();
		}
	}

	private List<JavaClassReader> loadReader(InputStream in, String name,
			@Nullable File file, @Nullable String parentFileName) throws IOException {
		byte[] magic = new byte[MAX_MAGIC_SIZE];
		if (in.read(magic) != magic.length) {
			return Collections.emptyList();
		}
		if (isStartWithBytes(magic, JAVA_CLASS_FILE_MAGIC) || name.endsWith(".class")) {
			byte[] data = CommonFileUtils.loadBytes(magic, in);
			String source = concatSource(parentFileName, name);
			JavaClassReader reader = new JavaClassReader(getNextUniqId(), source, data);
			return Collections.singletonList(reader);
		}
		if (isStartWithBytes(magic, ZIP_FILE_MAGIC) || CommonFileUtils.isZipFileExt(name)) {
			if (file != null) {
				return collectFromZip(file, name);
			}
			File zipFile = CommonFileUtils.saveToTempFile(magic, in, ".zip").toFile();
			List<JavaClassReader> readers = collectFromZip(zipFile, concatSource(parentFileName, name));
			CommonFileUtils.safeDeleteFile(zipFile);
			return readers;
		}
		return Collections.emptyList();
	}

	private List<JavaClassReader> loadReaderFromZipEntry(byte[] content, String name, String parentFileName) throws IOException {
		if (isStartWithBytes(content, JAVA_CLASS_FILE_MAGIC) || name.endsWith(".class")) {
			String source = concatSource(parentFileName, name);
			JavaClassReader reader = new JavaClassReader(getNextUniqId(), source, content);
			return Collections.singletonList(reader);
		}
		if (isStartWithBytes(content, ZIP_FILE_MAGIC) || CommonFileUtils.isZipFileExt(name)) {
			Path tempZip = Files.createTempFile(tempPath, "temp", ".zip");
			FileUtils.writeFile(tempZip, content);
			File zipFile = tempZip.toFile();
			List<JavaClassReader> readers = collectFromZip(zipFile, concatSource(parentFileName, name));
			CommonFileUtils.safeDeleteFile(zipFile);
			return readers;
		}
		return Collections.emptyList();
	}

	private static String concatSource(@Nullable String parentFileName, String name) {
		if (parentFileName == null) {
			return name;
		}
		return parentFileName + ':' + name;
	}

	private List<JavaClassReader> collectFromZip(File file, String name) {
		List<JavaClassReader> result = new ArrayList<>();
		try (ZipContent zip = zipReader.open(file)) {
			for (IZipEntry entry : zip.getEntries()) {
				if (entry.isDirectory()) {
					continue;
				}
				String entryName = entry.getName();
				if (entryName.startsWith("META-INF/versions/")) {
					// skip classes for different java versions
					continue;
				}
				try {
					List<JavaClassReader> readers;
					if (entry.preferBytes()) {
						readers = loadReaderFromZipEntry(entry.getBytes(), entryName, name);
					} else {
						readers = loadReader(entry.getInputStream(), entryName, null, name);
					}
					result.addAll(readers);
				} catch (Exception e) {
					LOG.error("Failed to read zip entry: {}", entry, e);
				}
			}
		} catch (Exception e) {
			LOG.error("Failed to process zip file: {}", name, e);
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

	private int getNextUniqId() {
		return classUniqId++;
	}
}
