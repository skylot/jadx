package jadx.core.utils.files;

import static jadx.core.utils.files.FileUtils.isApkFile;
import static jadx.core.utils.files.FileUtils.isZipDexFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.android.dex.Dex;

import jadx.core.utils.AsmUtils;
import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class InputFile {
	private static final Logger LOG = LoggerFactory.getLogger(InputFile.class);

	private final File file;
	private final List<DexFile> dexFiles = new ArrayList<>();

	public static void addFilesFrom(File file, List<InputFile> list, boolean... skipSources) throws IOException, DecodeException {
		InputFile inputFile = new InputFile(file);
		inputFile.searchDexFiles(skipSources.length == 0 ? false : skipSources[0]);
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
			addDexFile(new Dex(file));
			return;
		}
		if (fileName.endsWith(".class")) {
			for (Dex dex : loadFromClassFile(file)) {
				addDexFile(dex);
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
				for (Dex dex : loadFromJar(file.toPath())) {
					addDexFile(dex);
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

	private void addDexFile(Dex dexBuf) {
		addDexFile("", dexBuf);
	}

	private void addDexFile(String fileName, Dex dexBuf) {
		dexFiles.add(new DexFile(this, fileName, dexBuf));
	}

	private boolean loadFromZip(String ext) throws IOException, DecodeException {
		int index = 0;
		try (ZipFile zf = new ZipFile(file)) {
			// Input file could be .apk or .zip files
			// we should consider the input file could contain only one single dex, multi-dex,
			// or instantRun support dex for Android .apk files
			String instantRunDexSuffix = "classes" + ext;
			for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements(); ) {
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
								Dex dexBuf = makeDexBuf(entryName, inputStream);
								if (dexBuf != null) {
									addDexFile(entryName, dexBuf);
									index++;
								}
								break;

							case ".jar":
								index++;
								Path jarFile = Files.createTempFile(entryName, ".jar");
								Files.copy(inputStream, jarFile);
								for (Dex dex : loadFromJar(jarFile)) {
									addDexFile(entryName, dex);
								}
								break;

							default:
								throw new JadxRuntimeException("Unexpected extension in zip: " + ext);
						}
					} else if (entryName.equals("instant-run.zip") && ext.equals(".dex")) {
						File jarFile = FileUtils.createTempFile("instant-run.zip");
						try (FileOutputStream fos = new FileOutputStream(jarFile)) {
							IOUtils.copy(inputStream, fos);
						}
						InputFile tempFile = new InputFile(jarFile);
						tempFile.loadFromZip(ext);
						List<DexFile> dexFiles = tempFile.getDexFiles();
						if (!dexFiles.isEmpty()) {
							index += dexFiles.size();
							this.dexFiles.addAll(dexFiles);
						}
					}
				}
			}
		}
		return index > 0;
	}

	@Nullable
	private Dex makeDexBuf(String entryName, InputStream inputStream) {
		try {
			return new Dex(inputStream);
		} catch (Exception e) {
			LOG.error("Failed to load file: {}, error: {}", entryName, e.getMessage(), e);
			return null;
		}
	}

	private static List<Dex> loadFromJar(Path jar) throws DecodeException {
		JavaToDex j2d = new JavaToDex();
		try {
			LOG.info("converting to dex: {} ...", jar.getFileName());
			List<byte[]> byteList = j2d.convert(jar);
			if (byteList.isEmpty()) {
				throw new JadxException("Empty dx output");
			}
			List<Dex> dexList = new ArrayList<>(byteList.size());
			for (byte[] b : byteList) {
				dexList.add(new Dex(b));
			}
			return dexList;
		} catch (Exception e) {
			throw new DecodeException("java class to dex conversion error:\n " + e.getMessage(), e);
		} finally {
			if (j2d.isError()) {
				LOG.warn("dx message: {}", j2d.getDxErrors());
			}
		}
	}

	private static List<Dex> loadFromClassFile(File file) throws IOException, DecodeException {
		Path outFile = Files.createTempFile("cls", ".jar");
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
