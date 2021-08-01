package jadx.api.plugins.input.data.attributes.types;

import jadx.api.plugins.input.data.attributes.IJadxAttrType;
import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.api.plugins.input.data.attributes.PinnedAttribute;

public class SourceFileAttr extends PinnedAttribute {

	private final String fileName;

	public SourceFileAttr(String fileName) {
		this.fileName = fileName;
	}

	public String getFileName() {
		return fileName;
	}

	@Override
	public IJadxAttrType<SourceFileAttr> getAttrType() {
		return JadxAttrType.SOURCE_FILE;
	}

	@Override
	public String toString() {
		return "SOURCE:" + fileName;
	}
}
