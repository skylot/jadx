package jadx.core.dex.attributes;

import java.util.LinkedList;
import java.util.List;

import jadx.core.utils.Utils;

public class AttrList<T> implements IAttribute {

	private final AType<AttrList<T>> type;
	private final List<T> list = new LinkedList<>();

	public AttrList(AType<AttrList<T>> type) {
		this.type = type;
	}

	public List<T> getList() {
		return list;
	}

	@Override
	public AType<AttrList<T>> getType() {
		return type;
	}

	@Override
	public String toString() {
		return Utils.listToString(list);
	}
}
