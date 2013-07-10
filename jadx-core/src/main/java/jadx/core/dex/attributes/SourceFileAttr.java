package jadx.core.dex.attributes;

public class SourceFileAttr implements IAttribute {

	private final String fileName;

	public SourceFileAttr(String fileName) {
		this.fileName = fileName;
	}

	public String getFileName() {
		return fileName;
	}

	@Override
	public AttributeType getType() {
		return AttributeType.SOURCE_FILE;
	}

	@Override
	public String toString() {
		return "SOURCE:" + fileName;
	}
}
