package jadx.core.utils.files;

import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.exceptions.JadxException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.android.dx.io.DexBuffer;

public class InputFile {
	private static final Logger LOG = LoggerFactory.getLogger(InputFile.class);

	private final File file;
	private final DexBuffer dexBuf;

	public InputFile(File file) throws IOException, DecodeException {
		this.file = file;

		String fileName = file.getName();

		if (fileName.endsWith(".dex")) {
			this.dexBuf = new DexBuffer(file);
		} else if (fileName.endsWith(".apk")) {
			this.dexBuf = new DexBuffer(openDexFromApk(file));
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
				this.dexBuf = new DexBuffer(ba);
			} catch (Throwable e) {
				throw new DecodeException(
						"java class to dex conversion error:\n " + e.getMessage(), e);
			}
		} else
			throw new DecodeException("Unsupported input file: " + file);
	}

	private byte[] openDexFromApk(File file) throws IOException {
		ZipFile zf = new ZipFile(file);
		ZipEntry dex = zf.getEntry("classes.dex");
		ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
		try {
			InputStream in = zf.getInputStream(dex);

			byte[] buffer = new byte[8192];
			int count;
			while ((count = in.read(buffer)) != -1) {
				bytesOut.write(buffer, 0, count);
			}
			in.close();
		} finally {
			zf.close();
		}
		return bytesOut.toByteArray();
	}

	public File getFile() {
		return file;
	}

	public DexBuffer getDexBuffer() {
		return dexBuf;
	}

	@Override
	public String toString() {
		return file.toString();
	}

}
