package jadx.core.dex.visitors.methods;

import java.util.Collections;
import java.util.List;

import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.IMethodDetails;

public class MutableMethodDetails implements IMethodDetails {

	private final MethodInfo mthInfo;
	private ArgType retType;
	private List<ArgType> argTypes;
	private List<ArgType> typeParams;
	private List<ArgType> throwTypes;
	private boolean varArg;
	private int accFlags;

	public MutableMethodDetails(IMethodDetails base) {
		this.mthInfo = base.getMethodInfo();
		this.retType = base.getReturnType();
		this.argTypes = Collections.unmodifiableList(base.getArgTypes());
		this.typeParams = Collections.unmodifiableList(base.getTypeParameters());
		this.throwTypes = Collections.unmodifiableList(base.getThrows());
		this.varArg = base.isVarArg();
		this.accFlags = base.getRawAccessFlags();
	}

	@Override
	public MethodInfo getMethodInfo() {
		return mthInfo;
	}

	@Override
	public ArgType getReturnType() {
		return retType;
	}

	@Override
	public List<ArgType> getArgTypes() {
		return argTypes;
	}

	@Override
	public List<ArgType> getTypeParameters() {
		return typeParams;
	}

	@Override
	public List<ArgType> getThrows() {
		return throwTypes;
	}

	@Override
	public boolean isVarArg() {
		return varArg;
	}

	public void setRetType(ArgType retType) {
		this.retType = retType;
	}

	public void setArgTypes(List<ArgType> argTypes) {
		this.argTypes = argTypes;
	}

	public void setTypeParams(List<ArgType> typeParams) {
		this.typeParams = typeParams;
	}

	public void setThrowTypes(List<ArgType> throwTypes) {
		this.throwTypes = throwTypes;
	}

	public void setVarArg(boolean varArg) {
		this.varArg = varArg;
	}

	@Override
	public int getRawAccessFlags() {
		return accFlags;
	}

	public void setRawAccessFlags(int accFlags) {
		this.accFlags = accFlags;
	}

	@Override
	public String toAttrString() {
		return IMethodDetails.super.toAttrString() + " (mut)";
	}

	@Override
	public String toString() {
		return "Mutable" + toAttrString();
	}
}
