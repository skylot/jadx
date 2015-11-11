package jadx.core.utils.files;

import jadx.core.utils.AsmUtils;
import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.exceptions.JadxException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.android.dex.Dex;

public class InputFile {
	private static final Logger LOG = LoggerFactory.getLogger(InputFile.class);

	private final File file;
	private final Dex dexBuf;
	public int nextDexIndex = -1;
	private final int dexIndex;

	public InputFile(File file) throws IOException, DecodeException {
		this(file, 0);
	}

	public InputFile(File file, int dexIndex) throws IOException, DecodeException {
		if (!file.exists()) {
			throw new IOException("File not found: " + file.getAbsolutePath());
		}
		this.dexIndex = dexIndex;
		this.file = file;
		this.dexBuf = loadDexBuffer();
	}

	private Dex loadDexBuffer() throws IOException, DecodeException {
		String fileName = file.getName();
		if (fileName.endsWith(".dex")) {
			return new Dex(file);
		}
		if (fileName.endsWith(".class")) {
			return loadFromClassFile(file);
		}
		if (fileName.endsWith(".apk") || fileName.endsWith(".zip")) {
			Dex dex = loadFromZip(this,file);
			if (dex == null) {
				throw new IOException("File 'classes.dex' not found in file: " + file);
			}
			return dex;
		}
		if (fileName.endsWith(".jar")) {
			// check if jar contains 'classes.dex'
			Dex dex = loadFromZip(this,file);
			if (dex != null) {
				return dex;
			}
			return loadFromJar(file);
		}
		throw new DecodeException("Unsupported input file format: " + file);
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

	private static Dex loadFromZip(InputFile ipf, File file) throws IOException {
		ZipFile zf = new ZipFile(file);
		String dexName = "classes.dex";
		String futureDexName = "classes2.dex";
		if (ipf.dexIndex != 0) {
			dexName = "classes" + ipf.dexIndex + ".dex";
			futureDexName = "classes" + (ipf.dexIndex + 1) + ".dex";
		}
		ZipEntry dex = zf.getEntry(dexName);
		if (dex == null) {
			zf.close();
			return null;
		}
		try {
			ZipEntry futureDex = zf.getEntry(futureDexName);
			if (futureDex != null) {
				ipf.nextDexIndex = ipf.dexIndex == 0 ? 2 : ipf.dexIndex + 1;
			}
		} catch (Exception ex) {
		}
		ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
		InputStream in = null;
		try {
			in = zf.getInputStream(dex);
			byte[] buffer = new byte[8192];
			int count;
			while ((count = in.read(buffer)) != -1) {
				bytesOut.write(buffer, 0, count);
			}
		} finally {
			if (in != null) {
				in.close();
			}
			zf.close();
		}
		return new Dex(bytesOut.toByteArray());
	}

	private static Dex loadFromClassFile(File file) throws IOException, DecodeException {
		File outFile = File.createTempFile("jadx-tmp-", System.nanoTime() + ".jar");
		outFile.deleteOnExit();
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

	public Dex getDexBuffer() {
		return dexBuf;
	}

	@Override
	public String toString() {
		return file.toString();
	}
}
