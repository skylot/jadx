package jadx.core.dex.visitors.regions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.PhiInsn;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.mods.TernaryInsn;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.Region;
import jadx.core.dex.regions.conditions.IfRegion;
import jadx.core.dex.visitors.shrink.CodeShrinkVisitor;
import jadx.core.utils.InsnList;
import jadx.core.utils.InsnRemover;

/**
 * Convert 'if' to ternary operation
 */
public class TernaryMod implements IRegionIterativeVisitor {

	@Override
	public boolean visitRegion(MethodNode mth, IRegion region) {
		if (region instanceof IfRegion) {
			return makeTernaryInsn(mth, (IfRegion) region);
		}
		return false;
	}

	private static boolean makeTernaryInsn(MethodNode mth, IfRegion ifRegion) {
		if (ifRegion.contains(AFlag.ELSE_IF_CHAIN)) {
			return false;
		}
		IContainer thenRegion = ifRegion.getThenRegion();
		IContainer elseRegion = ifRegion.getElseRegion();
		if (thenRegion == null) {
			return false;
		}
		if (elseRegion == null) {
			if (mth.isConstructor()) {
				// force ternary conversion to inline all code in 'super' or 'this' calls
				return processOneBranchTernary(mth, ifRegion);
			}
			return false;
		}
		BlockNode tb = getTernaryInsnBlock(thenRegion);
		BlockNode eb = getTernaryInsnBlock(elseRegion);
		if (tb == null || eb == null) {
			return false;
		}
		List<BlockNode> conditionBlocks = ifRegion.getConditionBlocks();
		if (conditionBlocks.isEmpty()) {
			return false;
		}

		BlockNode header = conditionBlocks.get(0);
		InsnNode thenInsn = tb.getInstructions().get(0);
		InsnNode elseInsn = eb.getInstructions().get(0);

		if (thenInsn.getSourceLine() != elseInsn.getSourceLine()) {
			if (thenInsn.getSourceLine() != 0 && elseInsn.getSourceLine() != 0) {
				// sometimes source lines incorrect
				if (!checkLineStats(thenInsn, elseInsn)) {
					return false;
				}
			} else {
				// no debug info
				if (containsTernary(thenInsn) || containsTernary(elseInsn)) {
					// don't make nested ternary by default
					// TODO: add addition checks
					return false;
				}
			}
		}

		RegisterArg thenResArg = thenInsn.getResult();
		RegisterArg elseResArg = elseInsn.getResult();
		if (thenResArg != null && elseResArg != null) {
			PhiInsn thenPhi = thenResArg.getSVar().getOnlyOneUseInPhi();
			PhiInsn elsePhi = elseResArg.getSVar().getOnlyOneUseInPhi();
			if (thenPhi == null || thenPhi != elsePhi) {
				return false;
			}
			if (!ifRegion.getParent().replaceSubBlock(ifRegion, header)) {
				return false;
			}
			InsnList.remove(tb, thenInsn);
			InsnList.remove(eb, elseInsn);

			RegisterArg resArg;
			if (thenPhi.getArgsCount() == 2) {
				resArg = thenPhi.getResult();
				InsnRemover.unbindResult(mth, thenInsn);
			} else {
				resArg = thenResArg;
				thenPhi.removeArg(elseResArg);
			}
			InsnArg thenArg = InsnArg.wrapInsnIntoArg(thenInsn);
			InsnArg elseArg = InsnArg.wrapInsnIntoArg(elseInsn);
			TernaryInsn ternInsn = new TernaryInsn(ifRegion.getCondition(), resArg, thenArg, elseArg);
			ternInsn.setSourceLine(thenInsn.getSourceLine());

			InsnRemover.unbindResult(mth, elseInsn);

			// remove 'if' instruction
			header.getInstructions().clear();
			ternInsn.rebindArgs();
			header.getInstructions().add(ternInsn);

			clearConditionBlocks(conditionBlocks, header);

			// shrink method again
			CodeShrinkVisitor.shrinkMethod(mth);
			return true;
		}

		if (!mth.getReturnType().equals(ArgType.VOID)
				&& thenInsn.getType() == InsnType.RETURN
				&& elseInsn.getType() == InsnType.RETURN) {
			InsnArg thenArg = thenInsn.getArg(0);
			InsnArg elseArg = elseInsn.getArg(0);
			if (thenArg.isLiteral() != elseArg.isLiteral()) {
				// one arg is literal
				return false;
			}

			if (!ifRegion.getParent().replaceSubBlock(ifRegion, header)) {
				return false;
			}
			InsnList.remove(tb, thenInsn);
			InsnList.remove(eb, elseInsn);
			tb.remove(AFlag.RETURN);
			eb.remove(AFlag.RETURN);

			TernaryInsn ternInsn = new TernaryInsn(ifRegion.getCondition(), null, thenArg, elseArg);
			ternInsn.setSourceLine(thenInsn.getSourceLine());
			InsnNode retInsn = new InsnNode(InsnType.RETURN, 1);
			InsnArg arg = InsnArg.wrapInsnIntoArg(ternInsn);
			arg.setType(thenArg.getType());
			retInsn.addArg(arg);

			header.getInstructions().clear();
			retInsn.rebindArgs();
			header.getInstructions().add(retInsn);
			header.add(AFlag.RETURN);

			clearConditionBlocks(conditionBlocks, header);

			CodeShrinkVisitor.shrinkMethod(mth);
			return true;
		}
		return false;
	}

