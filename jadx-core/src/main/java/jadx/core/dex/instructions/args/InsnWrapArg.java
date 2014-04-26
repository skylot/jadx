package jadx.core.dex.instructions.args;

import jadx.core.dex.nodes.InsnNode;

public final class InsnWrapArg extends InsnArg {

	private final InsnNode wrappedInsn;

	public InsnWrapArg(InsnNode insn) {
		RegisterArg result = insn.getResult();
		this.type = result != null ? result.getType() : ArgType.VOID;
		this.wrappedInsn = insn;
	}

	public InsnNode getWrapInsn() {
		return wrappedInsn;
	}

	@Override
	public void setParentInsn(InsnNode parentInsn) {
		assert parentInsn != wrappedInsn : "Can't wrap instruction info itself: " + parentInsn;
		this.parentInsn = parentInsn;
	}

	@Override
	public boolean isInsnWrap() {
		return true;
	}

	@Override
	public String toString() {
		return "(wrap: " + type + "\n  " + wrappedInsn + ")";
	}
}
