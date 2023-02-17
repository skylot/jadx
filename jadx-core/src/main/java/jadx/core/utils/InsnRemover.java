package jadx.core.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import jadx.api.ICodeWriter;
import jadx.core.Consts;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.PhiInsn;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static jadx.core.utils.InsnUtils.isInsnType;
import static jadx.core.utils.ListUtils.allMatch;

/**
 * Helper class for correct instructions removing,
 * can be used while iterating over instructions list
 */
public class InsnRemover {

	private final MethodNode mth;
	private final List<InsnNode> toRemove;
	@Nullable
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
		if (instrList == null) {
			for (InsnNode remInsn : toRemove) {
				remove(mth, remInsn);
			}
		} else {
			unbindInsns(mth, toRemove);
			removeAll(instrList, toRemove);
		}
		toRemove.clear();
	}

	public static void unbindInsn(@Nullable MethodNode mth, InsnNode insn) {
		unbindAllArgs(mth, insn);
		unbindResult(mth, insn);
		insn.add(AFlag.DONT_GENERATE);
	}

	public static void unbindInsns(@Nullable MethodNode mth, List<InsnNode> insns) {
		// remove all usage first so on result unbind we can remove unused ssa vars
		insns.forEach(insn -> unbindAllArgs(mth, insn));
		insns.forEach(insn -> {
			unbindResult(mth, insn);
			insn.add(AFlag.DONT_GENERATE);
		});
	}

	public static void unbindAllArgs(@Nullable MethodNode mth, InsnNode insn) {
		for (InsnArg arg : insn.getArguments()) {
			unbindArgUsage(mth, arg);
		}
		if (insn.getType() == InsnType.PHI) {
			for (InsnArg arg : insn.getArguments()) {
				if (arg instanceof RegisterArg) {
					((RegisterArg) arg).getSVar().updateUsedInPhiList();
				}
			}
		}
		insn.add(AFlag.REMOVE);
		insn.add(AFlag.DONT_GENERATE);
	}

	public static void unbindResult(@Nullable MethodNode mth, InsnNode insn) {
		RegisterArg r = insn.getResult();
		if (r == null) {
			return;
		}
		if (mth != null) {
			SSAVar ssaVar = r.getSVar();
			if (ssaVar != null && ssaVar.getAssignInsn() == insn /* can be already reassigned */) {
				removeSsaVar(mth, ssaVar);
			}
		}
		insn.setResult(null);
	}

	private static void removeSsaVar(MethodNode mth, SSAVar ssaVar) {
		int useCount = ssaVar.getUseCount();
		if (useCount == 0) {
			mth.removeSVar(ssaVar);
			return;
		}
		// check if all usage only in PHI insns
		if (allMatch(ssaVar.getUseList(), arg -> isInsnType(arg.getParentInsn(), InsnType.PHI))) {
			for (RegisterArg arg : new ArrayList<>(ssaVar.getUseList())) {
				InsnNode parentInsn = arg.getParentInsn();
				if (parentInsn != null) {
					((PhiInsn) parentInsn).removeArg(arg);
				}
			}
			mth.removeSVar(ssaVar);
			return;
		}
		// check if all usage only in not generated instructions
		if (allMatch(ssaVar.getUseList(),
				arg -> arg.contains(AFlag.DONT_GENERATE) || (InsnUtils.contains(arg.getParentInsn(), AFlag.DONT_GENERATE)))) {
			for (RegisterArg arg : ssaVar.getUseList()) {
				arg.resetSSAVar();
			}
			mth.removeSVar(ssaVar);
			return;
		}
		throw new JadxRuntimeException("Can't remove SSA var: " + ssaVar + ", still in use, count: " + useCount
				+ ", list:" + ICodeWriter.NL + "  " + ssaVar.getUseList().stream()
						.map(arg -> arg + " from " + arg.getParentInsn())
						.collect(Collectors.joining(ICodeWriter.NL + "  ")));
	}

	public static void unbindArgUsage(@Nullable MethodNode mth, InsnArg arg) {
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
			boolean found = false;
			for (int i = 0; i < insnsCount; i++) {
				if (insns.get(i) == rem) {
					insns.remove(i);
					found = true;
					break;
				}
			}
			if (!found && Consts.DEBUG_WITH_ERRORS) {
				throw new JadxRuntimeException("Can't remove insn:"
						+ ICodeWriter.NL + "  " + rem
						+ ICodeWriter.NL + " not found in list:"
						+ ICodeWriter.NL + "  " + Utils.listToString(insns, ICodeWriter.NL + "  "));
			}
		}
	}

	public static void remove(MethodNode mth, InsnNode insn) {
		if (insn.contains(AFlag.WRAPPED)) {
			unbindInsn(mth, insn);
			return;
		}
		BlockNode block = BlockUtils.getBlockByInsn(mth, insn);
		if (block != null) {
			remove(mth, block, insn);
		} else {
			insn.add(AFlag.DONT_GENERATE);
			mth.addWarnComment("Not found block with instruction: " + insn);
		}
	}

	public static void remove(MethodNode mth, BlockNode block, InsnNode insn) {
		unbindInsn(mth, insn);
		removeWithoutUnbind(mth, block, insn);
	}

	public static boolean removeWithoutUnbind(MethodNode mth, BlockNode block, InsnNode insn) {
		// remove by pointer (don't use equals)
		Iterator<InsnNode> it = block.getInstructions().iterator();
		while (it.hasNext()) {
			InsnNode ir = it.next();
			if (ir == insn) {
				it.remove();
				return true;
			}
		}
		if (!insn.contains(AFlag.WRAPPED)) {
			mth.addWarnComment("Failed to remove instruction: " + insn + " from block: " + block);
		}
		return false;
	}

	public static void removeAllAndUnbind(MethodNode mth, BlockNode block, List<InsnNode> insns) {
		unbindInsns(mth, insns);
		removeAll(block.getInstructions(), insns);
	}

	public static void removeAllAndUnbind(MethodNode mth, IContainer container, List<InsnNode> insns) {
		unbindInsns(mth, insns);
		RegionUtils.visitBlocks(mth, container, b -> removeAll(b.getInstructions(), insns));
	}

	public static void removeAllAndUnbind(MethodNode mth, List<InsnNode> insns) {
		unbindInsns(mth, insns);
		for (BlockNode block : mth.getBasicBlocks()) {
			removeAll(block.getInstructions(), insns);
		}
	}

	public static void removeAllWithoutUnbind(BlockNode block, List<InsnNode> insns) {
		removeAll(block.getInstructions(), insns);
	}

	public static void removeAllMarked(MethodNode mth) {
		InsnRemover insnRemover = new InsnRemover(mth);
		for (BlockNode blockNode : mth.getBasicBlocks()) {
			for (InsnNode insn : blockNode.getInstructions()) {
				if (insn.contains(AFlag.REMOVE)) {
					insnRemover.addWithoutUnbind(insn);
				}
			}
			insnRemover.setBlock(blockNode);
			insnRemover.perform();
		}
	}

	public static void remove(MethodNode mth, BlockNode block, int index) {
		List<InsnNode> instructions = block.getInstructions();
		unbindInsn(mth, instructions.get(index));
		instructions.remove(index);
	}
}
