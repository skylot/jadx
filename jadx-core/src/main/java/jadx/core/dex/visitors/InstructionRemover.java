package jadx.core.dex.visitors;

import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Helper class for correct instructions removing,
 * can be used while iterating over instructions list
 */
public class InstructionRemover {

	private final List<InsnNode> insns;
	private final List<InsnNode> toRemove;

	public InstructionRemover(List<InsnNode> instructions) {
		this.insns = instructions;
		this.toRemove = new ArrayList<InsnNode>();
	}

	public void add(InsnNode insn) {
		toRemove.add(insn);
	}

	public void perform() {
		removeAll(insns, toRemove);
		toRemove.clear();
	}

	public static void unbindInsnList(List<InsnNode> unbind) {
		for (InsnNode rem : unbind)
			unbindInsn(rem);
	}

	public static void unbindInsn(InsnNode insn) {
		if (insn.getResult() != null) {
			InsnArg res = insn.getResult();
			res.getTypedVar().getUseList().remove(res);
		}
		for (InsnArg arg : insn.getArguments()) {
			if (arg.isRegister()) {
				arg.getTypedVar().getUseList().remove(arg);
			}
		}
	}

	public static void removeAll(BlockNode block, List<InsnNode> toRemove) {
		removeAll(block.getInstructions(), toRemove);
	}

	// Don't use 'insns.removeAll(toRemove)' because it will remove instructions by content
	// and here can be several instructions with same content
	public static void removeAll(List<InsnNode> insns, List<InsnNode> toRemove) {
		if (insns == toRemove) {
			for (InsnNode rem : toRemove)
				unbindInsn(rem);
			return;
		}

		for (InsnNode rem : toRemove) {
			unbindInsn(rem);
			for (Iterator<InsnNode> it = insns.iterator(); it.hasNext(); ) {
				InsnNode insn = it.next();
				if (insn == rem) {
					it.remove();
					break;
				}
			}
		}
	}

	public static void remove(BlockNode block, InsnNode insn) {
		unbindInsn(insn);
		// remove by pointer (don't use equals)
		for (Iterator<InsnNode> it = block.getInstructions().iterator(); it.hasNext(); ) {
			InsnNode ir = it.next();
			if (ir == insn) {
				it.remove();
				return;
			}
		}
	}

	public static void removeAllByContent(BlockNode block, List<InsnNode> toRemove) {
		for (InsnNode rem : toRemove) {
			unbindInsn(rem);
		}
		block.getInstructions().removeAll(toRemove);
	}

	public static void remove(BlockNode block, int index) {
		InsnNode insn = block.getInstructions().get(index);
		unbindInsn(insn);
		block.getInstructions().remove(index);
	}

}
