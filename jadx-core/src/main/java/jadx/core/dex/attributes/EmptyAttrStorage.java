package jadx.core.dex.attributes;

import java.util.Collections;
import java.util.List;

import jadx.api.plugins.input.data.annotations.IAnnotation;
import jadx.api.plugins.input.data.attributes.IJadxAttrType;
import jadx.api.plugins.input.data.attributes.IJadxAttribute;

public final class EmptyAttrStorage extends AttributeStorage {

	@Override
	public boolean contains(AFlag flag) {
		return false;
	}

	@Override
	public <T extends IJadxAttribute> boolean contains(IJadxAttrType<T> type) {
		return false;
	}

	@Override
	public <T extends IJadxAttribute> T get(IJadxAttrType<T> type) {
		return null;
	}

	@Override
	public IAnnotation getAnnotation(String cls) {
		return null;
	}

	@Override
	public <T> List<T> getAll(IJadxAttrType<AttrList<T>> type) {
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
	public <T extends IJadxAttribute> void remove(IJadxAttrType<T> type) {
		// ignore
	}

	@Override
	public void remove(IJadxAttribute attr) {
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
