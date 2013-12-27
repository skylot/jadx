package jadx.api;

import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.MethodNode;

import java.util.List;

public final class JavaMethod {
	private final MethodNode mth;
	private final JavaClass parent;

	public JavaMethod(JavaClass cls, MethodNode m) {
		this.parent = cls;
		this.mth = m;
	}

	public String getName() {
		return mth.getMethodInfo().getName();
	}

	public JavaClass getDeclaringClass() {
		return parent;
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
}
