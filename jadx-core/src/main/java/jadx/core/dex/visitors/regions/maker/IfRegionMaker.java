package jadx.core.dex.visitors.regions.maker;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.Consts;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.EdgeInsnAttr;
import jadx.core.dex.attributes.nodes.LoopInfo;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnContainer;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.Region;
import jadx.core.dex.regions.conditions.IfCondition;
import jadx.core.dex.regions.conditions.IfInfo;
import jadx.core.dex.regions.conditions.IfRegion;
import jadx.core.dex.regions.loops.LoopRegion;
import jadx.core.dex.trycatch.ExcHandlerAttr;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.blocks.BlockSet;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static jadx.core.utils.BlockUtils.bitSetToBlocks;
import static jadx.core.utils.BlockUtils.bitSetToOneBlock;
import static jadx.core.utils.BlockUtils.followEmptyPath;
import static jadx.core.utils.BlockUtils.getBottomBlock;
import static jadx.core.utils.BlockUtils.getPathCross;
import static jadx.core.utils.BlockUtils.isEqualPaths;
import static jadx.core.utils.BlockUtils.isEqualReturnBlocks;
import static jadx.core.utils.BlockUtils.isPathExists;
import static jadx.core.utils.BlockUtils.newBlocksBitSet;

final class IfRegionMaker {
	private static final Logger LOG = LoggerFactory.getLogger(IfRegionMaker.class);
	private final MethodNode mth;
	private final RegionMaker regionMaker;

	IfRegionMaker(MethodNode mth, RegionMaker regionMaker) {
		this.mth = mth;
		this.regionMaker = regionMaker;
	}

	BlockNode process(IRegion currentRegion, BlockNode block, IfNode ifnode, RegionStack stack) {

		if (block.contains(AFlag.ADDED_TO_REGION)) {
			// block already included in other 'if' region
			return ifnode.getThenBlock();
		}

		IfInfo currentIf = makeIfInfo(mth, block);
		if (currentIf == null) {
			return null;
		}
		IfInfo mergedIf = mergeNestedIfNodes(currentIf);
		if (mergedIf != null) {
			currentIf = mergedIf;
		} else {
			// invert simple condition (compiler often do it)
			// ensure that we only ever invert once, because if multiple regions contain this block
			// we'll change the block after it's already been included in a region, which can cause
			// other regions containing the block to believe the condition has been flipped when it
			// has not, or vice versa.
			if (!block.contains(AFlag.DONT_INVERT)) {
				currentIf = IfInfo.invert(currentIf);
				block.add(AFlag.DONT_INVERT);
			}
		}
		IfInfo modifiedIf = restructureIf(mth, block, currentIf);
		if (modifiedIf != null) {
			currentIf = modifiedIf;
		} else {
			if (currentIf.getMergedBlocks().size() <= 1) {
				return null;
			}
			currentIf = makeIfInfo(mth, block);
			currentIf = restructureIf(mth, block, currentIf);
			if (currentIf == null) {
				// all attempts failed
				return null;
			}
		}
		confirmMerge(currentIf);

		IfRegion ifRegion = new IfRegion(currentRegion);
		ifRegion.updateCondition(currentIf);
		currentRegion.getSubBlocks().add(ifRegion);

		BlockNode outBlock = currentIf.getOutBlock();
		stack.push(ifRegion);
		stack.addExit(outBlock);

		BlockNode thenBlock = currentIf.getThenBlock();
		if (thenBlock == null) {
			// empty then block, not normal, but maybe correct
			ifRegion.setThenRegion(new Region(ifRegion));
		} else {
			ifRegion.setThenRegion(regionMaker.makeRegion(thenBlock));
		}
		BlockNode elseBlock = currentIf.getElseBlock();
		if (elseBlock == null || stack.containsExit(elseBlock)) {
			ifRegion.setElseRegion(null);
		} else {
			ifRegion.setElseRegion(regionMaker.makeRegion(elseBlock));
		}

		// insert edge insns in new 'else' branch
		if (ifRegion.getElseRegion() == null && outBlock != null) {
			List<EdgeInsnAttr> edgeInsnAttrs = outBlock.getAll(AType.EDGE_INSN);
			if (!edgeInsnAttrs.isEmpty()) {
				List<InsnNode> instructions = new ArrayList<>();
				for (EdgeInsnAttr edgeInsnAttr : edgeInsnAttrs) {
					if (edgeInsnAttr.getEnd().equals(outBlock)) {
						if (currentIf.getMergedBlocks().contains(followEmptyPath(edgeInsnAttr.getStart(), true))) {
							instructions.add(edgeInsnAttr.getInsn());
						}
					}
				}

				if (!instructions.isEmpty()) {
					Region elseRegion = new Region(ifRegion);
					InsnContainer newBlock = new InsnContainer(instructions);
					elseRegion.add(newBlock);
					ifRegion.setElseRegion(elseRegion);
				}
			}
		}

		stack.pop();
		return outBlock;
	}

