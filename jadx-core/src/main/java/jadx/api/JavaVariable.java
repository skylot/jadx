package jadx.api;

import java.util.Collections;
import java.util.List;

import jadx.core.dex.nodes.VariableNode;

public class JavaVariable implements JavaNode {
	JavaClass cls;
	VariableNode node;

	public JavaVariable(JavaClass cls, VariableNode node) {
		this.cls = cls;
		this.node = node;
	}

	public VariableNode getVariableNode() {
		return node;
	}

	@Override
	public String getName() {
		return node.getName();
	}

	@Override
	public String getFullName() {
		return node.getName();
	}

	@Override
	public JavaClass getDeclaringClass() {
		return cls;
	}

	@Override
	public JavaClass getTopParentClass() {
		return cls.getTopParentClass();
	}

	@Override
	public int getDecompiledLine() {
		return node.getDecompiledLine();
	}

	@Override
	public int getDefPos() {
		return node.getDefPosition();
	}

	@Override
	public List<JavaNode> getUseIn() {
		return Collections.emptyList();
	}

	@Override
	public int hashCode() {
		return node.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof JavaVariable) {
			return node.equals(((JavaVariable) obj).getVariableNode());
		}
		return false;
	}
}
