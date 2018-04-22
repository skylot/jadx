package jadx.core.dex.attributes;

import java.util.Collections;
import java.util.List;

import jadx.core.dex.attributes.annotations.Annotation;

public final class EmptyAttrStorage extends AttributeStorage {

	@Override
	public boolean contains(AFlag flag) {
		return false;
	}

	@Override
	public <T extends IAttribute> boolean contains(AType<T> type) {
		return false;
	}

	@Override
	public <T extends IAttribute> T get(AType<T> type) {
		return null;
	}

	@Override
	public Annotation getAnnotation(String cls) {
		return null;
	}

	@Override
	public <T> List<T> getAll(AType<AttrList<T>> type) {
		return Collections.emptyList();
	}

	@Override
	public void clear() {
	}

	@Override
	public void remove(AFlag flag) {
	}

	@Override
	public <T extends IAttribute> void remove(AType<T> type) {
	}

	@Override
	public void remove(IAttribute attr) {
	}

	@Override
	public List<String> getAttributeStrings() {
		return Collections.emptyList();
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public String toString() {
		return "";
	}
}
