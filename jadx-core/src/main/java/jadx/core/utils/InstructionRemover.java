package jadx.core.utils;

import jadx.core.Consts;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for correct instructions removing,
 * can be used while iterating over instructions list
 */
public class InstructionRemover {

	private static final Logger LOG = LoggerFactory.getLogger(InstructionRemover.class);

	private final MethodNode mth;
	private final List<InsnNode> insns;
	private final List<InsnNode> toRemove;

	public InstructionRemover(MethodNode mth, BlockNode block) {
		this(mth, block.getInstructions());
	}

	public InstructionRemover(MethodNode mth, List<InsnNode> instructions) {
		this.mth = mth;
		this.insns = instructions;
		this.toRemove = new ArrayList<InsnNode>();
	}

	public void add(InsnNode insn) {
		toRemove.add(insn);
	}

	public void perform() {
		if (toRemove.isEmpty()) {
			return;
		}
		removeAll(insns, toRemove);
		toRemove.clear();
	}

	public static void unbindInsnList(MethodNode mth, List<InsnNode> unbind) {
		for (InsnNode rem : unbind) {
			unbindInsn(mth, rem);
		}
	}

	public static void unbindInsn(MethodNode mth, InsnNode insn) {
		RegisterArg r = insn.getResult();
		if (r != null && r.getSVar() != null) {
			if (Consts.DEBUG && r.getSVar().getUseCount() != 0) {
				LOG.debug("Unbind insn with result: {}", insn);
			}
			mth.removeSVar(r.getSVar());
		}
		for (InsnArg arg : insn.getArguments()) {
			unbindArgUsage(mth, arg);
		}
		insn.add(AFlag.INCONSISTENT_CODE);
	}

	public static void unbindArgUsage(MethodNode mth, InsnArg arg) {
		if (arg instanceof RegisterArg) {
			RegisterArg reg = (RegisterArg) arg;
			SSAVar sVar = reg.getSVar();
			if (sVar != null) {
				sVar.removeUse(reg);
			}
		} else if (arg instanceof InsnWrapArg) {
			InsnWrapArg wrap = (InsnWrapArg) arg;
			unbindInsn(mth, wrap.getWrapInsn());
		}
	}

	// Don't use 'insns.removeAll(toRemove)' because it will remove instructions by content
	// and here can be several instructions with same content
	private void removeAll(List<InsnNode> insns, List<InsnNode> toRemove) {
		for (InsnNode rem : toRemove) {
			unbindInsn(mth, rem);
			int insnsCount = insns.size();
			for (int i = 0; i < insnsCount; i++) {
				if (insns.get(i) == rem) {
					insns.remove(i);
					break;
				}
			}
		}
	}

	public static void remove(MethodNode mth, InsnNode insn) {
		BlockNode block = BlockUtils.getBlockByInsn(mth, insn);
		if (block != null) {
			remove(mth, block, insn);
		}
	}

	public static void remove(MethodNode mth, BlockNode block, InsnNode insn) {
		unbindInsn(mth, insn);
		// remove by pointer (don't use equals)
		Iterator<InsnNode> it = block.getInstructions().iterator();
		while (it.hasNext()) {
			InsnNode ir = it.next();
			if (ir == insn) {
				it.remove();
				return;
			}
		}
	}

	public static void remove(MethodNode mth, BlockNode block, int index) {
		List<InsnNode> instructions = block.getInstructions();
		unbindInsn(mth, instructions.get(index));
		instructions.remove(index);
	}

}