	@NotNull
	IfInfo buildIfInfo(LoopRegion loopRegion) {
		IfInfo condInfo = makeIfInfo(mth, loopRegion.getHeader());
		condInfo = searchNestedIf(condInfo);
		confirmMerge(condInfo);
		return condInfo;
	}

	@Nullable
	static IfInfo makeIfInfo(MethodNode mth, BlockNode ifBlock) {
		InsnNode lastInsn = BlockUtils.getLastInsn(ifBlock);
		if (lastInsn == null || lastInsn.getType() != InsnType.IF) {
			return null;
		}
		IfNode ifNode = (IfNode) lastInsn;
		IfCondition condition = IfCondition.fromIfNode(ifNode);
		IfInfo info = new IfInfo(mth, condition, ifNode.getThenBlock(), ifNode.getElseBlock());
		info.getMergedBlocks().add(ifBlock);
		return info;
	}

	static IfInfo searchNestedIf(IfInfo info) {
		IfInfo next = mergeNestedIfNodes(info);
		if (next != null) {
			return next;
		}
		return info;
	}

	static IfInfo restructureIf(MethodNode mth, BlockNode block, IfInfo info) {
		BlockNode thenBlock = info.getThenBlock();
		BlockNode elseBlock = info.getElseBlock();

		if (Objects.equals(thenBlock, elseBlock)) {
			IfInfo ifInfo = new IfInfo(info, null, null);
			ifInfo.setOutBlock(thenBlock);
			return ifInfo;
		}

		// select 'then', 'else' and 'exit' blocks
		if (thenBlock.contains(AFlag.RETURN) && elseBlock.contains(AFlag.RETURN)) {
			info.setOutBlock(null);
			return info;
		}
		// init outblock, which will be used in isBadBranchBlock to compare with branch block
		info.setOutBlock(findOutBlock(mth, thenBlock, elseBlock));

		boolean badThen = isBadBranchBlock(info, thenBlock);
		boolean badElse = isBadBranchBlock(info, elseBlock);
		if (badThen && badElse) {
			if (Consts.DEBUG_RESTRUCTURE) {
				LOG.debug("Stop processing blocks after 'if': {}, method: {}", info.getMergedBlocks(), mth);
			}
			return null;
		}
		if (badElse) {
			info = new IfInfo(info, thenBlock, null);
			info.setOutBlock(elseBlock);
		} else if (badThen) {
			info = IfInfo.invert(info);
			info = new IfInfo(info, elseBlock, null);
			info.setOutBlock(thenBlock);
		}

		// getPathCross may not find outBlock (e.g. one branch has return, outBlock definitely is
		// null), so should check further
		if (info.getOutBlock() == null) {
			BlockNode scopeOutBlockThen = findScopeOutBlock(mth, info.getThenBlock());
			BlockNode scopeOutBlockElse = findScopeOutBlock(mth, info.getElseBlock());
			if (scopeOutBlockThen == null && scopeOutBlockElse != null) {
				info.setOutBlock(scopeOutBlockElse);
			} else if (scopeOutBlockThen != null && scopeOutBlockElse == null) {
				info.setOutBlock(scopeOutBlockThen);
			} else if (scopeOutBlockThen != null && scopeOutBlockThen == scopeOutBlockElse) {
				info.setOutBlock(scopeOutBlockThen);
			}
		}

		if (BlockUtils.isBackEdge(block, info.getOutBlock())) {
			info.setOutBlock(null);
		}
		return info;
	}

