package jadx.gui.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.api.JavaVariable;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.JadxWrapper;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JField;
import jadx.gui.treemodel.JMethod;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JVariable;

public class JNodeCache {

	private final Map<JavaNode, JNode> cache = new ConcurrentHashMap<>();

	public JNode makeFrom(JavaNode javaNode) {
		if (javaNode == null) {
			return null;
		}
		JNode jNode = cache.get(javaNode);
		if (jNode == null) {
			jNode = convert(javaNode);
			cache.put(javaNode, jNode);
		}
		return jNode;
	}

	public JClass makeFrom(JavaClass javaCls) {
		if (javaCls == null) {
			return null;
		}
		return (JClass) cache.computeIfAbsent(javaCls,
				jn -> new JClass(javaCls, makeFrom(javaCls.getDeclaringClass())));
	}

	@Nullable
	public JNode renew(JadxWrapper wrapper, JNode node) {
		if (node instanceof JClass) {
			String rawName = ((JClass) node).getCls().getRawName();
			return makeFrom(wrapper.searchJavaClassByRawName(rawName));
		}
		return null;
	}

	private JNode convert(JavaNode node) {
		if (node == null) {
			return null;
		}
		if (node instanceof JavaClass) {
			return new JClass((JavaClass) node, makeFrom(node.getDeclaringClass()));
		}
		if (node instanceof JavaMethod) {
			return new JMethod((JavaMethod) node, makeFrom(node.getDeclaringClass()));
		}
		if (node instanceof JavaField) {
			return new JField((JavaField) node, makeFrom(node.getDeclaringClass()));
		}
		if (node instanceof JavaVariable) {
			JavaVariable javaVar = (JavaVariable) node;
			JMethod jMth = (JMethod) makeFrom(javaVar.getMth());
			return new JVariable(jMth, javaVar);
		}
		throw new JadxRuntimeException("Unknown type for JavaNode: " + node.getClass());
	}
}
