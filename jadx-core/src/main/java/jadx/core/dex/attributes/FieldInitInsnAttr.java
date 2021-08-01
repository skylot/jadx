package jadx.core.dex.attributes;

import jadx.api.plugins.input.data.attributes.IJadxAttrType;
import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.api.plugins.input.data.attributes.PinnedAttribute;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;

import static java.util.Objects.requireNonNull;

public final class FieldInitInsnAttr extends PinnedAttribute {
	private final MethodNode mth;
	private final InsnNode insn;

	public FieldInitInsnAttr(MethodNode mth, InsnNode insn) {
		this.mth = requireNonNull(mth);
		this.insn = requireNonNull(insn);
	}

	public InsnNode getInsn() {
		return insn;
	}

	public MethodNode getInsnMth() {
		return mth;
	}

	@Override
	public IJadxAttrType<? extends IJadxAttribute> getAttrType() {
		return AType.FIELD_INIT_INSN;
	}

	@Override
	public String toString() {
		return "INIT{" + insn + '}';
	}
}
