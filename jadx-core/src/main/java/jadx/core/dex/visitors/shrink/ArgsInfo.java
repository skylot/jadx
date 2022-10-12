package jadx.core.dex.visitors.shrink;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.mods.TernaryInsn;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.EmptyBitSet;
import jadx.core.utils.exceptions.JadxRuntimeException;

final class ArgsInfo {
	private final InsnNode insn;
	private final List<ArgsInfo> argsList;
	private final List<RegisterArg> args;
	private final int pos;
	private int inlineBorder;
	private ArgsInfo inlinedInsn;

	public ArgsInfo(InsnNode insn, List<ArgsInfo> argsList, int pos) {
		this.insn = insn;
		this.argsList = argsList;
		this.pos = pos;
		this.inlineBorder = pos;
		this.args = getArgs(insn);
	}

	public static List<RegisterArg> getArgs(InsnNode insn) {
		List<RegisterArg> args = new ArrayList<>();
		addArgs(insn, args);
		return args;
	}

	private static void addArgs(InsnNode insn, List<RegisterArg> args) {
		if (insn.getType() == InsnType.TERNARY) {
			args.addAll(((TernaryInsn) insn).getCondition().getRegisterArgs());
		}
		for (InsnArg arg : insn.getArguments()) {
			if (arg.isRegister()) {
				args.add((RegisterArg) arg);
			}
		}
		for (InsnArg arg : insn.getArguments()) {
			if (arg.isInsnWrap()) {
				addArgs(((InsnWrapArg) arg).getWrapInsn(), args);
			}
		}
	}

	public InsnNode getInsn() {
		return insn;
	}

	List<RegisterArg> getArgs() {
		return args;
	}

	public WrapInfo checkInline(int assignPos, RegisterArg arg) {
		if (assignPos >= inlineBorder || !canMove(assignPos, inlineBorder)) {
			return null;
		}
		inlineBorder = assignPos;
		return inline(assignPos, arg);
	}

	private boolean canMove(int from, int to) {
		ArgsInfo startInfo = argsList.get(from);
		List<RegisterArg> movedArgs = startInfo.getArgs();
		int start = from + 1;
		if (start == to) {
			// previous instruction or on edge of inline border
			return true;
		}
		if (start > to) {
			throw new JadxRuntimeException("Invalid inline insn positions: " + start + " - " + to);
		}
		BitSet movedSet;
		if (movedArgs.isEmpty()) {
			if (startInfo.insn.isConstInsn()) {
				return true;
			}
			movedSet = EmptyBitSet.EMPTY;
		} else {
			movedSet = new BitSet();
			for (RegisterArg arg : movedArgs) {
				movedSet.set(arg.getRegNum());
			}
		}
		boolean canReorder = startInfo.insn.canReorder();
		for (int i = start; i < to; i++) {
			ArgsInfo argsInfo = argsList.get(i);
			if (argsInfo.getInlinedInsn() == this) {
				continue;
			}
			InsnNode curInsn = argsInfo.insn;
			if (canReorder) {
				if (usedArgAssign(curInsn, movedSet)) {
					return false;
				}
			} else {
				if (!curInsn.canReorder() || usedArgAssign(curInsn, movedSet)) {
					return false;
				}
			}
		}
		return true;
	}

	static boolean usedArgAssign(InsnNode insn, BitSet args) {
		if (args.isEmpty()) {
			return false;
		}
		RegisterArg result = insn.getResult();
		if (result == null) {
			return false;
		}
		return args.get(result.getRegNum());
	}

	WrapInfo inline(int assignInsnPos, RegisterArg arg) {
		ArgsInfo argsInfo = argsList.get(assignInsnPos);
		argsInfo.inlinedInsn = this;
		return new WrapInfo(argsInfo.insn, arg);
	}

	ArgsInfo getInlinedInsn() {
		if (inlinedInsn != null) {
			ArgsInfo parent = inlinedInsn.getInlinedInsn();
			if (parent != null) {
				inlinedInsn = parent;
			}
		}
		return inlinedInsn;
	}

	@Override
	public String toString() {
		return "ArgsInfo: |" + inlineBorder
				+ " ->" + (inlinedInsn == null ? "-" : inlinedInsn.pos)
				+ ' ' + args + " : " + insn;
	}
}