	static BlockNode findOutBlock(MethodNode mth, BlockNode thenBlock, BlockNode elseBlock) {
		if (thenBlock == elseBlock) {
			return thenBlock;
		}
		if (thenBlock == null || elseBlock == null) {
			return null;
		}

		BitSet thenDomFrontier = newBlocksBitSet(mth);
		thenDomFrontier.or(thenBlock.getDomFrontier());
		thenDomFrontier.set(thenBlock.getPos());

		BitSet elseDomFrontier = newBlocksBitSet(mth);
		elseDomFrontier.or(elseBlock.getDomFrontier());
		elseDomFrontier.set(elseBlock.getPos());

		BitSet intersection = newBlocksBitSet(mth);
		intersection.or(thenDomFrontier);
		intersection.and(elseDomFrontier);

		intersection.clear(mth.getExitBlock().getPos());
		BlockNode oneBlock = bitSetToOneBlock(mth, intersection);

		// Attempt one: there's a unique block in the intersection of dom frontiers, and no path from
		// then->else or else->then
		if (oneBlock != null) {
			return oneBlock;
		}

		BitSet union = newBlocksBitSet(mth);
		union.or(thenBlock.getDomFrontier());
		union.or(elseBlock.getDomFrontier());
		union.clear(mth.getExitBlock().getPos());

		// Attempt two: look for a suitable block in the union.
		BitSet candidates = newBlocksBitSet(mth);
		for (BlockNode candidate : bitSetToBlocks(mth, union)) {
			if (isCandidateForOutBlock(mth, thenBlock, elseBlock, candidate)) {
				candidates.set(candidate.getPos());
			}
		}

		BlockNode bottom = getBottomBlock(bitSetToBlocks(mth, candidates), true);
		if (bottom != null) {
			return bottom;
		}

		// Attempt three: fallback to path cross again
		return getPathCross(mth, thenBlock, elseBlock);
	}

	static boolean isCandidateForOutBlock(MethodNode mth, BlockNode thenBlock, BlockNode elseBlock, BlockNode candidate) {
		// a candidate block requires:
		// - >1 predecessor
		// - each predecessor has a clean path from elseBlock or thenBlock, and there exist predecessors
		// covering both cases
		// - inside the union of the two dom frontiers

		if (candidate.getPredecessors().size() < 2) {
			return false; // block has only one pred, and so can't be the outblock
		}

		BitSet coverageThenPreds = newBlocksBitSet(mth);
		BitSet coverageElsePreds = newBlocksBitSet(mth);

		if (candidate == elseBlock) {
			coverageElsePreds.set(candidate.getPos());
		}
		if (candidate == thenBlock) {
			coverageThenPreds.set(candidate.getPos());
		}

		for (BlockNode pred : candidate.getPredecessors()) {
			if (isPathExists(thenBlock, pred)) {
				coverageThenPreds.set(pred.getPos());
			}

			if (isPathExists(elseBlock, pred)) {
				coverageElsePreds.set(pred.getPos());
			}
		}
		if (coverageElsePreds.cardinality() == 0 || coverageThenPreds.cardinality() == 0) {
			return false; // block has no path to both the then and else blocks
		}

		BlockNode coverageElsePred = bitSetToOneBlock(mth, coverageElsePreds);
		BlockNode coverageThenPred = bitSetToOneBlock(mth, coverageThenPreds);
		if (coverageElsePred != null && coverageElsePred == coverageThenPred) {
			return false; // the only paths from else and then go through the same block
		}

		return true;
	}

	private static boolean isBadBranchBlock(IfInfo info, BlockNode block) {
		// check if block at end of loop edge
		if (block.contains(AFlag.LOOP_START) && block.getPredecessors().size() == 1) {
			BlockNode pred = block.getPredecessors().get(0);
			if (pred.contains(AFlag.LOOP_END)) {
				List<LoopInfo> startLoops = block.getAll(AType.LOOP);
				List<LoopInfo> endLoops = pred.getAll(AType.LOOP);
				// search for same loop
				for (LoopInfo startLoop : startLoops) {
					for (LoopInfo endLoop : endLoops) {
						if (startLoop == endLoop) {
							return true;
						}
					}
				}
			}
		}
		// if branch block itself is outblock
		if (info.getOutBlock() != null) {
			return block == info.getOutBlock();
		}
		return !allPathsFromIf(block, info);
	}

