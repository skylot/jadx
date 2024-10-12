package jadx.core.utils.files;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Deprecated zip file wrapper
 */
public class ZipFile extends java.util.zip.ZipFile {

	public ZipFile(File file) throws IOException {
		this(file, OPEN_READ);
	}

	public ZipFile(File file, int mode) throws IOException {
		this(file, mode, StandardCharsets.UTF_8);
	}

	public ZipFile(String name, Charset charset) throws IOException {
		this(new File(name), OPEN_READ, charset);
	}

	public ZipFile(String name) throws IOException {
		this(name, StandardCharsets.UTF_8);
	}

	public ZipFile(File file, int mode, Charset charset) throws IOException {
		super(file, mode, charset);
	}
}
