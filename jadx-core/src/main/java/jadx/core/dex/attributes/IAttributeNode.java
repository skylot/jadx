package jadx.core.dex.attributes;

import java.util.List;

import jadx.api.plugins.input.data.annotations.IAnnotation;

public interface IAttributeNode {

	void add(AFlag flag);

	void addAttr(IAttribute attr);

	<T> void addAttr(AType<AttrList<T>> type, T obj);

	void copyAttributesFrom(AttrNode attrNode);

	<T extends IAttribute> void copyAttributeFrom(AttrNode attrNode, AType<T> attrType);

	<T extends IAttribute> void rewriteAttributeFrom(AttrNode attrNode, AType<T> attrType);

	boolean contains(AFlag flag);

	<T extends IAttribute> boolean contains(AType<T> type);

	<T extends IAttribute> T get(AType<T> type);

	IAnnotation getAnnotation(String cls);

	<T> List<T> getAll(AType<AttrList<T>> type);

	void remove(AFlag flag);

	<T extends IAttribute> void remove(AType<T> type);

	void removeAttr(IAttribute attr);

	void clearAttributes();

	List<String> getAttributesStringsList();

	String getAttributesString();

	boolean isAttrStorageEmpty();
}
