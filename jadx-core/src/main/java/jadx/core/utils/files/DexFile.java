package jadx.core.utils.files;

import java.nio.file.Path;

import com.android.dex.Dex;

public class DexFile {
	private final InputFile inputFile;
	private final String name;
	private final Dex dexBuf;
	private final Path path;

	public DexFile(InputFile inputFile, String name, Dex dexBuf, Path path) {
		this.inputFile = inputFile;
		this.name = name;
		this.dexBuf = dexBuf;
		this.path = path;
	}

	public String getName() {
		return name;
	}

	public Dex getDexBuf() {
		return dexBuf;
	}

	public Path getPath() {
		return path;
	}

	public InputFile getInputFile() {
		return inputFile;
	}

	@Override
	public String toString() {
		return inputFile + (name.isEmpty() ? "" : ':' + name);
	}
}
