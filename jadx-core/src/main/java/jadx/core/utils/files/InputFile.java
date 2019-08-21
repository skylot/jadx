package jadx.core.utils.files;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.android.dex.Dex;
import com.android.dex.DexException;

import jadx.core.utils.AsmUtils;
import jadx.core.utils.SmaliUtils;
import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static jadx.core.utils.files.FileUtils.isApkFile;
import static jadx.core.utils.files.FileUtils.isZipDexFile;

public class InputFile {
	private static final Logger LOG = LoggerFactory.getLogger(InputFile.class);

	private final File file;
	private final List<DexFile> dexFiles = new ArrayList<>();

	public static void addFilesFrom(File file, List<InputFile> list, boolean skipSources) throws IOException, DecodeException {
		InputFile inputFile = new InputFile(file);
		inputFile.searchDexFiles(skipSources);
		list.add(inputFile);
	}

	private InputFile(File file) throws IOException {
		if (!file.exists()) {
			throw new IOException("File not found: " + file.getAbsolutePath());
		}
		this.file = file;
	}

	private void searchDexFiles(boolean skipSources) throws IOException, DecodeException {
		String fileName = file.getName();

		if (fileName.endsWith(".dex")) {
			addDexFile(fileName, file.toPath());
			return;
		}
		if (fileName.endsWith(".smali")) {
			Path output = FileUtils.createTempFile(".dex");
			SmaliUtils.assembleDex(output.toAbsolutePath().toString(), file.getAbsolutePath());
			addDexFile(fileName, output);
			return;
		}
		if (fileName.endsWith(".class")) {
			for (Path path : loadFromClassFile(file)) {
				addDexFile(fileName, path);
			}
			return;
		}
		if (isApkFile(file) || isZipDexFile(file)) {
			loadFromZip(".dex");
			return;
		}
		if (fileName.endsWith(".jar") || fileName.endsWith(".aar")) {
			// check if jar/aar contains '.dex' files
			if (loadFromZip(".dex")) {
				return;
			}
			if (fileName.endsWith(".jar")) {
				for (Path path : loadFromJar(file.toPath())) {
					addDexFile(fileName, path);
				}
				return;
			}
			if (fileName.endsWith(".aar")) {
				loadFromZip(".jar");
				return;
			}
			return;
		}
		if (skipSources) {
			return;
		}
		LOG.warn("No dex files found in {}", file);
	}

	private boolean loadFromZip(String ext) throws IOException, DecodeException {
		int index = 0;
		try (ZipFile zf = new ZipFile(file)) {
			// Input file could be .apk or .zip files
			// we should consider the input file could contain only one single dex, multi-dex,
			// or instantRun support dex for Android .apk files
			String instantRunDexSuffix = "classes" + ext;
			for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements();) {
				ZipEntry entry = e.nextElement();
				if (!ZipSecurity.isValidZipEntry(entry)) {
					continue;
				}

				String entryName = entry.getName();
				try (InputStream inputStream = zf.getInputStream(entry)) {
					if ((entryName.startsWith("classes") && entryName.endsWith(ext))
							|| entryName.endsWith(instantRunDexSuffix)) {
						switch (ext) {
							case ".dex":
								Path path = copyToTmpDex(entryName, inputStream);
								if (addDexFile(entryName, path)) {
									index++;
								}
								break;

							case ".jar":
								index++;
								Path jarFile = FileUtils.createTempFile(entryName);
								Files.copy(inputStream, jarFile, StandardCopyOption.REPLACE_EXISTING);
								for (Path p : loadFromJar(jarFile)) {
									addDexFile(entryName, p);
								}
								break;

							default:
								throw new JadxRuntimeException("Unexpected extension in zip: " + ext);
						}
					} else if (entryName.equals("instant-run.zip") && ext.equals(".dex")) {
						Path jarFile = FileUtils.createTempFile("instant-run.zip");
						Files.copy(inputStream, jarFile, StandardCopyOption.REPLACE_EXISTING);
						InputFile tempFile = new InputFile(jarFile.toFile());
						tempFile.loadFromZip(ext);
						List<DexFile> files = tempFile.getDexFiles();
						if (!files.isEmpty()) {
							index += files.size();
							this.dexFiles.addAll(files);
						}
					}
				}
			}
		}
		return index > 0;
	}

	private boolean addDexFile(String entryName, @Nullable Path filePath) {
		if (filePath == null) {
			return false;
		}
		Dex dexBuf = loadDexBufFromPath(filePath, entryName);
		if (dexBuf == null) {
			return false;
		}
		dexFiles.add(new DexFile(this, entryName, dexBuf, filePath));
		return true;
	}

	@Nullable
	private Dex loadDexBufFromPath(Path path, String entryName) {
		try {
			return new Dex(Files.readAllBytes(path));
		} catch (DexException e) {
			LOG.error("Failed to load dex file: {}, error: {}", entryName, e.getMessage());
		} catch (Exception e) {
			LOG.error("Failed to load dex file: {}, error: {}", entryName, e.getMessage(), e);
		}
		return null;
	}

	@Nullable
	private Path copyToTmpDex(String entryName, InputStream inputStream) {
		try {
			Path path = FileUtils.createTempFile(".dex");
			Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
			return path;
		} catch (Exception e) {
			LOG.error("Failed to load file: {}, error: {}", entryName, e.getMessage(), e);
			return null;
		}
	}

	private static List<Path> loadFromJar(Path jar) throws DecodeException {
		JavaToDex j2d = new JavaToDex();
		try {
			LOG.info("converting to dex: {} ...", jar.getFileName());
			List<Path> pathList = j2d.convert(jar);
			if (pathList.isEmpty()) {
				throw new JadxException("Empty dx output");
			}
			if (LOG.isDebugEnabled()) {
				LOG.debug("result dex files: {}", pathList);
			}
			return pathList;
		} catch (Exception e) {
			throw new DecodeException("java class to dex conversion error:\n " + e.getMessage(), e);
		} finally {
			if (j2d.isError()) {
				LOG.warn("dx message: {}", j2d.getDxErrors());
			}
		}
	}

	private static List<Path> loadFromClassFile(File file) throws IOException, DecodeException {
		Path outFile = FileUtils.createTempFile(".jar");
		try (JarOutputStream jo = new JarOutputStream(Files.newOutputStream(outFile))) {
			String clsName = AsmUtils.getNameFromClassFile(file);
			if (clsName == null || !ZipSecurity.isValidZipEntryName(clsName)) {
				throw new IOException("Can't read class name from file: " + file);
			}
			FileUtils.addFileToJar(jo, file, clsName + ".class");
		}
		return loadFromJar(outFile);
	}

	public File getFile() {
		return file;
	}

	public List<DexFile> getDexFiles() {
		return dexFiles;
	}

	@Override
	public String toString() {
		return file.getAbsolutePath();
	}
}
