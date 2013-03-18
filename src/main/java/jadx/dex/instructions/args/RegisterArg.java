package jadx.dex.instructions.args;

import jadx.dex.instructions.IndexInsnNode;
import jadx.dex.instructions.InsnType;
import jadx.dex.nodes.InsnNode;
import jadx.dex.visitors.InstructionRemover;

public class RegisterArg extends InsnArg {
	protected final int regNum;

	public RegisterArg(int rn) {
		this.regNum = rn;
	}

	public RegisterArg(int rn, ArgType type) {
		this.typedVar = new TypedVar(type);
		this.regNum = rn;
	}

	public int getRegNum() {
		return regNum;
	}

	@Override
	public boolean isRegister() {
		return true;
	}

	public InsnNode getAssignInsn() {
		for (InsnArg arg : getTypedVar().getUseList()) {
			InsnNode assignInsn = arg.getParentInsn();
			if (assignInsn == null)
				// assign as function argument
				return null;
			else if (assignInsn.getResult() != null
					&& assignInsn.getResult().getRegNum() == regNum)
				return assignInsn;
		}
		return null;
	}

	/**
	 * Return constant value from register assign or null if not constant
	 * 
	 * @return LiteralArg, String or ArgType
	 */
	public Object getConstValue() {
		InsnNode parInsn = getAssignInsn();
		if (parInsn != null && parInsn.getType() == InsnType.CONST) {
			if (parInsn.getArgsCount() == 0) {
				// const in 'index' - string or class
				return ((IndexInsnNode) parInsn).getIndex();
			} else {
				return parInsn.getArg(0);
			}
		}
		return null;
	}

	@Override
	public boolean isThis() {
		if (isRegister()) {
			String name = getTypedVar().getName();
			if (name != null && name.equals("this"))
				return true;

			// maybe it was moved from 'this' register
			InsnNode ai = getAssignInsn();
			if (ai != null && ai.getType() == InsnType.MOVE) {
				if (ai.getArg(0).isThis()) {
					// actually we need to remove this instruction but we can't
					// because of iterating on instructions list
					// so unbind insn and rely on code shrinker
					InstructionRemover.unbindInsn(ai);
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return regNum * 31 + typedVar.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		RegisterArg other = (RegisterArg) obj;
		if (regNum != other.regNum) return false;
		if (!typedVar.equals(other.typedVar)) return false;
		return true;
	}

	@Override
	public String toString() {
		return "(r" + regNum + " " + typedVar + ")";
	}
}
