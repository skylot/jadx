package jadx.core.utils.files;

import com.android.dex.Dex;

public class DexFile {
	private final InputFile inputFile;
	private final String name;
	private final Dex dexBuf;

	public DexFile(InputFile inputFile, String name, Dex dexBuf) {
		this.inputFile = inputFile;
		this.name = name;
		this.dexBuf = dexBuf;
	}

	public String getName() {
		return name;
	}

	public Dex getDexBuf() {
		return dexBuf;
	}

	public InputFile getInputFile() {
		return inputFile;
	}

	@Override
	public String toString() {
		return inputFile + (name.isEmpty() ? "" : ":" + name);
	}
}
