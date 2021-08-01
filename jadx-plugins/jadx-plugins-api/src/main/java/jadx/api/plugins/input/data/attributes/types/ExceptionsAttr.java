package jadx.api.plugins.input.data.attributes.types;

import java.util.List;

import jadx.api.plugins.input.data.attributes.IJadxAttrType;
import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.api.plugins.input.data.attributes.PinnedAttribute;

public class ExceptionsAttr extends PinnedAttribute {
	private final List<String> list;

	public ExceptionsAttr(List<String> list) {
		this.list = list;
	}

	public List<String> getList() {
		return list;
	}

	@Override
	public IJadxAttrType<ExceptionsAttr> getAttrType() {
		return JadxAttrType.EXCEPTIONS;
	}

	@Override
	public String toString() {
		return "EXCEPTIONS:" + list;
	}
}
