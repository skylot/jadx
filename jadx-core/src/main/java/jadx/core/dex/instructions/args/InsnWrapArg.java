package jadx.core.dex.instructions.args;

import jadx.core.dex.nodes.InsnNode;

import org.jetbrains.annotations.NotNull;

public final class InsnWrapArg extends InsnArg {

	private final InsnNode wrappedInsn;

	public InsnWrapArg(@NotNull InsnNode insn) {
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
	public int hashCode() {
		return wrappedInsn.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof InsnWrapArg)) {
			return false;
		}
		InsnWrapArg that = (InsnWrapArg) o;
		InsnNode thisInsn = wrappedInsn;
		InsnNode thatInsn = that.wrappedInsn;
		if (!thisInsn.isSame(thatInsn)) {
			return false;
		}
		int count = thisInsn.getArgsCount();
		for (int i = 0; i < count; i++) {
			if (!thisInsn.getArg(i).equals(thatInsn.getArg(i))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		return "(wrap: " + type + "\n  " + wrappedInsn + ")";
	}
}
