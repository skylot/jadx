package jadx.plugins.input.javaconvert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.utils.ZipSecurity;

public class JavaConvertLoader {
	private static final Logger LOG = LoggerFactory.getLogger(JavaConvertLoader.class);

	public static ConvertResult process(List<Path> input) {
		ConvertResult result = new ConvertResult();
		processJars(input, result);
		processAars(input, result);
		processClassFiles(input, result);
		return result;
	}

	private static void processJars(List<Path> input, ConvertResult result) {
		PathMatcher jarMatcher = FileSystems.getDefault().getPathMatcher("glob:**.jar");
		input.stream()
				.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
				.filter(jarMatcher::matches)
				.forEach(path -> {
					try {
						convertJar(result, path);
					} catch (Exception e) {
						LOG.error("Failed to convert file: {}", path.toAbsolutePath(), e);
					}
				});
	}

	private static void processClassFiles(List<Path> input, ConvertResult result) {
		PathMatcher jarMatcher = FileSystems.getDefault().getPathMatcher("glob:**.class");
		List<Path> clsFiles = input.stream()
				.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
				.filter(jarMatcher::matches)
				.collect(Collectors.toList());
		if (clsFiles.isEmpty()) {
			return;
		}
		try {
			Path jarFile = Files.createTempFile("jadx-", ".jar");
			try (JarOutputStream jo = new JarOutputStream(Files.newOutputStream(jarFile))) {
				for (Path file : clsFiles) {
					String clsName = AsmUtils.getNameFromClassFile(file);
					if (clsName == null || !ZipSecurity.isValidZipEntryName(clsName)) {
						throw new IOException("Can't read class name from file: " + file);
					}
					addFileToJar(jo, file, clsName + ".class");
				}
			}
			result.addTempPath(jarFile);
			LOG.debug("Packed class files {} into jar {}", clsFiles, jarFile);
			convertJar(result, jarFile);
		} catch (Exception e) {
			LOG.error("Error process class files", e);
		}
	}

	private static void processAars(List<Path> input, ConvertResult result) {
		PathMatcher aarMatcher = FileSystems.getDefault().getPathMatcher("glob:**.aar");
		input.stream()
				.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
				.filter(aarMatcher::matches)
				.forEach(path -> ZipSecurity.readZipEntries(path.toFile(), (entry, in) -> {
					try {
						String entryName = entry.getName();
						if (entryName.endsWith(".jar")) {
							Path tempJar = saveInputStreamToFile(in, ".jar");
							result.addTempPath(tempJar);
							LOG.debug("Loading jar: {} ...", entryName);
							convertJar(result, tempJar);
						}
					} catch (Exception e) {
						LOG.error("Failed to process zip entry: {}", entry, e);
					}
				}));
	}

	private static void convertJar(ConvertResult result, Path path) throws Exception {
		if (repackAndConvertJar(result, path)) {
			return;
		}
		convertSimpleJar(result, path);
	}

	private static boolean repackAndConvertJar(ConvertResult result, Path path) throws Exception {
		// check if jar need a full repackage
		Boolean repackNeeded = ZipSecurity.visitZipEntries(path.toFile(), (zipFile, zipEntry) -> {
			String entryName = zipEntry.getName();
			if (zipEntry.isDirectory()) {
				if (entryName.equals("BOOT-INF/")) {
					return true; // Spring Boot jar
				}
				if (entryName.equals("META-INF/versions/")) {
					return true; // exclude duplicated classes
				}
			}
			if (entryName.endsWith(".jar")) {
				return true; // contains sub jars
			}
			if (entryName.endsWith("module-info.class")) {
				return true; // need to exclude module files
			}
			return null;
		});
		if (!Objects.equals(repackNeeded, Boolean.TRUE)) {
			return false;
		}

		Path jarFile = Files.createTempFile("jadx-classes-", ".jar");
		result.addTempPath(jarFile);
		try (JarOutputStream jo = new JarOutputStream(Files.newOutputStream(jarFile))) {
			ZipSecurity.readZipEntries(path.toFile(), (entry, in) -> {
				try {
					String entryName = entry.getName();
					if (entryName.endsWith(".class")) {
						if (entryName.endsWith("module-info.class")
								|| entryName.startsWith("META-INF/versions/")) {
							return;
						}
						byte[] clsFileContent = inputStreamToByteArray(in);
						String clsName = AsmUtils.getNameFromClassFile(clsFileContent);
						if (clsName == null || !ZipSecurity.isValidZipEntryName(clsName)) {
							throw new IOException("Can't read class name from file: " + entryName);
						}
						addJarEntry(jo, clsName + ".class", clsFileContent, entry.getLastModifiedTime());
					} else if (entryName.endsWith(".jar")) {
						Path tempJar = saveInputStreamToFile(in, ".jar");
						result.addTempPath(tempJar);
						convertJar(result, tempJar);
					}
				} catch (Exception e) {
					LOG.error("Failed to process jar entry: {} in {}", entry, path, e);
				}
			});
		}
		convertSimpleJar(result, jarFile);
		return true;
	}

	private static void convertSimpleJar(ConvertResult result, Path path) throws Exception {
		Path tempDirectory = Files.createTempDirectory("jadx-");
		result.addTempPath(tempDirectory);

		DxConverter.run(path, tempDirectory);

		LOG.debug("Converted to dex: {}", path.toAbsolutePath());
		result.addConvertedFiles(collectFilesInDir(tempDirectory));
	}

	private static List<Path> collectFilesInDir(Path tempDirectory) throws IOException {
		PathMatcher dexMatcher = FileSystems.getDefault().getPathMatcher("glob:**.dex");
		try (Stream<Path> pathStream = Files.walk(tempDirectory, 1)) {
			return pathStream
					.filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS))
					.filter(dexMatcher::matches)
					.collect(Collectors.toList());
		}
	}

	private static void addFileToJar(JarOutputStream jar, Path source, String entryName) throws IOException {
		byte[] fileContent = Files.readAllBytes(source);
		FileTime lastModifiedTime = Files.getLastModifiedTime(source, LinkOption.NOFOLLOW_LINKS);
		addJarEntry(jar, entryName, fileContent, lastModifiedTime);
	}

	private static void addJarEntry(JarOutputStream jar, String entryName, byte[] content,
			FileTime modTime) throws IOException {
		JarEntry entry = new JarEntry(entryName);
		if (modTime != null) {
			entry.setTime(modTime.toMillis());
		}
		jar.putNextEntry(entry);
		jar.write(content);
		jar.closeEntry();
	}

	private static void copyStream(InputStream input, OutputStream output) throws IOException {
		byte[] buffer = new byte[8 * 1024];
		while (true) {
			int count = input.read(buffer);
			if (count == -1) {
				break;
			}
			output.write(buffer, 0, count);
		}
	}

	private static byte[] inputStreamToByteArray(InputStream input) throws IOException {
		try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			copyStream(input, output);
			return output.toByteArray();
		}
	}

	private static Path saveInputStreamToFile(InputStream in, String suffix) throws IOException {
		Path tempJar = Files.createTempFile("jadx-temp-", suffix);
		try (OutputStream out = Files.newOutputStream(tempJar)) {
			copyStream(in, out);
		} catch (Exception e) {
			throw new IOException("Failed to save temp file", e);
		}
		return tempJar;
	}
}
