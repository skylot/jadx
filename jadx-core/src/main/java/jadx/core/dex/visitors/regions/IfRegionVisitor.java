package jadx.core.dex.visitors.regions;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.IfCondition;
import jadx.core.dex.regions.IfRegion;
import jadx.core.dex.regions.Region;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.RegionUtils;

import java.util.List;

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
		markElseIfChains(ifRegion);

		TernaryMod.makeTernaryInsn(mth, ifRegion);
	}

	private static void simplifyIfCondition(IfRegion ifRegion) {
		if (ifRegion.simplifyCondition()) {
			IfCondition condition = ifRegion.getCondition();
			if (condition.getMode() == IfCondition.Mode.NOT) {
				invertIfRegion(ifRegion);
			}
		}
		if (RegionUtils.isEmpty(ifRegion.getThenRegion())
				|| hasSimpleReturnBlock(ifRegion.getThenRegion())) {
			invertIfRegion(ifRegion);
		}
	}

	private static void moveReturnToThenBlock(MethodNode mth, IfRegion ifRegion) {
		if (!mth.getReturnType().equals(ArgType.VOID)
				&& hasSimpleReturnBlock(ifRegion.getElseRegion())
				/*&& insnsCount(ifRegion.getThenRegion()) < 2*/) {
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
				&& RegionUtils.hasExitBlock(ifRegion.getThenRegion())) {
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
