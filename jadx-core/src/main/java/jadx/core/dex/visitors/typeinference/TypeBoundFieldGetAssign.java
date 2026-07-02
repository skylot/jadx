package jadx.core.dex.visitors.typeinference;

import java.util.List;

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
		this.fieldInfo = (FieldInfo) getNode.getIndex();
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
		if (initType.isGenericType() || initType.isArray()) {
			// type variable unresolvable from the (raw) instance type: erase it (arrays element-wise)
			// to its leftmost bound, like javac, so the result type is valid in the current scope
			return eraseTypeVar(initType);
		}
		return initType;
	}

	private static ArgType eraseTypeVar(ArgType type) {
		if (type.isArray()) {
			// erase element, keep rank (T[] -> Object[])
			return ArgType.array(eraseTypeVar(type.getArrayElement()));
		}
		if (type.isGenericType()) {
			List<ArgType> bounds = type.getExtendTypes();
			if (bounds.isEmpty()) {
				return ArgType.OBJECT;
			}
			ArgType bound = bounds.get(0);
			if (bound.isGenericType()) {
				// bound is itself a type variable (<S extends T>)
				return ArgType.OBJECT;
			}
			// drop type args so we don't reintroduce an out-of-scope variable (Comparable<T> -> Comparable)
			return bound.isGeneric() ? ArgType.object(bound.getObject()) : bound;
		}
		if (type.isGeneric()) {
			// parameterized element (List<T>[]) -> raw
			return ArgType.object(type.getObject());
		}
		// already concrete (array element)
		return type;
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
