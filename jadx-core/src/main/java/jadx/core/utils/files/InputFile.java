package jadx.core.utils.files;

import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.exceptions.JadxRuntimeException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.android.dex.Dex;

public class InputFile {
	private static final Logger LOG = LoggerFactory.getLogger(InputFile.class);

	private final File file;
	private final Dex dexBuf;

	public InputFile(File file) throws IOException, DecodeException {
		this.file = file;
		if (!file.exists()) {
			throw new IOException("File not found: " + file.getAbsolutePath());
		}
		String fileName = file.getName();

		if (fileName.endsWith(".dex")) {
			this.dexBuf = new Dex(file);
		} else if (fileName.endsWith(".apk")) {
			this.dexBuf = new Dex(openDexFromApk(file));
		} else if (fileName.endsWith(".class") || fileName.endsWith(".jar")) {
			try {
				LOG.info("converting to dex: {} ...", fileName);
				JavaToDex j2d = new JavaToDex();
				byte[] ba = j2d.convert(file.getAbsolutePath());
				if (ba.length == 0) {
					throw new JadxException(
							j2d.isError() ? j2d.getDxErrors() : "Empty dx output");
				} else if (j2d.isError()) {
					LOG.warn("dx message: " + j2d.getDxErrors());
				}
				this.dexBuf = new Dex(ba);
			} catch (Throwable e) {
				throw new DecodeException(
						"java class to dex conversion error:\n " + e.getMessage(), e);
			}
		} else {
			throw new DecodeException("Unsupported input file: " + file);
		}
	}

	private byte[] openDexFromApk(File file) throws IOException {
		ZipFile zf = new ZipFile(file);
		ZipEntry dex = zf.getEntry("classes.dex");
		if (dex == null) {
			zf.close();
			throw new JadxRuntimeException("File 'classes.dex' not found in apk file: " + file);
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
		return bytesOut.toByteArray();
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
