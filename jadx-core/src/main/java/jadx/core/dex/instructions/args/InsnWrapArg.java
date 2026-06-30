package jadx.core.dex.instructions.args;

import org.jetbrains.annotations.NotNull;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.instructions.ConstStringNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

public final class InsnWrapArg extends InsnArg {

	private final InsnNode wrappedInsn;

	/**
	 * Use {@link InsnArg#wrapInsnIntoArg(InsnNode)} instead this constructor
	 */
	InsnWrapArg(@NotNull InsnNode insn) {
		RegisterArg result = insn.getResult();
		this.type = result != null ? result.getType() : ArgType.UNKNOWN;
		this.wrappedInsn = insn;
	}

	public InsnNode getWrapInsn() {
		return wrappedInsn;
	}

	public InsnNode unWrapWithCopy() {
		InsnNode copy = wrappedInsn.copyWithoutResult();
		copy.remove(AFlag.WRAPPED);
		return copy;
	}

	@Override
	public void setParentInsn(InsnNode parentInsn) {
		if (parentInsn == wrappedInsn) {
			throw new JadxRuntimeException("Can't wrap instruction info itself: " + parentInsn);
		}
		this.parentInsn = parentInsn;
	}

	@Override
	public InsnArg duplicate() {
		InsnNode wrapInsn = wrappedInsn;
		InsnNode wrapInsnCopy = wrapInsn.copyWithoutResult();
		if (wrapInsn.getResult() != null && wrapInsn.contains(AFlag.FORCE_ASSIGN_INLINE)) {
			// keep same SSA var in result arg, this will break previous version, mark it for removal
			wrapInsnCopy.setResult(wrapInsn.getResult().duplicate());
			wrapInsn.add(AFlag.DONT_GENERATE);
		}
		InsnWrapArg copy = new InsnWrapArg(wrapInsnCopy);
		copy.setType(type);
		return copyCommonParams(copy);
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
	public String toShortString() {
		if (wrappedInsn.getType() == InsnType.CONST_STR) {
			return "(\"" + ((ConstStringNode) wrappedInsn).getString() + "\")";
		}
		return "(wrap " + type + ":" + wrappedInsn.getType() + ')';
	}

	@Override
	public String toString() {
		if (wrappedInsn.getType() == InsnType.CONST_STR) {
			return "(\"" + ((ConstStringNode) wrappedInsn).getString() + "\")";
		}
		return "(wrap " + type + ":" + wrappedInsn + ')';
	}
}
