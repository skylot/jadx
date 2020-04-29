package jadx.core.dex.attributes;

import java.util.Collections;
import java.util.List;

import jadx.api.plugins.input.data.annotations.IAnnotation;

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
	public IAnnotation getAnnotation(String cls) {
		return null;
	}

	@Override
	public <T> List<T> getAll(AType<AttrList<T>> type) {
		return Collections.emptyList();
	}

	@Override
	public void clear() {
		// ignore
	}

	@Override
	public void remove(AFlag flag) {
		// ignore
	}

	@Override
	public <T extends IAttribute> void remove(AType<T> type) {
		// ignore
	}

	@Override
	public void remove(IAttribute attr) {
		// ignore
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
