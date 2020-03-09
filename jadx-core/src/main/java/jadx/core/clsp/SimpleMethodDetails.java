package jadx.core.clsp;

import java.util.Collections;
import java.util.List;

import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.GenericTypeParameter;
import jadx.core.dex.nodes.IMethodDetails;

public class SimpleMethodDetails implements IMethodDetails {

	private final MethodInfo methodInfo;

	public SimpleMethodDetails(MethodInfo methodInfo) {
		this.methodInfo = methodInfo;
	}

	@Override
	public MethodInfo getMethodInfo() {
		return methodInfo;
	}

	@Override
	public ArgType getReturnType() {
		return methodInfo.getReturnType();
	}

	@Override
	public List<ArgType> getArgTypes() {
		return methodInfo.getArgumentsTypes();
	}

	@Override
	public List<GenericTypeParameter> getTypeParameters() {
		return Collections.emptyList();
	}

	@Override
	public List<ArgType> getThrows() {
		return Collections.emptyList();
	}

	@Override
	public boolean isVarArg() {
		return false;
	}

	@Override
	public String toString() {
		return "SimpleMethodDetails{" + methodInfo + '}';
	}
}
