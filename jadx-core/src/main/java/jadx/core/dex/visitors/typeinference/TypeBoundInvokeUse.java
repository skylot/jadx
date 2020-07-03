package jadx.core.dex.visitors.typeinference;

import jadx.core.dex.instructions.BaseInvokeNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.RootNode;

/**
 * Special dynamic bound for invoke with generics.
 * Arguments bound type calculated using instance generic type.
 */
public final class TypeBoundInvokeUse implements ITypeBoundDynamic {
	private final RootNode root;
	private final BaseInvokeNode invokeNode;
	private final RegisterArg arg;
	private final ArgType genericArgType;

	public TypeBoundInvokeUse(RootNode root, BaseInvokeNode invokeNode, RegisterArg arg, ArgType genericArgType) {
		this.root = root;
		this.invokeNode = invokeNode;
		this.arg = arg;
		this.genericArgType = genericArgType;
	}

	@Override
	public BoundEnum getBound() {
		return BoundEnum.USE;
	}

	@Override
	public ArgType getType(TypeUpdateInfo updateInfo) {
		return getArgType(updateInfo.getType(invokeNode.getInstanceArg()), updateInfo.getType(arg));
	}

	@Override
	public ArgType getType() {
		return getArgType(invokeNode.getInstanceArg().getType(), arg.getType());
	}

	private ArgType getArgType(ArgType instanceType, ArgType argType) {
		ArgType resultGeneric = root.getTypeUtils().replaceClassGenerics(instanceType, genericArgType);
		if (resultGeneric != null) {
			return resultGeneric;
		}
		return argType;
	}

	@Override
	public RegisterArg getArg() {
		return arg;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TypeBoundInvokeUse that = (TypeBoundInvokeUse) o;
		return invokeNode.equals(that.invokeNode);
	}

	@Override
	public int hashCode() {
		return invokeNode.hashCode();
	}

	@Override
	public String toString() {
		return "InvokeAssign{" + invokeNode.getCallMth().getShortId()
				+ ", argType=" + genericArgType
				+ ", currentType=" + getType()
				+ ", instanceArg=" + invokeNode.getInstanceArg()
				+ '}';
	}
}
