package jadx.core.dex.visitors.regions;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.instructions.PhiInsn;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.conditions.IfCondition;
import jadx.core.dex.regions.loops.IndexLoop;
import jadx.core.dex.regions.loops.LoopRegion;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.RegionUtils;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoopRegionVisitor extends AbstractVisitor implements IRegionVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(LoopRegionVisitor.class);

	@Override
	public void visit(MethodNode mth) {
		DepthRegionTraversal.traverseAll(mth, this);
	}

	@Override
	public void enterRegion(MethodNode mth, IRegion region) {
		if (region instanceof LoopRegion) {
			processLoopRegion(mth, (LoopRegion) region);
		}
	}

	private static void processLoopRegion(MethodNode mth, LoopRegion loopRegion) {
		if (loopRegion.isConditionAtEnd()) {
			return;
		}
		IfCondition condition = loopRegion.getCondition();
		if (condition == null) {
			return;
		}
		List<RegisterArg> args = condition.getRegisterArgs();
		if (checkForIndexedLoop(mth, loopRegion, args)) {
			return;
		}
	}

	/**
	 * Check for indexed loop.
	 */
	private static boolean checkForIndexedLoop(MethodNode mth, LoopRegion loopRegion, List<RegisterArg> condArgs) {
		InsnNode incrInsn = RegionUtils.getLastInsn(loopRegion);
		if (incrInsn == null) {
			return false;
		}
		RegisterArg incrArg = incrInsn.getResult();
		if (incrArg == null
				|| incrArg.getSVar() == null
				|| !incrArg.getSVar().isUsedInPhi()) {
			return false;
		}
		PhiInsn phiInsn = incrArg.getSVar().getUsedInPhi();
		if (phiInsn.getArgsCount() != 2
				|| !phiInsn.getArg(1).equals(incrArg)
				|| incrArg.getSVar().getUseCount() != 1) {
			return false;
		}
		RegisterArg arg = phiInsn.getResult();
		if (!condArgs.contains(arg) || arg.getSVar().isUsedInPhi()) {
			return false;
		}
		RegisterArg initArg = phiInsn.getArg(0);
		InsnNode initInsn = initArg.getAssignInsn();
		if (initInsn == null || initArg.getSVar().getUseCount() != 1) {
			return false;
		}
		if (!usedOnlyInLoop(mth, loopRegion, arg)) {
			return false;
		}
		initInsn.add(AFlag.SKIP);
		incrInsn.add(AFlag.SKIP);
		loopRegion.setType(new IndexLoop(initInsn, incrInsn));
		return true;
	}

	private static boolean usedOnlyInLoop(MethodNode mth, LoopRegion loopRegion, RegisterArg arg) {
		List<RegisterArg> useList = arg.getSVar().getUseList();
		for (RegisterArg useArg : useList) {
			if (!argInLoop(mth, loopRegion, useArg)) {
				return false;
			}
		}
		return true;
	}

	private static boolean argInLoop(MethodNode mth, LoopRegion loopRegion, RegisterArg arg) {
		InsnNode parentInsn = arg.getParentInsn();
		if (parentInsn == null) {
			return false;
		}
		BlockNode block = BlockUtils.getBlockByInsn(mth, parentInsn);
		if (block == null) {
			LOG.debug("Instruction not found: {}, mth: {}", parentInsn, mth);
			return false;
		}
		return RegionUtils.isRegionContainsBlock(loopRegion, block);
	}

	@Override
	public void leaveRegion(MethodNode mth, IRegion region) {
	}

	@Override
	public void processBlock(MethodNode mth, IBlock container) {
	}
}
