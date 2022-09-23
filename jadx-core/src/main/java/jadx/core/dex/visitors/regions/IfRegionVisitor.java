package jadx.core.dex.visitors.regions;

import java.util.List;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.Region;
import jadx.core.dex.regions.conditions.IfCondition;
import jadx.core.dex.regions.conditions.IfCondition.Mode;
import jadx.core.dex.regions.conditions.IfRegion;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.RegionUtils;

import static jadx.core.utils.RegionUtils.insnsCount;

public class IfRegionVisitor extends AbstractVisitor {
	private static final ProcessIfRegionVisitor PROCESS_IF_REGION_VISITOR = new ProcessIfRegionVisitor();
	private static final RemoveRedundantElseVisitor REMOVE_REDUNDANT_ELSE_VISITOR = new RemoveRedundantElseVisitor();

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}
		process(mth);
	}

	public static void process(MethodNode mth) {
		TernaryMod.process(mth);
		DepthRegionTraversal.traverse(mth, PROCESS_IF_REGION_VISITOR);
		DepthRegionTraversal.traverseIterative(mth, REMOVE_REDUNDANT_ELSE_VISITOR);
	}

	private static class ProcessIfRegionVisitor extends AbstractRegionVisitor {
		@Override
		public boolean enterRegion(MethodNode mth, IRegion region) {
			if (region instanceof IfRegion) {
				IfRegion ifRegion = (IfRegion) region;
				orderBranches(mth, ifRegion);
				markElseIfChains(mth, ifRegion);
			}
			return true;
		}
	}

	@SuppressWarnings({ "UnnecessaryReturnStatement", "StatementWithEmptyBody" })
	private static void orderBranches(MethodNode mth, IfRegion ifRegion) {
		if (RegionUtils.isEmpty(ifRegion.getElseRegion())) {
			return;
		}
		if (RegionUtils.isEmpty(ifRegion.getThenRegion())) {
			invertIfRegion(ifRegion);
			return;
		}
		if (mth.contains(AFlag.USE_LINES_HINTS)) {
			int thenLine = RegionUtils.getFirstSourceLine(ifRegion.getThenRegion());
			int elseLine = RegionUtils.getFirstSourceLine(ifRegion.getElseRegion());
			if (thenLine != 0 && elseLine != 0) {
				if (thenLine > elseLine) {
					invertIfRegion(ifRegion);
				}
				return;
			}
		}
		if (ifRegion.simplifyCondition()) {
			IfCondition condition = ifRegion.getCondition();
			if (condition != null && condition.getMode() == Mode.NOT) {
				invertIfRegion(ifRegion);
			}
		}
		int thenSize = insnsCount(ifRegion.getThenRegion());
		int elseSize = insnsCount(ifRegion.getElseRegion());
		if (isSimpleExitBlock(mth, ifRegion.getElseRegion())) {
			if (isSimpleExitBlock(mth, ifRegion.getThenRegion())) {
				if (elseSize < thenSize) {
					invertIfRegion(ifRegion);
					return;
				}
			}
			boolean lastRegion = RegionUtils.hasExitEdge(ifRegion);
			if (elseSize == 1 && lastRegion && mth.isVoidReturn()) {
				InsnNode lastElseInsn = RegionUtils.getLastInsn(ifRegion.getElseRegion());
				if (lastElseInsn != null && lastElseInsn.getType() == InsnType.THROW) {
					// move `throw` into `then` block
					invertIfRegion(ifRegion);
				} else {
					// single return at method end will be removed later
				}
				return;
			}
			if (!lastRegion) {
				invertIfRegion(ifRegion);
			}
			return;
		}
		boolean thenExit = RegionUtils.hasExitBlock(ifRegion.getThenRegion());
		boolean elseExit = RegionUtils.hasExitBlock(ifRegion.getElseRegion());
		if (elseExit && (!thenExit || elseSize < thenSize)) {
			invertIfRegion(ifRegion);
			return;
		}
		// move 'if' from 'then' branch to make 'else if' chain
		if (isIfRegion(ifRegion.getThenRegion())
				&& !isIfRegion(ifRegion.getElseRegion())
				&& !thenExit) {
			invertIfRegion(ifRegion);
			return;
		}
		// move 'break' into 'then' branch
		if (RegionUtils.hasBreakInsn(ifRegion.getElseRegion())) {
			invertIfRegion(ifRegion);
			return;
		}
	}

	private static boolean isIfRegion(IContainer container) {
		if (container instanceof IfRegion) {
			return true;
		}
		if (container instanceof IRegion) {
			List<IContainer> subBlocks = ((IRegion) container).getSubBlocks();
			return subBlocks.size() == 1 && subBlocks.get(0) instanceof IfRegion;
		}
		return false;
	}

	/**
	 * Mark if-else-if chains
	 */
	private static void markElseIfChains(MethodNode mth, IfRegion ifRegion) {
		if (isSimpleExitBlock(mth, ifRegion.getThenRegion())) {
			return;
		}
		IContainer elsRegion = ifRegion.getElseRegion();
		if (elsRegion instanceof Region) {
			List<IContainer> subBlocks = ((Region) elsRegion).getSubBlocks();
			if (subBlocks.size() == 1 && subBlocks.get(0) instanceof IfRegion) {
				subBlocks.get(0).add(AFlag.ELSE_IF_CHAIN);
				elsRegion.add(AFlag.ELSE_IF_CHAIN);
			}
		}
	}

	private static class RemoveRedundantElseVisitor implements IRegionIterativeVisitor {
		@Override
		public boolean visitRegion(MethodNode mth, IRegion region) {
			if (region instanceof IfRegion) {
				return removeRedundantElseBlock(mth, (IfRegion) region);
			}
			return false;
		}
	}

	private static boolean removeRedundantElseBlock(MethodNode mth, IfRegion ifRegion) {
		if (ifRegion.getElseRegion() == null
				|| ifRegion.contains(AFlag.ELSE_IF_CHAIN)
				|| ifRegion.getElseRegion().contains(AFlag.ELSE_IF_CHAIN)) {
			return false;
		}
		if (!RegionUtils.hasExitBlock(ifRegion.getThenRegion())) {
			return false;
		}
		// code style check:
		// will remove 'return;' from 'then' and 'else' with one instruction
		// see #jadx.tests.integration.conditions.TestConditions9
		if (mth.isVoidReturn()
				&& insnsCount(ifRegion.getThenRegion()) == 2
				&& insnsCount(ifRegion.getElseRegion()) == 2) {
			return false;
		}
		IRegion parent = ifRegion.getParent();
		Region newRegion = new Region(parent);
		if (parent.replaceSubBlock(ifRegion, newRegion)) {
			newRegion.add(ifRegion);
			newRegion.add(ifRegion.getElseRegion());
			ifRegion.setElseRegion(null);
			return true;
		}
		return false;
	}

	private static void invertIfRegion(IfRegion ifRegion) {
		IContainer elseRegion = ifRegion.getElseRegion();
		if (elseRegion != null) {
			ifRegion.invert();
		}
	}

	private static boolean isSimpleExitBlock(MethodNode mth, IContainer container) {
		if (container == null) {
			return false;
		}
		if (container.contains(AFlag.RETURN) || RegionUtils.isExitBlock(mth, container)) {
			return true;
		}
		if (container instanceof IRegion) {
			List<IContainer> subBlocks = ((IRegion) container).getSubBlocks();
			return subBlocks.size() == 1 && RegionUtils.isExitBlock(mth, subBlocks.get(0));
		}
		return false;
	}
}
