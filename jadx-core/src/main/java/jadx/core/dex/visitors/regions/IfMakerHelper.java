package jadx.core.dex.visitors.regions;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.LoopInfo;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.conditions.IfCondition;
import jadx.core.dex.regions.conditions.IfCondition.Mode;
import jadx.core.dex.regions.conditions.IfInfo;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static jadx.core.dex.visitors.regions.RegionMaker.isEqualPaths;
import static jadx.core.dex.visitors.regions.RegionMaker.isEqualReturnBlocks;
import static jadx.core.utils.BlockUtils.getNextBlock;
import static jadx.core.utils.BlockUtils.isPathExists;

public class IfMakerHelper {
	private static final Logger LOG = LoggerFactory.getLogger(IfMakerHelper.class);

	private IfMakerHelper() {
	}

	static IfInfo makeIfInfo(BlockNode ifBlock) {
		IfNode ifNode = (IfNode) ifBlock.getInstructions().get(0);
		IfCondition condition = IfCondition.fromIfNode(ifNode);
		IfInfo info = new IfInfo(condition, ifNode.getThenBlock(), ifNode.getElseBlock());
		info.setIfBlock(ifBlock);
		info.getMergedBlocks().add(ifBlock);
		return info;
	}

	static IfInfo searchNestedIf(IfInfo info) {
		IfInfo tmp = mergeNestedIfNodes(info);
		return tmp != null ? tmp : info;
	}

	static IfInfo restructureIf(MethodNode mth, BlockNode block, IfInfo info) {
		BlockNode thenBlock = info.getThenBlock();
		BlockNode elseBlock = info.getElseBlock();

		// select 'then', 'else' and 'exit' blocks
		if (thenBlock.contains(AFlag.RETURN) && elseBlock.contains(AFlag.RETURN)) {
			info.setOutBlock(null);
			return info;
		}
		boolean badThen = isBadBranchBlock(info, thenBlock);
		boolean badElse = isBadBranchBlock(info, elseBlock);
		if (badThen && badElse) {
			LOG.debug("Stop processing blocks after 'if': {}, method: {}", info.getIfBlock(), mth);
			return null;
		}
		if (badElse) {
			info = new IfInfo(info, thenBlock, null);
			info.setOutBlock(elseBlock);
		} else if (badThen) {
			info = IfInfo.invert(info);
			info = new IfInfo(info, elseBlock, null);
			info.setOutBlock(thenBlock);
		} else {
			List<BlockNode> thenSC = thenBlock.getCleanSuccessors();
			List<BlockNode> elseSC = elseBlock.getCleanSuccessors();
			if (thenSC.size() == 1 && sameElements(thenSC, elseSC)) {
				info.setOutBlock(thenSC.get(0));
			} else if (info.getMergedBlocks().size() == 1
					&& block.getDominatesOn().size() == 2) {
				info.setOutBlock(BlockUtils.getPathCross(mth, thenBlock, elseBlock));
			}
		}
		if (info.getOutBlock() == null) {
			for (BlockNode d : block.getDominatesOn()) {
				if (d != thenBlock && d != elseBlock
						&& !info.getMergedBlocks().contains(d)
						&& isPathExists(thenBlock, d)) {
					info.setOutBlock(d);
					break;
				}
			}
		}
		if (BlockUtils.isBackEdge(block, info.getOutBlock())) {
			info.setOutBlock(null);
		}
		return info;
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
		return !allPathsFromIf(block, info);
	}

	private static boolean allPathsFromIf(BlockNode block, IfInfo info) {
		List<BlockNode> preds = block.getPredecessors();
		Set<BlockNode> ifBlocks = info.getMergedBlocks();
		for (BlockNode pred : preds) {
			pred = BlockUtils.skipSyntheticPredecessor(pred);
			if (!ifBlocks.contains(pred) && !pred.contains(AFlag.LOOP_END)) {
				return false;
			}
		}
		return true;
	}

	private static boolean sameElements(Collection<BlockNode> c1, Collection<BlockNode> c2) {
		return c1.size() == c2.size() && c1.containsAll(c2);
	}

