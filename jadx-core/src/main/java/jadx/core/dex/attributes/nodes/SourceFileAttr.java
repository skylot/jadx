package jadx.core.dex.attributes.nodes;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttribute;

public class SourceFileAttr implements IAttribute {

	private final String fileName;

	public SourceFileAttr(String fileName) {
		this.fileName = fileName;
	}

	public String getFileName() {
		return fileName;
	}

	@Override
	public AType<SourceFileAttr> getType() {
		return AType.SOURCE_FILE;
	}

	@Override
	public String toString() {
		return "SOURCE:" + fileName;
	}
}
