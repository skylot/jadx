package jadx.api;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.RegisterArg;
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
		if (mth.getMethodInfo().getArgumentsTypes().isEmpty()) {
			return Collections.emptyList();
		}
		List<RegisterArg> arguments = mth.getArguments(false);
		Stream<ArgType> argTypeStream;
		if (arguments == null || arguments.isEmpty() || mth.isNoCode()) {
			argTypeStream = mth.getMethodInfo().getArgumentsTypes().stream();
		} else {
			argTypeStream = arguments.stream().map(RegisterArg::getType);
		}
		return argTypeStream
				.map(type -> ArgType.tryToResolveClassAlias(mth.dex(), type))
				.collect(Collectors.toList());
	}

	public ArgType getReturnType() {
		ArgType retType = mth.getReturnType();
		if (retType == null) {
			retType = mth.getMethodInfo().getReturnType();
		}
		return ArgType.tryToResolveClassAlias(mth.dex(), retType);
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
