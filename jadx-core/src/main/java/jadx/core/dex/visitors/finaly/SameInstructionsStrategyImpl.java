package jadx.core.dex.visitors.finaly;

import java.util.Objects;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.RegDebugInfoAttr;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.InsnNode;

public final class SameInstructionsStrategyImpl extends SameInstructionsStrategy {

	private static boolean sameDebugInfo(RegisterArg dupReg, RegisterArg fReg) {
		RegDebugInfoAttr fDbgInfo = fReg.get(AType.REG_DEBUG_INFO);
		RegDebugInfoAttr dupDbgInfo = dupReg.get(AType.REG_DEBUG_INFO);
		if (fDbgInfo == null || dupDbgInfo == null) {
			return false;
		}
		return dupDbgInfo.equals(fDbgInfo);
	}

	private static boolean assignInsnDifferent(RegisterArg dupReg, RegisterArg fReg) {
		InsnNode assignInsn = fReg.getAssignInsn();
		InsnNode dupAssign = dupReg.getAssignInsn();
		if (assignInsn == null || dupAssign == null) {
			return true;
		}
		if (!assignInsn.isSame(dupAssign)) {
			return true;
		}
		if (assignInsn.isConstInsn() && dupAssign.isConstInsn()) {
			// Do this and not deep equals since we already know that the result is the same and that the insn
			// type is the same
			return !Objects.equals(assignInsn.getArguments(), assignInsn.getArguments());
		}
		return false;
	}

	@Override
	public boolean sameInsns(InsnNode dupInsn, InsnNode fInsn) {
		if (!dupInsn.isSame(fInsn)) {
			return false;
		}
		for (int i = 0; i < dupInsn.getArgsCount(); i++) {
			InsnArg dupArg = dupInsn.getArg(i);
			InsnArg fArg = fInsn.getArg(i);
			if (!isSameArgs(dupArg, fArg)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isSameArgs(InsnArg dupArg, InsnArg fArg) {
		if (dupArg == null) {
			return false;
		}
		boolean isReg = dupArg.isRegister();
		if (isReg != fArg.isRegister()) {
			return false;
		}
		if (isReg) {
			RegisterArg dupReg = (RegisterArg) dupArg;
			RegisterArg fReg = (RegisterArg) fArg;
			if (!dupReg.sameCodeVar(fReg)
					&& !sameDebugInfo(dupReg, fReg)
					&& assignInsnDifferent(dupReg, fReg)) {
				return false;
			}
		}
		boolean remConst = dupArg.isConst();
		if (remConst != fArg.isConst()) {
			return false;
		}
		return !(remConst && !dupArg.isSameConst(fArg));
	}
}
