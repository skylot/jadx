package jadx.gui.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.api.JavaVariable;
import jadx.core.utils.exceptions.JadxRuntimeException;
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
		// don't use 'computeIfAbsent' method here, it will cause 'Recursive update' exception
		JNode jNode = cache.get(javaNode);
		if (jNode == null || jNode.getJavaNode() != javaNode) {
			jNode = convert(javaNode);
			cache.put(javaNode, jNode);
		}
		return jNode;
	}

	public JClass makeFrom(JavaClass javaCls) {
		if (javaCls == null) {
			return null;
		}
		JClass jCls = (JClass) cache.get(javaCls);
		if (jCls == null || jCls.getCls() != javaCls) {
			jCls = convert(javaCls);
			cache.put(javaCls, jCls);
		}
		return jCls;
	}

	public void remove(JavaNode javaNode) {
		cache.remove(javaNode);
	}

	public void removeWholeClass(JavaClass javaCls) {
		remove(javaCls);
		javaCls.getMethods().forEach(this::remove);
		javaCls.getFields().forEach(this::remove);
		javaCls.getInnerClasses().forEach(this::remove);
		javaCls.getInlinedClasses().forEach(this::remove);
	}

	private JClass convert(JavaClass cls) {
		JavaClass parentCls = cls.getDeclaringClass();
		if (parentCls == cls) {
			return new JClass(cls, null);
		}
		return new JClass(cls, makeFrom(parentCls));
	}

	private JNode convert(JavaNode node) {
		if (node == null) {
			return null;
		}
		if (node instanceof JavaClass) {
			return convert(((JavaClass) node));
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
