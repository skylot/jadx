package jadx.core.dex.visitors.regions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.PhiInsn;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
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
public class TernaryMod extends AbstractRegionVisitor implements IRegionIterativeVisitor {

	private static final TernaryMod INSTANCE = new TernaryMod();

	public static void process(MethodNode mth) {
		// first: convert all found ternary nodes in one iteration
		DepthRegionTraversal.traverse(mth, INSTANCE);
		if (mth.contains(AFlag.REQUEST_CODE_SHRINK)) {
			CodeShrinkVisitor.shrinkMethod(mth);
		}
		// second: iterative runs with shrink after each change
		DepthRegionTraversal.traverseIterative(mth, INSTANCE);
	}

	@Override
	public boolean enterRegion(MethodNode mth, IRegion region) {
		if (processRegion(mth, region)) {
			mth.add(AFlag.REQUEST_CODE_SHRINK);
		}
		return true;
	}

	@Override
	public boolean visitRegion(MethodNode mth, IRegion region) {
		if (processRegion(mth, region)) {
			CodeShrinkVisitor.shrinkMethod(mth);
			return true;
		}
		return false;
	}

	private static boolean processRegion(MethodNode mth, IRegion region) {
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
			return processOneBranchTernary(mth, ifRegion);
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

		if (!verifyLineHints(mth, thenInsn, elseInsn)) {
			return false;
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
			int branchLine = Math.max(thenInsn.getSourceLine(), elseInsn.getSourceLine());
			ternInsn.setSourceLine(Math.max(ifRegion.getSourceLine(), branchLine));

			InsnRemover.unbindResult(mth, elseInsn);

			// remove 'if' instruction
			header.getInstructions().clear();
			ternInsn.rebindArgs();
			header.getInstructions().add(ternInsn);

			clearConditionBlocks(conditionBlocks, header);
			return true;
		}

		if (!mth.isVoidReturn()
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
			InsnNode retInsn = new InsnNode(InsnType.RETURN, 1);
			InsnArg arg = InsnArg.wrapInsnIntoArg(ternInsn);
			arg.setType(thenArg.getType());
			retInsn.addArg(arg);

			header.getInstructions().clear();
			retInsn.rebindArgs();
			header.getInstructions().add(retInsn);
			header.add(AFlag.RETURN);

			clearConditionBlocks(conditionBlocks, header);
			return true;
		}
		return false;
	}

	private static boolean verifyLineHints(MethodNode mth, InsnNode thenInsn, InsnNode elseInsn) {
		if (mth.contains(AFlag.USE_LINES_HINTS)
				&& thenInsn.getSourceLine() != elseInsn.getSourceLine()) {
			if (thenInsn.getSourceLine() != 0 && elseInsn.getSourceLine() != 0) {
				// sometimes source lines incorrect
				return checkLineStats(thenInsn, elseInsn);
			}
			// don't make nested ternary by default
			// TODO: add addition checks
			return !containsTernary(thenInsn) && !containsTernary(elseInsn);
		}
		return true;
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

	@SuppressWarnings("StatementWithEmptyBody")
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
			if (!resArg.sameRegAndSVar(arg)) {
				otherArg = (RegisterArg) arg;
				break;
			}
		}
		if (otherArg == null) {
			return;
		}
		InsnNode elseAssign = otherArg.getAssignInsn();
		if (mth.isConstructor() || (mth.getParentClass().isEnum() && mth.getMethodInfo().isClassInit())) {
			// forcing ternary inline for constructors (will help in moving super call to the top) and enums
			// skip code style checks
		} else {
			if (elseAssign != null && elseAssign.isConstInsn()) {
				if (!verifyLineHints(mth, insn, elseAssign)) {
					return;
				}
			} else {
				if (insn.getResult().sameCodeVar(otherArg)) {
					// don't use same variable in else branch to prevent: l = (l == 0) ? 1 : l
					return;
				}
			}
		}

		// all checks passed
		BlockNode header = ifRegion.getConditionBlocks().get(0);
		if (!ifRegion.getParent().replaceSubBlock(ifRegion, header)) {
			return;
		}
		InsnArg elseArg;
		if (elseAssign != null && elseAssign.isConstInsn()) {
			// inline constant
			SSAVar elseVar = elseAssign.getResult().getSVar();
			if (elseVar.getUseCount() == 1 && elseVar.getOnlyOneUseInPhi() == phiInsn) {
				InsnRemover.remove(mth, elseAssign);
			}
			elseArg = InsnArg.wrapInsnIntoArg(elseAssign);
		} else {
			elseArg = otherArg.duplicate();
		}
		InsnArg thenArg = InsnArg.wrapInsnIntoArg(insn);
		RegisterArg resultArg = phiInsn.getResult().duplicate();
		TernaryInsn ternInsn = new TernaryInsn(ifRegion.getCondition(), resultArg, thenArg, elseArg);
		ternInsn.simplifyCondition();

		InsnRemover.unbindAllArgs(mth, phiInsn);
		InsnRemover.unbindResult(mth, insn);
		InsnList.remove(block, insn);
		header.getInstructions().clear();
		ternInsn.rebindArgs();
		header.getInstructions().add(ternInsn);

		clearConditionBlocks(ifRegion.getConditionBlocks(), header);

		// shrink method again
		CodeShrinkVisitor.shrinkMethod(mth);
	}

	private TernaryMod() {
	}
}
