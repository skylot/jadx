package jadx.dex.instructions.args;

import jadx.dex.nodes.InsnNode;

public class InsnWrapArg extends InsnArg {

	private final InsnNode wrappedInsn;

	public InsnWrapArg(InsnNode insn) {
		ArgType type = (insn.getResult() == null ? ArgType.VOID : insn.getResult().getType());
		this.typedVar = new TypedVar(type);
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
		return "(wrap: " + typedVar + "\n  " + wrappedInsn + ")";
	}
}
