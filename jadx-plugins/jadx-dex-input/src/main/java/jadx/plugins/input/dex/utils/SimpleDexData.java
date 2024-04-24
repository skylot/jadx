package jadx.plugins.input.dex.utils;

import java.util.Objects;

public class SimpleDexData implements IDexData {
	private final String fileName;
	private final byte[] content;

	public SimpleDexData(String fileName, byte[] content) {
		this.fileName = Objects.requireNonNull(fileName);
		this.content = Objects.requireNonNull(content);
	}

	@Override
	public String getFileName() {
		return fileName;
	}

	@Override
	public byte[] getContent() {
		return content;
	}

	@Override
	public String toString() {
		return "DexData{" + fileName + ", size=" + content.length + '}';
	}
}
