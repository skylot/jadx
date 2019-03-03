package jadx.core.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.PhiInsn;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;

/**
 * Helper class for correct instructions removing,
 * can be used while iterating over instructions list
 */
public class InsnRemover {

	private final MethodNode mth;
	private final List<InsnNode> toRemove;
	private List<InsnNode> instrList;

	public InsnRemover(MethodNode mth) {
		this(mth, null);
	}

	public InsnRemover(MethodNode mth, BlockNode block) {
		this.mth = mth;
		this.toRemove = new ArrayList<>();
		if (block != null) {
			this.instrList = block.getInstructions();
		}
	}

	public void setBlock(BlockNode block) {
		this.instrList = block.getInstructions();
	}

	public void addAndUnbind(InsnNode insn) {
		toRemove.add(insn);
		unbindInsn(mth, insn);
	}

	public void addWithoutUnbind(InsnNode insn) {
		toRemove.add(insn);
	}

	public void perform() {
		if (toRemove.isEmpty()) {
			return;
		}
		removeAll(instrList, toRemove);
		toRemove.clear();
	}

	public static void unbindInsn(MethodNode mth, InsnNode insn) {
		for (InsnArg arg : insn.getArguments()) {
			unbindArgUsage(mth, arg);
		}
		if (insn.getType() == InsnType.PHI) {
			for (InsnArg arg : insn.getArguments()) {
				if (arg instanceof RegisterArg) {
					fixUsedInPhiFlag((RegisterArg) arg);
				}
			}
		}
		unbindResult(mth, insn);
		insn.add(AFlag.REMOVE);
	}

	public static void fixUsedInPhiFlag(RegisterArg useReg) {
		PhiInsn usedIn = null;
		for (RegisterArg reg : useReg.getSVar().getUseList()) {
			InsnNode parentInsn = reg.getParentInsn();
			if (parentInsn != null
					&& parentInsn.getType() == InsnType.PHI
					&& parentInsn.containsArg(useReg)) {
				usedIn = (PhiInsn) parentInsn;
			}
		}
		useReg.getSVar().setUsedInPhi(usedIn);
	}

	public static void unbindResult(MethodNode mth, InsnNode insn) {
		RegisterArg r = insn.getResult();
		if (r != null && r.getSVar() != null && mth != null) {
			SSAVar ssaVar = r.getSVar();
			if (ssaVar.getUseCount() == 0) {
				mth.removeSVar(ssaVar);
			}
		}
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

	// Don't use 'instrList.removeAll(toRemove)' because it will remove instructions by content
	// and here can be several instructions with same content
	private static void removeAll(List<InsnNode> insns, List<InsnNode> toRemove) {
		if (toRemove == null || toRemove.isEmpty()) {
			return;
		}
		for (InsnNode rem : toRemove) {
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

	public static void removeAllAndUnbind(MethodNode mth, BlockNode block, List<InsnNode> insns) {
		for (InsnNode insn : insns) {
			unbindInsn(mth, insn);
		}
		removeAll(block.getInstructions(), insns);
	}

	public static void remove(MethodNode mth, BlockNode block, int index) {
		List<InsnNode> instructions = block.getInstructions();
		unbindInsn(mth, instructions.get(index));
		instructions.remove(index);
	}
}
