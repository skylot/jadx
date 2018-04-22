package jadx.core.dex.attributes;

import java.util.List;

import jadx.core.dex.attributes.annotations.Annotation;

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

	private AttributeStorage initStorage() {
		AttributeStorage store = storage;
		if (store == EMPTY_ATTR_STORAGE) {
			store = new AttributeStorage();
			storage = store;
		}
		return store;
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
	public Annotation getAnnotation(String cls) {
		return storage.getAnnotation(cls);
	}

	@Override
	public <T> List<T> getAll(AType<AttrList<T>> type) {
		return storage.getAll(type);
	}

	@Override
	public void remove(AFlag flag) {
		storage.remove(flag);
	}

	@Override
	public <T extends IAttribute> void remove(AType<T> type) {
		storage.remove(type);
	}

	@Override
	public void removeAttr(IAttribute attr) {
		storage.remove(attr);
	}

	@Override
	public void clearAttributes() {
		storage.clear();
	}

	@Override
	public List<String> getAttributesStringsList() {
		return storage.getAttributeStrings();
	}

	@Override
	public String getAttributesString() {
		return storage.toString();
	}
}