	private static boolean allPathsFromIf(BlockNode block, IfInfo info) {
		List<BlockNode> preds = block.getPredecessors();
		BlockSet ifBlocks = info.getMergedBlocks();
		for (BlockNode pred : preds) {
			if (pred.contains(AFlag.LOOP_END)) {
				// ignore loop back edge
				continue;
			}
			BlockNode top = BlockUtils.skipSyntheticPredecessor(pred);
			if (!ifBlocks.contains(top)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * if startBlock is in a (try) scope, find the scope end as outBlock
	 */
	private @Nullable static BlockNode findScopeOutBlock(MethodNode mth, BlockNode startBlock) {
		if (startBlock == null) {
			return null;
		}
		List<BlockNode> domFrontiers = BlockUtils.bitSetToBlocks(mth, startBlock.getDomFrontier());
		BlockNode scopeOutBlock = null;

		// find handler from domFrontier(could be scope end), if domFrontier is handler
		// and its topSplitter dominates branch block, then branch should end
		for (BlockNode domFrontier : domFrontiers) {
			ExcHandlerAttr handler = domFrontier.get(AType.EXC_HANDLER);
			if (handler == null) {
				continue;
			}
			BlockNode topSplitter = handler.getTryBlock().getTopSplitter();
			if (startBlock.isDominator(topSplitter)) {
				scopeOutBlock = BlockUtils.getTryAndHandlerCrossBlock(mth, handler.getHandler());
				break;
			}
		}

		return scopeOutBlock;
	}

	static IfInfo mergeNestedIfNodes(IfInfo currentIf) {
		BlockNode curThen = currentIf.getThenBlock();
		BlockNode curElse = currentIf.getElseBlock();
		if (curThen == curElse) {
			return null;
		}
		if (BlockUtils.isFollowBackEdge(curThen)
				|| BlockUtils.isFollowBackEdge(curElse)) {
			return null;
		}
		boolean followThenBranch;
		IfInfo nextIf = getNextIf(currentIf, curThen);
		if (nextIf != null) {
			followThenBranch = true;
		} else {
			nextIf = getNextIf(currentIf, curElse);
			if (nextIf != null) {
				followThenBranch = false;
			} else {
				return null;
			}
		}

		boolean assignInlineNeeded = !nextIf.getForceInlineInsns().isEmpty();
		if (assignInlineNeeded) {
			for (BlockNode mergedBlock : currentIf.getMergedBlocks()) {
				if (mergedBlock.contains(AFlag.LOOP_START)) {
					// don't inline assigns into loop condition
					return currentIf;
				}
			}
		}

		if (isInversionNeeded(currentIf, nextIf)) {
			// invert current node for match pattern
			nextIf = IfInfo.invert(nextIf);
		}
		boolean thenPathSame = isEqualPaths(curThen, nextIf.getThenBlock());
		boolean elsePathSame = isEqualPaths(curElse, nextIf.getElseBlock());
		if (!thenPathSame && !elsePathSame) {
			// complex condition, run additional checks
			if (checkConditionBranches(curThen, curElse)
					|| checkConditionBranches(curElse, curThen)) {
				return null;
			}
			BlockNode otherBranchBlock = followThenBranch ? curElse : curThen;
			otherBranchBlock = BlockUtils.followEmptyPath(otherBranchBlock);
			if (!isPathExists(nextIf.getFirstIfBlock(), otherBranchBlock)) {
				return checkForTernaryInCondition(currentIf);
			}

			// this is nested conditions with different mode (i.e (a && b) || c),
			// search next condition for merge, get null if failed
			IfInfo tmpIf = mergeNestedIfNodes(nextIf);
			if (tmpIf != null) {
				nextIf = tmpIf;
				if (isInversionNeeded(currentIf, nextIf)) {
					nextIf = IfInfo.invert(nextIf);
				}
				if (!canMerge(currentIf, nextIf, followThenBranch)) {
					return currentIf;
				}
			} else {
				return currentIf;
			}
		} else {
			if (assignInlineNeeded) {
				boolean sameOuts = (thenPathSame && !followThenBranch) || (elsePathSame && followThenBranch);
				if (!sameOuts) {
					// don't inline assigns inside simple condition
					currentIf.resetForceInlineInsns();
					return currentIf;
				}
			}
		}

		IfInfo result = mergeIfInfo(currentIf, nextIf, followThenBranch);
		// search next nested if block
		return searchNestedIf(result);
	}

	private static IfInfo checkForTernaryInCondition(IfInfo currentIf) {
		IfInfo nextThen = getNextIf(currentIf, currentIf.getThenBlock());
		IfInfo nextElse = getNextIf(currentIf, currentIf.getElseBlock());
		if (nextThen == null || nextElse == null) {
			return null;
		}
		if (!nextThen.getFirstIfBlock().getDomFrontier().equals(nextElse.getFirstIfBlock().getDomFrontier())) {
			return null;
		}
		nextThen = searchNestedIf(nextThen);
		nextElse = searchNestedIf(nextElse);
		if (nextThen.getThenBlock() == nextElse.getThenBlock()
				&& nextThen.getElseBlock() == nextElse.getElseBlock()) {
			return mergeTernaryConditions(currentIf, nextThen, nextElse);
		}
		if (nextThen.getThenBlock() == nextElse.getElseBlock()
				&& nextThen.getElseBlock() == nextElse.getThenBlock()) {
			nextElse = IfInfo.invert(nextElse);
			return mergeTernaryConditions(currentIf, nextThen, nextElse);
		}
		return null;
	}

	private static IfInfo mergeTernaryConditions(IfInfo currentIf, IfInfo nextThen, IfInfo nextElse) {
		IfCondition newCondition = IfCondition.ternary(currentIf.getCondition(),
				nextThen.getCondition(), nextElse.getCondition());
		IfInfo result = new IfInfo(currentIf.getMth(), newCondition, nextThen.getThenBlock(), nextThen.getElseBlock());
		result.merge(currentIf, nextThen, nextElse);
		confirmMerge(result);
		return result;
	}

	private static boolean isInversionNeeded(IfInfo currentIf, IfInfo nextIf) {
		return isEqualPaths(currentIf.getElseBlock(), nextIf.getThenBlock())
				|| isEqualPaths(currentIf.getThenBlock(), nextIf.getElseBlock());
	}

	private static boolean canMerge(IfInfo a, IfInfo b, boolean followThenBranch) {
		if (followThenBranch) {
			return isEqualPaths(a.getElseBlock(), b.getElseBlock());
		} else {
			return isEqualPaths(a.getThenBlock(), b.getThenBlock());
		}
	}

	private static boolean checkConditionBranches(BlockNode from, BlockNode to) {
		return from.getCleanSuccessors().size() == 1 && from.getCleanSuccessors().contains(to);
	}

	static IfInfo mergeIfInfo(IfInfo first, IfInfo second, boolean followThenBranch) {
		MethodNode mth = first.getMth();
		Set<BlockNode> skipBlocks = first.getSkipBlocks();
		BlockNode thenBlock;
		BlockNode elseBlock;
		if (followThenBranch) {
			thenBlock = second.getThenBlock();
			elseBlock = getBranchBlock(first.getElseBlock(), second.getElseBlock(), skipBlocks, mth);
		} else {
			thenBlock = getBranchBlock(first.getThenBlock(), second.getThenBlock(), skipBlocks, mth);
			elseBlock = second.getElseBlock();
		}
		IfCondition.Mode mergeOperation = followThenBranch ? IfCondition.Mode.AND : IfCondition.Mode.OR;
		IfCondition condition = IfCondition.merge(mergeOperation, first.getCondition(), second.getCondition());
		IfInfo result = new IfInfo(mth, condition, thenBlock, elseBlock);
		result.merge(first, second);
		return result;
	}

	private static BlockNode getBranchBlock(BlockNode first, BlockNode second, Set<BlockNode> skipBlocks, MethodNode mth) {
		if (first == second) {
			return second;
		}
		if (isEqualReturnBlocks(first, second)) {
			skipBlocks.add(first);
			return second;
		}
		if (BlockUtils.isDuplicateBlockPath(first, second)) {
			first.add(AFlag.REMOVE);
			skipBlocks.add(first);
			return second;
		}
		BlockNode cross = BlockUtils.getPathCross(mth, first, second);
		if (cross != null) {
			BlockUtils.visitBlocksOnPath(mth, first, cross, skipBlocks::add);
			BlockUtils.visitBlocksOnPath(mth, second, cross, skipBlocks::add);
			skipBlocks.remove(cross);
			return cross;
		}
		BlockNode firstSkip = BlockUtils.followEmptyPath(first);
		BlockNode secondSkip = BlockUtils.followEmptyPath(second);
		if (firstSkip.equals(secondSkip) || isEqualReturnBlocks(firstSkip, secondSkip)) {
			skipBlocks.add(first);
			skipBlocks.add(second);
			BlockUtils.visitBlocksOnEmptyPath(first, skipBlocks::add);
			BlockUtils.visitBlocksOnEmptyPath(second, skipBlocks::add);
			return secondSkip;
		}
		throw new JadxRuntimeException("Unexpected merge pattern");
	}

	static void confirmMerge(IfInfo info) {
		if (info.getMergedBlocks().size() > 1) {
			for (BlockNode block : info.getMergedBlocks()) {
				if (block != info.getFirstIfBlock()) {
					block.add(AFlag.ADDED_TO_REGION);
				}
			}
		}
		if (!info.getSkipBlocks().isEmpty()) {
			for (BlockNode block : info.getSkipBlocks()) {
				block.add(AFlag.ADDED_TO_REGION);
			}
			info.getSkipBlocks().clear();
		}
		for (InsnNode forceInlineInsn : info.getForceInlineInsns()) {
			forceInlineInsn.add(AFlag.FORCE_ASSIGN_INLINE);
		}
	}

	private static IfInfo getNextIf(IfInfo info, BlockNode block) {
		if (!canSelectNext(info, block)) {
			return null;
		}
		return getNextIfNodeInfo(info, block);
	}

	private static boolean canSelectNext(IfInfo info, BlockNode block) {
		if (block.getPredecessors().size() == 1) {
			return true;
		}
		return info.getMergedBlocks().containsAll(block.getPredecessors());
	}

	private static IfInfo getNextIfNodeInfo(IfInfo info, BlockNode block) {
		if (block == null || block.contains(AType.LOOP) || block.contains(AFlag.ADDED_TO_REGION)) {
			return null;
		}
		InsnNode lastInsn = BlockUtils.getLastInsn(block);
		if (lastInsn != null && lastInsn.getType() == InsnType.IF) {
			return makeIfInfo(info.getMth(), block);
		}
		BlockNode next = getNextBlockInIfSuccessorChain(block);
		if (next == null) {
			return null;
		}
		if (next.getPredecessors().size() != 1 || next.contains(AFlag.ADDED_TO_REGION)) {
			return null;
		}
		List<InsnNode> forceInlineInsns = new ArrayList<>();
		if (!checkInsnsInline(block, next, forceInlineInsns)) {
			return null;
		}
		IfInfo nextInfo = makeIfInfo(info.getMth(), next);
		if (nextInfo == null) {
			return getNextIfNodeInfo(info, next);
		}
		nextInfo.addInsnsForForcedInline(forceInlineInsns);
		return nextInfo;
	}

	/**
	 * Allow singular successor to block or 2 successors where one is a EXC_BOTTOM_SPLITTER
	 */
	private static @Nullable BlockNode getNextBlockInIfSuccessorChain(BlockNode block) {

		// skip this block and search in successors chain
		List<BlockNode> successors = block.getSuccessors();
		if (successors.size() > 2 || successors.size() == 0) {
			return null;
		}
		// We might have the next IF and a EXC_BOTTOM_SPLITTER block to delimit a try region
		BlockNode first = successors.get(0);
		if (successors.size() == 1) {
			return first;
		}
		BlockNode second = successors.get(1);
		boolean firstIsHandlerPath = first.contains(AFlag.EXC_BOTTOM_SPLITTER);
		boolean secondIsHandlerPath = second.contains(AFlag.EXC_BOTTOM_SPLITTER);
		if (!firstIsHandlerPath && !secondIsHandlerPath) {
			// unknown case
			return null;
		}
		if (firstIsHandlerPath && secondIsHandlerPath) {
			// unknown case
			return null;
		}
		BlockNode candidate = firstIsHandlerPath ? second : first;

		// Continue to recurse through blocks as long as none of them have any instructions
		if (candidate.getInstructions().isEmpty()) {
			return getNextBlockInIfSuccessorChain(candidate);
		}

		return candidate;
	}

	/**
	 * Check that all instructions can be inlined
	 */
	private static boolean checkInsnsInline(BlockNode block, BlockNode next, List<InsnNode> forceInlineInsns) {
		List<InsnNode> insns = block.getInstructions();
		if (insns.isEmpty()) {
			return true;
		}
		boolean pass = true;
		for (InsnNode insn : insns) {
			RegisterArg res = insn.getResult();
			if (res == null) {
				return false;
			}
			List<RegisterArg> useList = res.getSVar().getUseList();
			int useCount = useList.size();
			if (useCount == 0) {
				// TODO?
				return false;
			}
			InsnArg arg = useList.get(0);
			InsnNode usePlace = arg.getParentInsn();
			if (!BlockUtils.blockContains(block, usePlace)
					&& !BlockUtils.blockContains(next, usePlace)) {
				return false;
			}
			if (useCount > 1) {
				forceInlineInsns.add(insn);
			} else {
				// allow only forced assign inline
				pass = false;
			}
		}
		return pass;
	}
}
