package jadx.core.dex.attributes.fldinit;

import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;

import static java.util.Objects.requireNonNull;

public final class FieldInitInsnAttr extends FieldInitAttr {
	private final MethodNode mth;
	private final InsnNode insn;

	FieldInitInsnAttr(MethodNode mth, InsnNode insn) {
		this.mth = requireNonNull(mth);
		this.insn = requireNonNull(insn);
	}

	@Override
	public InsnNode getInsn() {
		return insn;
	}

	@Override
	public MethodNode getInsnMth() {
		return mth;
	}

	@Override
	public boolean isInsn() {
		return true;
	}

	@Override
	public String toString() {
		return "INIT{" + insn + '}';
	}
}
