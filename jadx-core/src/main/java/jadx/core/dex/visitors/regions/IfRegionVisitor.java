package jadx.core.dex.visitors.regions;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.IfCondition;
import jadx.core.dex.regions.IfCondition.Mode;
import jadx.core.dex.regions.IfRegion;
import jadx.core.dex.regions.Region;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.RegionUtils;

import java.util.List;

import static jadx.core.utils.RegionUtils.insnsCount;

public class IfRegionVisitor extends AbstractVisitor implements IRegionVisitor, IRegionIterativeVisitor {

	@Override
	public void visit(MethodNode mth) {
		DepthRegionTraversal.traverseAll(mth, this);
		DepthRegionTraversal.traverseAllIterative(mth, this);
	}

	@Override
	public void enterRegion(MethodNode mth, IRegion region) {
		if (region instanceof IfRegion) {
			processIfRegion(mth, (IfRegion) region);
		}
	}

	@Override
	public boolean visitRegion(MethodNode mth, IRegion region) {
		if (region instanceof IfRegion) {
			return removeRedundantElseBlock((IfRegion) region);
		}
		return false;
	}

	@Override
	public void processBlock(MethodNode mth, IBlock container) {
	}

	@Override
	public void leaveRegion(MethodNode mth, IRegion region) {
	}

	private static void processIfRegion(MethodNode mth, IfRegion ifRegion) {
		simplifyIfCondition(ifRegion);
		moveReturnToThenBlock(mth, ifRegion);
		moveBreakToThenBlock(ifRegion);
		markElseIfChains(ifRegion);

		TernaryMod.makeTernaryInsn(mth, ifRegion);
	}

	private static void simplifyIfCondition(IfRegion ifRegion) {
		if (ifRegion.simplifyCondition()) {
			IfCondition condition = ifRegion.getCondition();
			if (condition.getMode() == Mode.NOT) {
				invertIfRegion(ifRegion);
			}
		}
		IContainer elseRegion = ifRegion.getElseRegion();
		if (elseRegion == null || RegionUtils.isEmpty(elseRegion)) {
			return;
		}
		boolean thenIsEmpty = RegionUtils.isEmpty(ifRegion.getThenRegion());
		if (thenIsEmpty || hasSimpleReturnBlock(ifRegion.getThenRegion())) {
			invertIfRegion(ifRegion);
		}

		if (!thenIsEmpty) {
			// move 'if' from then to make 'else if' chain
			if (isIfRegion(ifRegion.getThenRegion())
					&& !isIfRegion(elseRegion)) {
				invertIfRegion(ifRegion);
			}

		}
	}

	private static boolean isIfRegion(IContainer container) {
		if (container instanceof IfRegion) {
			return true;
		}
		if (container instanceof IRegion) {
			List<IContainer> subBlocks = ((IRegion) container).getSubBlocks();
			if (subBlocks.size() == 1 && subBlocks.get(0) instanceof IfRegion) {
				return true;
			}
		}
		return false;
	}

	private static void moveReturnToThenBlock(MethodNode mth, IfRegion ifRegion) {
		if (!mth.getReturnType().equals(ArgType.VOID)
				&& hasSimpleReturnBlock(ifRegion.getElseRegion())
				/*&& insnsCount(ifRegion.getThenRegion()) < 2*/) {
			invertIfRegion(ifRegion);
		}
	}

	private static void moveBreakToThenBlock(IfRegion ifRegion) {
		if (ifRegion.getElseRegion() != null
				&& RegionUtils.hasBreakInsn(ifRegion.getElseRegion())) {
			invertIfRegion(ifRegion);
		}
	}

	/**
	 * Mark if-else-if chains
	 */
	private static void markElseIfChains(IfRegion ifRegion) {
		if (hasSimpleReturnBlock(ifRegion.getThenRegion())) {
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

	private static boolean removeRedundantElseBlock(IfRegion ifRegion) {
		if (ifRegion.getElseRegion() != null
				&& !ifRegion.contains(AFlag.ELSE_IF_CHAIN)
				&& !ifRegion.getElseRegion().contains(AFlag.ELSE_IF_CHAIN)
				&& hasBranchTerminator(ifRegion)
				&& insnsCount(ifRegion.getThenRegion()) < 2) {
			IRegion parent = ifRegion.getParent();
			Region newRegion = new Region(parent);
			if (parent.replaceSubBlock(ifRegion, newRegion)) {
				newRegion.add(ifRegion);
				newRegion.add(ifRegion.getElseRegion());
				ifRegion.setElseRegion(null);
				return true;
			}
		}
		return false;
	}

	private static boolean hasBranchTerminator(IfRegion ifRegion) {
		// TODO: check for exception throw
		return RegionUtils.hasExitBlock(ifRegion.getThenRegion())
				|| RegionUtils.hasBreakInsn(ifRegion.getThenRegion());
	}

	private static void invertIfRegion(IfRegion ifRegion) {
		IContainer elseRegion = ifRegion.getElseRegion();
		if (elseRegion != null) {
			ifRegion.invert();
		}
	}

	private static boolean hasSimpleReturnBlock(IContainer region) {
		if (region == null) {
			return false;
		}
		if (region.contains(AFlag.RETURN)) {
			return true;
		}
		if (region instanceof IRegion) {
			List<IContainer> subBlocks = ((IRegion) region).getSubBlocks();
			return subBlocks.size() == 1 && subBlocks.get(0).contains(AFlag.RETURN);
		}
		return false;
	}
}
