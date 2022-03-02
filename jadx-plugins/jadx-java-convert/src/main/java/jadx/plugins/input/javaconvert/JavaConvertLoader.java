package jadx.plugins.input.javaconvert;

import java.io.IOException;
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

import jadx.api.plugins.utils.CommonFileUtils;
import jadx.api.plugins.utils.ZipSecurity;

public class JavaConvertLoader {
	private static final Logger LOG = LoggerFactory.getLogger(JavaConvertLoader.class);

	private final JavaConvertOptions options;

	public JavaConvertLoader(JavaConvertOptions options) {
		this.options = options;
	}

	public ConvertResult process(List<Path> input) {
		ConvertResult result = new ConvertResult();
		processJars(input, result);
		processAars(input, result);
		processClassFiles(input, result);
		return result;
	}

	private void processJars(List<Path> input, ConvertResult result) {
		PathMatcher jarMatcher = FileSystems.getDefault().getPathMatcher("glob:**.jar");
		input.stream()
				.filter(jarMatcher::matches)
				.forEach(path -> {
					try {
						convertJar(result, path);
					} catch (Exception e) {
						LOG.error("Failed to convert file: {}", path.toAbsolutePath(), e);
					}
				});
	}

	private void processClassFiles(List<Path> input, ConvertResult result) {
		PathMatcher jarMatcher = FileSystems.getDefault().getPathMatcher("glob:**.class");
		List<Path> clsFiles = input.stream()
				.filter(jarMatcher::matches)
				.collect(Collectors.toList());
		if (clsFiles.isEmpty()) {
			return;
		}
		try {
			LOG.debug("Converting class files ...");
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
			LOG.debug("Packed {} class files into jar: {}", clsFiles.size(), jarFile);
			convertJar(result, jarFile);
		} catch (Exception e) {
			LOG.error("Error process class files", e);
		}
	}

	private void processAars(List<Path> input, ConvertResult result) {
		PathMatcher aarMatcher = FileSystems.getDefault().getPathMatcher("glob:**.aar");
		input.stream()
				.filter(aarMatcher::matches)
				.forEach(path -> ZipSecurity.readZipEntries(path.toFile(), (entry, in) -> {
					try {
						String entryName = entry.getName();
						if (entryName.endsWith(".jar")) {
							Path tempJar = CommonFileUtils.saveToTempFile(in, ".jar");
							result.addTempPath(tempJar);
							LOG.debug("Loading jar: {} ...", entryName);
							convertJar(result, tempJar);
						}
					} catch (Exception e) {
						LOG.error("Failed to process zip entry: {}", entry, e);
					}
				}));
	}

	private void convertJar(ConvertResult result, Path path) throws Exception {
		if (repackAndConvertJar(result, path)) {
			return;
		}
		convertSimpleJar(result, path);
	}

	private boolean repackAndConvertJar(ConvertResult result, Path path) throws Exception {
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
		LOG.debug("Repacking jar file: {} ...", path.toAbsolutePath());
		Path jarFile = Files.createTempFile("jadx-classes-", ".jar");
		result.addTempPath(jarFile);
		try (JarOutputStream jo = new JarOutputStream(Files.newOutputStream(jarFile))) {
			ZipSecurity.readZipEntries(path.toFile(), (entry, in) -> {
				try {
					String entryName = entry.getName();
					if (entryName.endsWith(".class")) {
						if (entryName.endsWith("module-info.class")
								|| entryName.startsWith("META-INF/versions/")) {
							LOG.debug(" exclude: {}", entryName);
							return;
						}
						byte[] clsFileContent = CommonFileUtils.loadBytes(in);
						String clsName = AsmUtils.getNameFromClassFile(clsFileContent);
						if (clsName == null || !ZipSecurity.isValidZipEntryName(clsName)) {
							throw new IOException("Can't read class name from file: " + entryName);
						}
						addJarEntry(jo, clsName + ".class", clsFileContent, entry.getLastModifiedTime());
					} else if (entryName.endsWith(".jar")) {
						Path tempJar = CommonFileUtils.saveToTempFile(in, ".jar");
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

	private void convertSimpleJar(ConvertResult result, Path path) throws Exception {
		Path tempDirectory = Files.createTempDirectory("jadx-");
		result.addTempPath(tempDirectory);
		LOG.debug("Converting to dex ...");
		convert(path, tempDirectory);
		List<Path> dexFiles = collectFilesInDir(tempDirectory);
		LOG.debug("Converted {} to {} dex", path.toAbsolutePath(), dexFiles.size());
		result.addConvertedFiles(dexFiles);
	}

	private void convert(Path path, Path tempDirectory) {
		JavaConvertOptions.Mode mode = options.getMode();
		switch (mode) {
			case DX:
				try {
					DxConverter.run(path, tempDirectory);
				} catch (Throwable e) {
					LOG.error("DX convert failed, path: {}", path, e);
				}
				break;

			case D8:
				try {
					D8Converter.run(path, tempDirectory, options);
				} catch (Throwable e) {
					LOG.error("D8 convert failed, path: {}", path, e);
				}
				break;

			case BOTH:
				try {
					DxConverter.run(path, tempDirectory);
				} catch (Throwable e) {
					LOG.warn("DX convert failed, trying D8, path: {}", path);
					try {
						D8Converter.run(path, tempDirectory, options);
					} catch (Throwable ex) {
						LOG.error("D8 convert failed: {}", ex.getMessage());
					}
				}
				break;
		}
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
}
