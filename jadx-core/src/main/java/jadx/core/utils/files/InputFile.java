package jadx.core.utils.files;

import jadx.core.utils.AsmUtils;
import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.exceptions.JadxRuntimeException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.android.dex.Dex;

public class InputFile {
	private static final Logger LOG = LoggerFactory.getLogger(InputFile.class);

	private final File file;
	private final List<DexFile> dexFiles = new ArrayList<DexFile>();

	public static void addFilesFrom(File file, List<InputFile> list) throws IOException, DecodeException {
		InputFile inputFile = new InputFile(file);
		inputFile.searchDexFiles();
		list.add(inputFile);
	}

	private InputFile(File file) throws IOException, DecodeException {
		if (!file.exists()) {
			throw new IOException("File not found: " + file.getAbsolutePath());
		}
		this.file = file;
	}

	private void searchDexFiles() throws IOException, DecodeException {
		String fileName = file.getName();
		if (fileName.endsWith(".dex")) {
			addDexFile(new Dex(file));
			return;
		}
		if (fileName.endsWith(".class")) {
			addDexFile(loadFromClassFile(file));
			return;
		}
		if (fileName.endsWith(".apk") || fileName.endsWith(".zip")) {
			loadFromZip(".dex");
			return;
		}
		if (fileName.endsWith(".jar")) {
			// check if jar contains '.dex' files
			if (loadFromZip(".dex")) {
				return;
			}
			addDexFile(loadFromJar(file));
			return;
		}
		if (fileName.endsWith(".aar")) {
			loadFromZip(".jar");
			return;
		}
		throw new DecodeException("Unsupported input file format: " + file);
	}

	private void addDexFile(Dex dexBuf) throws IOException {
		addDexFile("", dexBuf);
	}

	private void addDexFile(String fileName, Dex dexBuf) throws IOException {
		dexFiles.add(new DexFile(this, fileName, dexBuf));
	}

	private boolean loadFromZip(String ext) throws IOException, DecodeException {
		ZipFile zf = new ZipFile(file);
		int index = 0;
		while (true) {
			String entryName = "classes" + (index == 0 ? "" : index) + ext;
			ZipEntry entry = zf.getEntry(entryName);
			if (entry == null) {
				break;
			}
			InputStream inputStream = zf.getInputStream(entry);
			try {
				if (ext.equals(".dex")) {
					addDexFile(entryName, new Dex(inputStream));
				} else if (ext.equals(".jar")) {
					File jarFile = FileUtils.createTempFile(entryName);
					FileOutputStream fos = new FileOutputStream(jarFile);
					try {
						IOUtils.copy(inputStream, fos);
					} finally {
						fos.close();
					}
					addDexFile(entryName, loadFromJar(jarFile));
				} else {
					throw new JadxRuntimeException("Unexpected extension in zip: " + ext);
				}
			} finally {
				inputStream.close();
			}
			index++;
			if (index == 1) {
				index = 2;
			}
		}
		zf.close();
		return index > 0;
	}

	private static Dex loadFromJar(File jarFile) throws DecodeException {
		try {
			LOG.info("converting to dex: {} ...", jarFile.getName());
			JavaToDex j2d = new JavaToDex();
			byte[] ba = j2d.convert(jarFile.getAbsolutePath());
			if (ba.length == 0) {
				throw new JadxException(j2d.isError() ? j2d.getDxErrors() : "Empty dx output");
			} else if (j2d.isError()) {
				LOG.warn("dx message: {}", j2d.getDxErrors());
			}
			return new Dex(ba);
		} catch (Throwable e) {
			throw new DecodeException("java class to dex conversion error:\n " + e.getMessage(), e);
		}
	}

	private static Dex loadFromClassFile(File file) throws IOException, DecodeException {
		File outFile = FileUtils.createTempFile("cls.jar");
		FileOutputStream out = null;
		JarOutputStream jo = null;
		try {
			out = new FileOutputStream(outFile);
			jo = new JarOutputStream(out);
			String clsName = AsmUtils.getNameFromClassFile(file);
			if (clsName == null) {
				throw new IOException("Can't read class name from file: " + file);
			}
			FileUtils.addFileToJar(jo, file, clsName + ".class");
		} finally {
			if (jo != null) {
				jo.close();
			}
			if (out != null) {
				out.close();
			}
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
