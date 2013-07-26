package jadx.api;

import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.MethodNode;

import java.util.List;

public class JavaMethod {
	private final MethodNode mth;

	public JavaMethod(MethodNode m) {
		this.mth = m;
	}

	public String getName() {
		MethodInfo mi = mth.getMethodInfo();
		if (mi.isConstructor()) {
			return mth.getParentClass().getShortName();
		} else if (mi.isClassInit()) {
			return "static";
		}
		return mi.getName();
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
