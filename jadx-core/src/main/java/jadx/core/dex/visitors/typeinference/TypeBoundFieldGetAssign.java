package jadx.core.dex.visitors.typeinference;

import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.RootNode;

/**
 * Dynamic bound for instance field get of generic type.
 * Bound type calculated using instance generic type.
 */
public final class TypeBoundFieldGetAssign implements ITypeBoundDynamic {
	private final RootNode root;
	private final IndexInsnNode getNode;
	private final FieldInfo fieldInfo;
	private final ArgType initType;

	public TypeBoundFieldGetAssign(RootNode root, IndexInsnNode getNode, ArgType initType) {
		this.root = root;
		this.getNode = getNode;
		this.fieldInfo = ((FieldInfo) getNode.getIndex());
		this.initType = initType;
	}

	@Override
	public BoundEnum getBound() {
		return BoundEnum.ASSIGN;
	}

	@Override
	public ArgType getType(TypeUpdateInfo updateInfo) {
		return getResultType(updateInfo.getType(getInstanceArg()));
	}

	@Override
	public ArgType getType() {
		return getResultType(getInstanceArg().getType());
	}

	private ArgType getResultType(ArgType instanceType) {
		ArgType resultGeneric = root.getTypeUtils().replaceClassGenerics(instanceType, initType);
		if (resultGeneric != null && !resultGeneric.isWildcard()) {
			return resultGeneric;
		}
		return initType; // TODO: check if this type is allowed in current scope
	}

	private InsnArg getInstanceArg() {
		return getNode.getArg(0);
	}

	@Override
	public RegisterArg getArg() {
		return getNode.getResult();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TypeBoundFieldGetAssign that = (TypeBoundFieldGetAssign) o;
		return getNode.equals(that.getNode);
	}

	@Override
	public int hashCode() {
		return getNode.hashCode();
	}

	@Override
	public String toString() {
		return "FieldGetAssign{" + fieldInfo
				+ ", type=" + getType()
				+ ", instanceArg=" + getInstanceArg()
				+ '}';
	}
}
