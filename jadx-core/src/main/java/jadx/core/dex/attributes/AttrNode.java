package jadx.core.dex.attributes;

import java.util.List;

import jadx.api.plugins.input.data.annotations.IAnnotation;

public abstract class AttrNode implements IAttributeNode {

	private static final AttributeStorage EMPTY_ATTR_STORAGE = new EmptyAttrStorage();

	private AttributeStorage storage = EMPTY_ATTR_STORAGE;

	@Override
	public void add(AFlag flag) {
		initStorage().add(flag);
	}

	@Override
	public void addAttr(IAttribute attr) {
		initStorage().add(attr);
	}

	@Override
	public <T> void addAttr(AType<AttrList<T>> type, T obj) {
		initStorage().add(type, obj);
	}

	@Override
	public void copyAttributesFrom(AttrNode attrNode) {
		AttributeStorage copyFrom = attrNode.storage;
		if (!copyFrom.isEmpty()) {
			initStorage().addAll(copyFrom);
		}
	}

	@Override
	public <T extends IAttribute> void copyAttributeFrom(AttrNode attrNode, AType<T> attrType) {
		IAttribute attr = attrNode.get(attrType);
		if (attr != null) {
			this.addAttr(attr);
		}
	}

	/**
	 * Remove attribute in this node, add copy from other if exists
	 */
	@Override
	public <T extends IAttribute> void rewriteAttributeFrom(AttrNode attrNode, AType<T> attrType) {
		remove(attrType);
		copyAttributeFrom(attrNode, attrType);
	}

	private AttributeStorage initStorage() {
		AttributeStorage store = storage;
		if (store == EMPTY_ATTR_STORAGE) {
			store = new AttributeStorage();
			storage = store;
		}
		return store;
	}

	private void unloadIfEmpty() {
		if (storage.isEmpty() && storage != EMPTY_ATTR_STORAGE) {
			storage = EMPTY_ATTR_STORAGE;
		}
	}

	@Override
	public boolean contains(AFlag flag) {
		return storage.contains(flag);
	}

	@Override
	public <T extends IAttribute> boolean contains(AType<T> type) {
		return storage.contains(type);
	}

	@Override
	public <T extends IAttribute> T get(AType<T> type) {
		return storage.get(type);
	}

	@Override
	public IAnnotation getAnnotation(String cls) {
		return storage.getAnnotation(cls);
	}

	@Override
	public <T> List<T> getAll(AType<AttrList<T>> type) {
		return storage.getAll(type);
	}

	@Override
	public void remove(AFlag flag) {
		storage.remove(flag);
		unloadIfEmpty();
	}

	@Override
	public <T extends IAttribute> void remove(AType<T> type) {
		storage.remove(type);
		unloadIfEmpty();
	}

	@Override
	public void removeAttr(IAttribute attr) {
		storage.remove(attr);
		unloadIfEmpty();
	}

	@Override
	public void clearAttributes() {
		storage.clear();
		unloadIfEmpty();
	}

	/**
	 * Remove all attribute with exceptions from {@link AType#SKIP_ON_UNLOAD}
	 */
	public void unloadAttributes() {
		if (storage == EMPTY_ATTR_STORAGE) {
			return;
		}
		storage.unloadAttributes();
		unloadIfEmpty();
	}

	@Override
	public List<String> getAttributesStringsList() {
		return storage.getAttributeStrings();
	}

	@Override
	public String getAttributesString() {
		return storage.toString();
	}

	@Override
	public boolean isAttrStorageEmpty() {
		return storage.isEmpty();
	}
}
