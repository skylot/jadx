package jadx.core.dex.visitors.regions;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.regions.IfCondition;
import jadx.core.dex.regions.IfCondition.Mode;
import jadx.core.dex.regions.IfInfo;
import jadx.core.utils.BlockUtils;

import java.util.List;

import static jadx.core.utils.BlockUtils.isPathExists;

public class IfMakerHelper {
	private IfMakerHelper() {
	}

	static IfInfo makeIfInfo(BlockNode ifBlock) {
		return makeIfInfo(ifBlock, IfCondition.fromIfBlock(ifBlock));
	}

	static IfInfo mergeNestedIfNodes(BlockNode block) {
		IfInfo info = makeIfInfo(block);
		return mergeNestedIfNodes(info);
	}

	private static IfInfo mergeNestedIfNodes(IfInfo currentIf) {
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
		if (!RegionMaker.isEqualPaths(curElse, nextIf.getElseBlock())
				&& !RegionMaker.isEqualPaths(curThen, nextIf.getThenBlock())) {
			// complex condition, run additional checks
			if (checkConditionBranches(curThen, curElse) || checkConditionBranches(curElse, curThen)) {
				return null;
			}
			BlockNode otherBranchBlock = followThenBranch ? curElse : curThen;
			if (!isPathExists(nextIf.getIfBlock(), otherBranchBlock)) {
				return null;
			}
			if (isPathExists(nextIf.getThenBlock(), otherBranchBlock)
					&& isPathExists(nextIf.getElseBlock(), otherBranchBlock)) {
				// both branches paths points to one block
				return null;
			}

			// this is nested conditions with different mode (i.e (a && b) || c),
			// search next condition for merge, get null if failed
			IfInfo tmpIf = mergeNestedIfNodes(nextIf);
			if (tmpIf != null) {
				nextIf = tmpIf;
				if (isInversionNeeded(currentIf, nextIf)) {
					nextIf = IfInfo.invert(nextIf);
				}
			}
		}

		IfInfo result = mergeIfInfo(currentIf, nextIf, followThenBranch);
		// search next nested if block
		IfInfo next = mergeNestedIfNodes(result);
		if (next != null) {
			return next;
		}
		return result;
	}

	private static boolean isInversionNeeded(IfInfo currentIf, IfInfo nextIf) {
		return RegionMaker.isEqualPaths(currentIf.getElseBlock(), nextIf.getThenBlock())
				|| RegionMaker.isEqualPaths(currentIf.getThenBlock(), nextIf.getElseBlock());
	}

	private static IfInfo makeIfInfo(BlockNode ifBlock, IfCondition condition) {
		IfNode ifnode = (IfNode) ifBlock.getInstructions().get(0);
		IfInfo info = new IfInfo(condition, ifnode.getThenBlock(), ifnode.getElseBlock());
		info.setIfBlock(ifBlock);
		info.getMergedBlocks().add(ifBlock);
		return info;
	}

	private static boolean checkConditionBranches(BlockNode from, BlockNode to) {
		return from.getCleanSuccessors().size() == 1 && from.getCleanSuccessors().contains(to);
	}

	private static IfInfo mergeIfInfo(IfInfo first, IfInfo second, boolean followThenBranch) {
		Mode mergeOperation = followThenBranch ? Mode.AND : Mode.OR;
		BlockNode otherPathBlock = followThenBranch ? first.getElseBlock() : first.getThenBlock();
		RegionMaker.skipSimplePath(otherPathBlock);
		first.getIfBlock().add(AFlag.SKIP);
		second.getIfBlock().add(AFlag.SKIP);

		IfCondition condition = IfCondition.merge(mergeOperation, first.getCondition(), second.getCondition());
		IfInfo result = new IfInfo(condition, second);
		result.setIfBlock(first.getIfBlock());
		result.getMergedBlocks().addAll(first.getMergedBlocks());
		result.getMergedBlocks().addAll(second.getMergedBlocks());
		return result;
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
}
