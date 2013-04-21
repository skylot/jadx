package jadx.dex.attributes;

import jadx.dex.nodes.InsnNode;

public class MethodInlineAttr implements IAttribute {

	private final InsnNode insn;

	public MethodInlineAttr(InsnNode insn) {
		this.insn = insn;
	}

	public InsnNode getInsn() {
		return insn;
	}

	@Override
	public AttributeType getType() {
		return AttributeType.METHOD_INLINE;
	}

	@Override
	public String toString() {
		return "INLINE: " + insn;
	}
}
