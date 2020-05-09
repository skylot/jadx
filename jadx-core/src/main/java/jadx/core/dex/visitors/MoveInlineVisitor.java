package jadx.core.dex.visitors;

import java.util.ArrayList;
import java.util.List;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.RegDebugInfoAttr;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.shrink.CodeShrinkVisitor;
import jadx.core.dex.visitors.ssa.SSATransform;
import jadx.core.utils.InsnRemover;

@JadxVisitor(
		name = "MoveInlineVisitor",
		desc = "Inline redundant move instructions",
		runAfter = SSATransform.class,
		runBefore = CodeShrinkVisitor.class
)
public class MoveInlineVisitor extends AbstractVisitor {
	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}
		moveInline(mth);
	}

	private static void moveInline(MethodNode mth) {
		InsnRemover remover = new InsnRemover(mth);
		for (BlockNode block : mth.getBasicBlocks()) {
			remover.setBlock(block);
			List<InsnNode> insns = block.getInstructions();
			int size = insns.size();
			for (int i = 0; i < size; i++) {
				InsnNode insn = insns.get(i);
				if (insn.getType() == InsnType.MOVE
						&& processMove(mth, block, insn, i)) {
					remover.addAndUnbind(insn);
				}
			}
			remover.perform();
		}
	}

	private static boolean processMove(MethodNode mth, BlockNode block, InsnNode move, int i) {
		RegisterArg resultArg = move.getResult();
		InsnArg moveArg = move.getArg(0);
		if (resultArg.sameRegAndSVar(moveArg)) {
			return true;
		}
		SSAVar ssaVar = resultArg.getSVar();
		if (ssaVar.isUsedInPhi()) {
			return false;
		}
		RegDebugInfoAttr debugInfo = resultArg.get(AType.REG_DEBUG_INFO);
		for (RegisterArg useArg : ssaVar.getUseList()) {
			InsnNode useInsn = useArg.getParentInsn();
			if (useInsn == null || !fromThisBlock(block, useInsn, i)) {
				return false;
			}
			RegDebugInfoAttr debugInfoAttr = useArg.get(AType.REG_DEBUG_INFO);
			if (debugInfoAttr != null) {
				debugInfo = debugInfoAttr;
			}
		}

		// all checks passed, execute inline
		for (RegisterArg useArg : new ArrayList<>(ssaVar.getUseList())) {
			InsnNode useInsn = useArg.getParentInsn();
			InsnArg replaceArg;
			if (moveArg.isRegister()) {
				replaceArg = ((RegisterArg) moveArg).duplicate(useArg.getInitType());
			} else {
				replaceArg = moveArg.duplicate();
			}
			replaceArg.copyAttributesFrom(useArg);
			if (debugInfo != null) {
				replaceArg.addAttr(debugInfo);
			}
			if (useInsn == null || !useInsn.replaceArg(useArg, replaceArg)) {
				mth.addWarnComment("Failed to replace arg in insn: " + useInsn);
			}
		}
		return true;
	}

	private static boolean fromThisBlock(BlockNode block, InsnNode insn, int curPos) {
		List<InsnNode> list = block.getInstructions();
		int size = list.size();
		for (int j = curPos; j < size; j++) {
			if (list.get(j) == insn) {
				return true;
			}
		}
		return false;
	}
}
