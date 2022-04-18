package jadx.core.dex.visitors.typeinference;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.RootNode;

/**
 * Allow to ignore down casts (return arg type instead cast type)
 * Such casts will be removed later.
 */
public final class TypeBoundCheckCastAssign implements ITypeBoundDynamic {
	private final RootNode root;
	private final IndexInsnNode insn;

	public TypeBoundCheckCastAssign(RootNode root, IndexInsnNode insn) {
		this.root = root;
		this.insn = insn;
	}

	@Override
	public BoundEnum getBound() {
		return BoundEnum.ASSIGN;
	}

	@Override
	public ArgType getType(TypeUpdateInfo updateInfo) {
		return getReturnType(updateInfo.getType(insn.getArg(0)));
	}

	@Override
	public ArgType getType() {
		return getReturnType(insn.getArg(0).getType());
	}

	private ArgType getReturnType(ArgType argType) {
		ArgType castType = (ArgType) insn.getIndex();
		TypeCompareEnum result = root.getTypeCompare().compareTypes(argType, castType);
		return result.isNarrow() ? argType : castType;
	}

	@Override
	public @Nullable RegisterArg getArg() {
		return insn.getResult();
	}

	public IndexInsnNode getInsn() {
		return insn;
	}

	@Override
	public String toString() {
		return "CHECK_CAST_ASSIGN{(" + insn.getIndex() + ") " + insn.getArg(0).getType() + "}";
	}
}
