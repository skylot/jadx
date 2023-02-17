package jadx.core.dex.attributes;

import java.util.List;

import jadx.api.CommentsLevel;
import jadx.api.plugins.input.data.annotations.IAnnotation;
import jadx.api.plugins.input.data.attributes.IJadxAttrType;
import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.core.Consts;
import jadx.core.dex.attributes.nodes.JadxCommentsAttr;
import jadx.core.utils.Utils;

public abstract class AttrNode implements IAttributeNode {

	private static final AttributeStorage EMPTY_ATTR_STORAGE = new EmptyAttrStorage();

	private AttributeStorage storage = EMPTY_ATTR_STORAGE;

	@Override
	public void add(AFlag flag) {
		initStorage().add(flag);
		if (Consts.DEBUG_ATTRIBUTES) {
			addDebugComment("Add flag " + flag + " at " + Utils.currentStackTrace(2));
		}
	}

	@Override
	public void addAttr(IJadxAttribute attr) {
		initStorage().add(attr);
		if (Consts.DEBUG_ATTRIBUTES) {
			addDebugComment("Add attribute " + attr.getClass().getSimpleName()
					+ ": " + attr + " at " + Utils.currentStackTrace(2));
		}
	}

	@Override
	public void addAttrs(List<IJadxAttribute> list) {
		initStorage().add(list);
	}

	@Override
	public <T> void addAttr(IJadxAttrType<AttrList<T>> type, T obj) {
		initStorage().add(type, obj);
		if (Consts.DEBUG_ATTRIBUTES) {
			addDebugComment("Add attribute " + obj + " at " + Utils.currentStackTrace(2));
		}
	}

	public <T> void addAttr(IJadxAttrType<AttrList<T>> type, List<T> list) {
		AttributeStorage strg = initStorage();
		list.forEach(attr -> strg.add(type, attr));
	}

	@Override
	public void copyAttributesFrom(AttrNode attrNode) {
		AttributeStorage copyFrom = attrNode.storage;
		if (!copyFrom.isEmpty()) {
			initStorage().addAll(copyFrom);
		}
	}

	@Override
	public <T extends IJadxAttribute> void copyAttributeFrom(AttrNode attrNode, AType<T> attrType) {
		IJadxAttribute attr = attrNode.get(attrType);
		if (attr != null) {
			this.addAttr(attr);
		}
	}

	/**
	 * Remove attribute in this node, add copy from other if exists
	 */
	@Override
	public <T extends IJadxAttribute> void rewriteAttributeFrom(AttrNode attrNode, AType<T> attrType) {
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
	public <T extends IJadxAttribute> boolean contains(IJadxAttrType<T> type) {
		return storage.contains(type);
	}

	@Override
	public <T extends IJadxAttribute> T get(IJadxAttrType<T> type) {
		return storage.get(type);
	}

	@Override
	public IAnnotation getAnnotation(String cls) {
		return storage.getAnnotation(cls);
	}

	@Override
	public <T> List<T> getAll(IJadxAttrType<AttrList<T>> type) {
		return storage.getAll(type);
	}

	@Override
	public void remove(AFlag flag) {
		storage.remove(flag);
		unloadIfEmpty();
	}

	@Override
	public <T extends IJadxAttribute> void remove(IJadxAttrType<T> type) {
		storage.remove(type);
		unloadIfEmpty();
	}

	@Override
	public void removeAttr(IJadxAttribute attr) {
		storage.remove(attr);
		unloadIfEmpty();
	}

	@Override
	public void clearAttributes() {
		storage.clear();
		unloadIfEmpty();
	}

	/**
	 * Remove all attribute
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

	private void addDebugComment(String msg) {
		JadxCommentsAttr commentsAttr = get(AType.JADX_COMMENTS);
		if (commentsAttr == null) {
			commentsAttr = new JadxCommentsAttr();
			initStorage().add(commentsAttr);
		}
		commentsAttr.add(CommentsLevel.DEBUG, msg);
	}
}
