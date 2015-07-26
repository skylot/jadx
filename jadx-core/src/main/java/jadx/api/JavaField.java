package jadx.api;

import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.FieldNode;

public final class JavaField implements JavaNode {

	private final FieldNode field;
	private final JavaClass parent;

	JavaField(FieldNode f, JavaClass cls) {
		this.field = f;
		this.parent = cls;
	}

	@Override
	public String getName() {
		return field.getAlias();
	}

	@Override
	public String getFullName() {
		return parent.getFullName() + "." + getName();
	}

	@Override
	public JavaClass getDeclaringClass() {
		return parent;
	}

	@Override
	public JavaClass getTopParentClass() {
		return parent.getTopParentClass();
	}

	public AccessInfo getAccessFlags() {
		return field.getAccessFlags();
	}

	public ArgType getType() {
		return field.getType();
	}

	public int getDecompiledLine() {
		return field.getDecompiledLine();
	}

	@Override
	public int hashCode() {
		return field.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return this == o || o instanceof JavaField && field.equals(((JavaField) o).field);
	}

	@Override
	public String toString() {
		return field.toString();
	}
}
