package jadx.core.dex.visitors.typeinference;

import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.RootNode;

/**
 * Special dynamic bound for invoke with generics.
 * Bound type calculated using instance generic type.
 * TODO: also can depends on argument types
 */
public final class TypeBoundInvokeAssign implements ITypeBoundDynamic {
	private final RootNode root;
	private final InvokeNode invokeNode;
	private final ArgType genericReturnType;

	public TypeBoundInvokeAssign(RootNode root, InvokeNode invokeNode, ArgType genericReturnType) {
		this.root = root;
		this.invokeNode = invokeNode;
		this.genericReturnType = genericReturnType;
	}

	@Override
	public BoundEnum getBound() {
		return BoundEnum.ASSIGN;
	}

	@Override
	public ArgType getType(TypeUpdateInfo updateInfo) {
		return getReturnType(updateInfo.getType(invokeNode.getArg(0)));
	}

	@Override
	public ArgType getType() {
		return getReturnType(invokeNode.getArg(0).getType());
	}

	private ArgType getReturnType(ArgType instanceType) {
		ArgType resultGeneric = root.getTypeUtils().replaceClassGenerics(instanceType, genericReturnType);
		if (resultGeneric != null && !resultGeneric.isWildcard()) {
			return resultGeneric;
		}
		return invokeNode.getCallMth().getReturnType();
	}

	@Override
	public RegisterArg getArg() {
		return invokeNode.getResult();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TypeBoundInvokeAssign that = (TypeBoundInvokeAssign) o;
		return invokeNode.equals(that.invokeNode);
	}

	@Override
	public int hashCode() {
		return invokeNode.hashCode();
	}

	@Override
	public String toString() {
		return "InvokeAssign{" + invokeNode.getCallMth().getShortId()
				+ ", returnType=" + genericReturnType
				+ ", currentType=" + getType()
				+ ", instanceArg=" + invokeNode.getArg(0)
				+ '}';
	}
}
