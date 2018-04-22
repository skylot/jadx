package jadx.api;

import java.util.List;

import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.MethodNode;

public final class JavaMethod implements JavaNode {
	private final MethodNode mth;
	private final JavaClass parent;

	JavaMethod(JavaClass cls, MethodNode m) {
		this.parent = cls;
		this.mth = m;
	}

	@Override
	public String getName() {
		return mth.getAlias();
	}

	@Override
	public String getFullName() {
		return mth.getMethodInfo().getFullName();
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
		return mth.getAccessFlags();
	}

	public List<ArgType> getArguments() {
		return mth.getMethodInfo().getArgumentsTypes();
	}

	public ArgType getReturnType() {
		return mth.getReturnType();
	}

	public boolean isConstructor() {
		return mth.getMethodInfo().isConstructor();
	}

	public boolean isClassInit() {
		return mth.getMethodInfo().isClassInit();
	}

	public int getDecompiledLine() {
		return mth.getDecompiledLine();
	}

	@Override
	public int hashCode() {
		return mth.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return this == o || o instanceof JavaMethod && mth.equals(((JavaMethod) o).mth);
	}

	@Override
	public String toString() {
		return mth.toString();
	}
}