	static IfInfo mergeNestedIfNodes(IfInfo currentIf) {
		BlockNode curThen = currentIf.getThenBlock();
		BlockNode curElse = currentIf.getElseBlock();
		if (curThen == curElse) {
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
		if (isInversionNeeded(currentIf, nextIf)) {
			// invert current node for match pattern
			nextIf = IfInfo.invert(nextIf);
		}
		if (!isEqualPaths(curThen, nextIf.getThenBlock())
				&& !isEqualPaths(curElse, nextIf.getElseBlock())) {
			// complex condition, run additional checks
			if (checkConditionBranches(curThen, curElse)
					|| checkConditionBranches(curElse, curThen)) {
				return null;
			}
			BlockNode otherBranchBlock = followThenBranch ? curElse : curThen;
			otherBranchBlock = BlockUtils.skipSyntheticSuccessor(otherBranchBlock);
			if (!isPathExists(nextIf.getIfBlock(), otherBranchBlock)) {
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
		if (!nextThen.getIfBlock().getDomFrontier().equals(nextElse.getIfBlock().getDomFrontier())) {
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
		IfInfo result = new IfInfo(newCondition, nextThen.getThenBlock(), nextThen.getElseBlock());
		result.setIfBlock(currentIf.getIfBlock());
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

	private static IfInfo mergeIfInfo(IfInfo first, IfInfo second, boolean followThenBranch) {
		Mode mergeOperation = followThenBranch ? Mode.AND : Mode.OR;

		IfCondition condition = IfCondition.merge(mergeOperation, first.getCondition(), second.getCondition());
		// skip synthetic successor if both parts leads to same block
		BlockNode thenBlock;
		BlockNode elseBlock;
		if (followThenBranch) {
			thenBlock = second.getThenBlock();
			elseBlock = getCrossBlock(first.getElseBlock(), second.getElseBlock());
		} else {
			thenBlock = getCrossBlock(first.getThenBlock(), second.getThenBlock());
			elseBlock = second.getElseBlock();
		}
		IfInfo result = new IfInfo(condition, thenBlock, elseBlock);
		result.setIfBlock(first.getIfBlock());
		result.merge(first, second);

		BlockNode otherPathBlock = followThenBranch ? first.getElseBlock() : first.getThenBlock();
		skipSimplePath(otherPathBlock, result.getSkipBlocks());
		return result;
	}

	private static BlockNode getCrossBlock(BlockNode first, BlockNode second) {
		if (isSameBlocks(first, second)) {
			return second;
		}
		BlockNode firstSkip = BlockUtils.skipSyntheticSuccessor(first);
		if (isSameBlocks(firstSkip, second)) {
			return second;
		}
		BlockNode secondSkip = BlockUtils.skipSyntheticSuccessor(second);
		if (isSameBlocks(firstSkip, secondSkip) || isSameBlocks(first, secondSkip)) {
			return secondSkip;
		}
		throw new JadxRuntimeException("Unexpected merge pattern");
	}

	private static boolean isSameBlocks(BlockNode first, BlockNode second) {
		return first == second || isEqualReturnBlocks(first, second);
	}

	static void confirmMerge(IfInfo info) {
		if (info.getMergedBlocks().size() > 1) {
			for (BlockNode block : info.getMergedBlocks()) {
				if (block != info.getIfBlock()) {
					block.add(AFlag.SKIP);
				}
			}
		}
		if (!info.getSkipBlocks().isEmpty()) {
			for (BlockNode block : info.getSkipBlocks()) {
				block.add(AFlag.SKIP);
			}
			info.getSkipBlocks().clear();
		}
	}

	private static IfInfo getNextIf(IfInfo info, BlockNode block) {
		if (!canSelectNext(info, block)) {
			return null;
		}
		BlockNode nestedIfBlock = getNextIfNode(block);
		if (nestedIfBlock != null) {
			return makeIfInfo(nestedIfBlock);
		}
		return null;
	}

	private static boolean canSelectNext(IfInfo info, BlockNode block) {
		if (block.getPredecessors().size() == 1) {
			return true;
		}
		if (info.getMergedBlocks().containsAll(block.getPredecessors())) {
			return true;
		}
		return false;
	}

	private static BlockNode getNextIfNode(BlockNode block) {
		if (block == null || block.contains(AType.LOOP) || block.contains(AFlag.SKIP)) {
			return null;
		}
		List<InsnNode> insns = block.getInstructions();
		if (insns.size() == 1 && insns.get(0).getType() == InsnType.IF) {
			return block;
		}
		// skip this block and search in successors chain
		List<BlockNode> successors = block.getSuccessors();
		if (successors.size() != 1) {
			return null;
		}

		BlockNode next = successors.get(0);
		if (next.getPredecessors().size() != 1) {
			return null;
		}
		boolean pass = true;
		if (!insns.isEmpty()) {
			// check that all instructions can be inlined
			for (InsnNode insn : insns) {
				RegisterArg res = insn.getResult();
				if (res == null) {
					pass = false;
					break;
				}
				List<RegisterArg> useList = res.getSVar().getUseList();
				if (useList.size() != 1) {
					pass = false;
					break;
				}
				InsnArg arg = useList.get(0);
				InsnNode usePlace = arg.getParentInsn();
				if (!BlockUtils.blockContains(block, usePlace)
						&& !BlockUtils.blockContains(next, usePlace)) {
					pass = false;
					break;
				}
			}
		}
		if (pass) {
			return getNextIfNode(next);
		}
		return null;
	}

	private static void skipSimplePath(BlockNode block, Set<BlockNode> skipped) {
		while (block != null
				&& block.getCleanSuccessors().size() < 2
				&& block.getPredecessors().size() == 1) {
			skipped.add(block);
			block = getNextBlock(block);
		}
	}
}