	private static void clearConditionBlocks(List<BlockNode> conditionBlocks, BlockNode header) {
		for (BlockNode block : conditionBlocks) {
			if (block != header) {
				block.getInstructions().clear();
				block.add(AFlag.REMOVE);
			}
		}
	}

	private static BlockNode getTernaryInsnBlock(IContainer thenRegion) {
		if (thenRegion instanceof Region) {
			Region r = (Region) thenRegion;
			if (r.getSubBlocks().size() == 1) {
				IContainer container = r.getSubBlocks().get(0);
				if (container instanceof BlockNode) {
					BlockNode block = (BlockNode) container;
					if (block.getInstructions().size() == 1) {
						return block;
					}
				}
			}
		}
		return null;
	}

	private static boolean containsTernary(InsnNode insn) {
		if (insn.getType() == InsnType.TERNARY) {
			return true;
		}
		for (int i = 0; i < insn.getArgsCount(); i++) {
			InsnArg arg = insn.getArg(i);
			if (arg.isInsnWrap()) {
				InsnNode wrapInsn = ((InsnWrapArg) arg).getWrapInsn();
				if (containsTernary(wrapInsn)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Return 'true' if there are several args with same source lines
	 */
	private static boolean checkLineStats(InsnNode t, InsnNode e) {
		if (t.getResult() == null || e.getResult() == null) {
			return false;
		}
		PhiInsn tPhi = t.getResult().getSVar().getOnlyOneUseInPhi();
		PhiInsn ePhi = e.getResult().getSVar().getOnlyOneUseInPhi();
		if (ePhi == null || tPhi != ePhi) {
			return false;
		}
		Map<Integer, Integer> map = new HashMap<>(tPhi.getArgsCount());
		for (InsnArg arg : tPhi.getArguments()) {
			if (!arg.isRegister()) {
				continue;
			}
			InsnNode assignInsn = ((RegisterArg) arg).getAssignInsn();
			if (assignInsn == null) {
				continue;
			}
			int sourceLine = assignInsn.getSourceLine();
			if (sourceLine != 0) {
				map.merge(sourceLine, 1, Integer::sum);
			}
		}
		for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
			if (entry.getValue() >= 2) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Convert one variable change with only 'then' branch:
	 * 'if (c) {r = a;}' to 'r = c ? a : r'
	 * Convert if 'r' used only once
	 */
	private static boolean processOneBranchTernary(MethodNode mth, IfRegion ifRegion) {
		IContainer thenRegion = ifRegion.getThenRegion();
		BlockNode block = getTernaryInsnBlock(thenRegion);
		if (block != null) {
			InsnNode insn = block.getInstructions().get(0);
			RegisterArg result = insn.getResult();
			if (result != null) {
				replaceWithTernary(mth, ifRegion, block, insn);
			}
		}
		return false;
	}

	private static void replaceWithTernary(MethodNode mth, IfRegion ifRegion, BlockNode block, InsnNode insn) {
		RegisterArg resArg = insn.getResult();
		if (resArg.getSVar().getUseList().size() != 1) {
			return;
		}
		PhiInsn phiInsn = resArg.getSVar().getOnlyOneUseInPhi();
		if (phiInsn == null || phiInsn.getArgsCount() != 2) {
			return;
		}
		RegisterArg otherArg = null;
		for (InsnArg arg : phiInsn.getArguments()) {
			if (arg != resArg && arg instanceof RegisterArg) {
				otherArg = (RegisterArg) arg;
				break;
			}
		}
		if (otherArg == null) {
			return;
		}

		// all checks passed
		BlockNode header = ifRegion.getConditionBlocks().get(0);
		if (!ifRegion.getParent().replaceSubBlock(ifRegion, header)) {
			return;
		}
		InsnList.remove(block, insn);
		TernaryInsn ternInsn = new TernaryInsn(ifRegion.getCondition(),
				phiInsn.getResult(), InsnArg.wrapInsnIntoArg(insn), otherArg);
		ternInsn.setSourceLine(insn.getSourceLine());

		InsnRemover.unbindAllArgs(mth, phiInsn);
		header.getInstructions().clear();
		ternInsn.rebindArgs();
		header.getInstructions().add(ternInsn);

		clearConditionBlocks(ifRegion.getConditionBlocks(), header);

		// shrink method again
		CodeShrinkVisitor.shrinkMethod(mth);
	}
}
