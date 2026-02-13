package jadx.core.dex.attributes;

import java.util.ArrayList;
import java.util.List;

import jadx.api.plugins.input.data.attributes.IJadxAttrType;
import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.core.utils.Utils;

public class AttrList<T> implements IJadxAttribute {

	private static final int MAX_ATTRLIST_LENGTH = 300;

	private final IJadxAttrType<AttrList<T>> type;
	private final List<T> list = new ArrayList<>();

	public AttrList(IJadxAttrType<AttrList<T>> type) {
		this.type = type;
	}

	public List<T> getList() {
		return list;
	}

	@Override
	public IJadxAttrType<AttrList<T>> getAttrType() {
		return type;
	}

	@Override
	public String toString() {
		String commaDelimited = Utils.listToString(list, ", ");
		// if the comma delimited list is too long, use newlines instead to maintain readability
		if (commaDelimited.length() > MAX_ATTRLIST_LENGTH) {
			return Utils.listToString(list, "\n    ");
		}
		return commaDelimited;
	}
}
