package jadx.core.dex.attributes;

import java.util.List;

import jadx.api.plugins.input.data.annotations.IAnnotation;
import jadx.api.plugins.input.data.attributes.IJadxAttrType;
import jadx.api.plugins.input.data.attributes.IJadxAttribute;

public interface IAttributeNode {

	void add(AFlag flag);

	void addAttr(IJadxAttribute attr);

	void addAttrs(List<IJadxAttribute> list);

	<T> void addAttr(IJadxAttrType<AttrList<T>> type, T obj);

	void copyAttributesFrom(AttrNode attrNode);

	<T extends IJadxAttribute> void copyAttributeFrom(AttrNode attrNode, AType<T> attrType);

	<T extends IJadxAttribute> void rewriteAttributeFrom(AttrNode attrNode, AType<T> attrType);

	boolean contains(AFlag flag);

	<T extends IJadxAttribute> boolean contains(IJadxAttrType<T> type);

	<T extends IJadxAttribute> T get(IJadxAttrType<T> type);

	IAnnotation getAnnotation(String cls);

	<T> List<T> getAll(IJadxAttrType<AttrList<T>> type);

	void remove(AFlag flag);

	<T extends IJadxAttribute> void remove(IJadxAttrType<T> type);

	void removeAttr(IJadxAttribute attr);

	void clearAttributes();

	List<String> getAttributesStringsList();

	String getAttributesString();

	boolean isAttrStorageEmpty();
}
