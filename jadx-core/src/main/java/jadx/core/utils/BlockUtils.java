package jadx.core.utils;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.IgnoreEdgeAttr;
import jadx.core.dex.attributes.nodes.PhiListAttr;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.PhiInsn;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.mods.TernaryInsn;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.conditions.IfCondition;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class BlockUtils {

	private BlockUtils() {
	}

	public static BlockNode getBlockByOffset(int offset, Iterable<BlockNode> casesBlocks) {
		for (BlockNode block : casesBlocks) {
			if (block.getStartOffset() == offset) {
				return block;
			}
		}
		throw new JadxRuntimeException("Can't find block by offset: "
				+ InsnUtils.formatOffset(offset)
				+ " in list " + casesBlocks);
	}

	public static BlockNode selectOther(BlockNode node, List<BlockNode> blocks) {
		List<BlockNode> list = blocks;
		if (list.size() > 2) {
			list = cleanBlockList(list);
		}
		if (list.size() != 2) {
			throw new JadxRuntimeException("Incorrect nodes count for selectOther: " + node + " in " + list);
		}
		BlockNode first = list.get(0);
		if (first != node) {
			return first;
		} else {
			return list.get(1);
		}
	}

	public static BlockNode selectOtherSafe(BlockNode node, List<BlockNode> blocks) {
		int size = blocks.size();
		if (size == 1) {
			BlockNode first = blocks.get(0);
			return first != node ? first : null;
		}
		if (size == 2) {
			BlockNode first = blocks.get(0);
			return first != node ? first : blocks.get(1);
		}
		return null;
	}

	public static boolean isBlockMustBeCleared(BlockNode b) {
		if (b.contains(AType.EXC_HANDLER) || b.contains(AFlag.SKIP)) {
			return true;
		}
		if (b.contains(AFlag.SYNTHETIC)) {
			List<BlockNode> s = b.getSuccessors();
			if (s.size() == 1 && s.get(0).contains(AType.EXC_HANDLER)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Remove exception handlers from block nodes list
	 */
	private static List<BlockNode> cleanBlockList(List<BlockNode> list) {
		List<BlockNode> ret = new ArrayList<>(list.size());
		for (BlockNode block : list) {
			if (!isBlockMustBeCleared(block)) {
				ret.add(block);
			}
		}
		return ret;
	}

	/**
	 * Remove exception handlers from block nodes bitset
	 */
	public static void cleanBitSet(MethodNode mth, BitSet bs) {
		for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
			BlockNode block = mth.getBasicBlocks().get(i);
			if (isBlockMustBeCleared(block)) {
				bs.clear(i);
			}
		}
	}

	/**
	 * Return predecessors list without blocks contains 'IGNORE_EDGE' attribute.
	 *
	 * @return new list of filtered predecessors
	 */
	public static List<BlockNode> filterPredecessors(BlockNode block) {
		List<BlockNode> predecessors = block.getPredecessors();
		List<BlockNode> list = new ArrayList<>(predecessors.size());
		for (BlockNode pred : predecessors) {
			IgnoreEdgeAttr edgeAttr = pred.get(AType.IGNORE_EDGE);
			if (edgeAttr == null || !edgeAttr.contains(block)) {
				list.add(pred);
			}
		}
		return list;
	}

	public static boolean isBackEdge(BlockNode from, BlockNode to) {
		if (to == null) {
			return false;
		}
		if (from.getCleanSuccessors().contains(to)) {
			return false; // already checked
		}
		return from.getSuccessors().contains(to);
	}

	/**
	 * Check if instruction contains in block (use == for comparison, not equals)
	 */
	public static boolean blockContains(BlockNode block, InsnNode insn) {
		for (InsnNode bi : block.getInstructions()) {
			if (bi == insn) {
				return true;
			}
		}
		return false;
	}

	public static boolean checkLastInsnType(IBlock block, InsnType expectedType) {
		InsnNode insn = getLastInsn(block);
		return insn != null && insn.getType() == expectedType;
	}

	@Nullable
	public static InsnNode getLastInsn(IBlock block) {
		List<InsnNode> insns = block.getInstructions();
		if (insns.isEmpty()) {
			return null;
		}
		return insns.get(insns.size() - 1);
	}

	public static BlockNode getBlockByInsn(MethodNode mth, InsnNode insn) {
		if (insn instanceof PhiInsn) {
			return searchBlockWithPhi(mth, (PhiInsn) insn);
		}
		if (insn.contains(AFlag.WRAPPED)) {
			return getBlockByWrappedInsn(mth, insn);
		}
		for (BlockNode bn : mth.getBasicBlocks()) {
			if (blockContains(bn, insn)) {
				return bn;
			}
		}
		return null;
	}

	public static BlockNode searchBlockWithPhi(MethodNode mth, PhiInsn insn) {
		for (BlockNode block : mth.getBasicBlocks()) {
			PhiListAttr phiListAttr = block.get(AType.PHI_LIST);
			if (phiListAttr != null) {
				for (PhiInsn phiInsn : phiListAttr.getList()) {
					if (phiInsn == insn) {
						return block;
					}
				}
			}
		}
		return null;
	}

	private static BlockNode getBlockByWrappedInsn(MethodNode mth, InsnNode insn) {
		for (BlockNode bn : mth.getBasicBlocks()) {
			for (InsnNode bi : bn.getInstructions()) {
				if (bi == insn || foundWrappedInsn(bi, insn) != null) {
					return bn;
				}
			}
		}
		return null;
	}

	public static InsnNode searchInsnParent(MethodNode mth, InsnNode insn) {
		InsnArg insnArg = searchWrappedInsnParent(mth, insn);
		if (insnArg == null) {
			return null;
		}
		return insnArg.getParentInsn();
	}

	public static InsnArg searchWrappedInsnParent(MethodNode mth, InsnNode insn) {
		if (!insn.contains(AFlag.WRAPPED)) {
			return null;
		}
		for (BlockNode bn : mth.getBasicBlocks()) {
			for (InsnNode bi : bn.getInstructions()) {
				InsnArg res = foundWrappedInsn(bi, insn);
				if (res != null) {
					return res;
				}
			}
		}
		return null;
	}

	private static InsnArg foundWrappedInsn(InsnNode container, InsnNode insn) {
		for (InsnArg arg : container.getArguments()) {
			if (arg.isInsnWrap()) {
				InsnNode wrapInsn = ((InsnWrapArg) arg).getWrapInsn();
				if (wrapInsn == insn) {
					return arg;
				}
				InsnArg res = foundWrappedInsn(wrapInsn, insn);
				if (res != null) {
					return res;
				}
			}
		}
		if (container instanceof TernaryInsn) {
			return foundWrappedInsnInCondition(((TernaryInsn) container).getCondition(), insn);
		}
		return null;
	}

	private static InsnArg foundWrappedInsnInCondition(IfCondition cond, InsnNode insn) {
		if (cond.isCompare()) {
			IfNode cmpInsn = cond.getCompare().getInsn();
			return foundWrappedInsn(cmpInsn, insn);
		}
		for (IfCondition nestedCond : cond.getArgs()) {
			InsnArg res = foundWrappedInsnInCondition(nestedCond, insn);
			if (res != null) {
				return res;
			}
		}
		return null;
	}

	public static BitSet blocksToBitSet(MethodNode mth, List<BlockNode> blocks) {
		BitSet bs = new BitSet(mth.getBasicBlocks().size());
		for (BlockNode block : blocks) {
			bs.set(block.getId());
		}
		return bs;
	}

	public static List<BlockNode> bitSetToBlocks(MethodNode mth, BitSet bs) {
		int size = bs.cardinality();
		if (size == 0) {
			return Collections.emptyList();
		}
		List<BlockNode> blocks = new ArrayList<>(size);
		for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
			BlockNode block = mth.getBasicBlocks().get(i);
			blocks.add(block);
		}
		return blocks;
	}

	/**
	 * Return first successor which not exception handler and not follow loop back edge
	 */
	public static BlockNode getNextBlock(BlockNode block) {
		List<BlockNode> s = block.getCleanSuccessors();
		return s.isEmpty() ? null : s.get(0);
	}

	/**
	 * Return successor on path to 'pathEnd' block
	 */
	public static BlockNode getNextBlockToPath(BlockNode block, BlockNode pathEnd) {
		List<BlockNode> successors = block.getCleanSuccessors();
		if (successors.contains(pathEnd)) {
			return pathEnd;
		}
		Set<BlockNode> path = getAllPathsBlocks(block, pathEnd);
		for (BlockNode s : successors) {
			if (path.contains(s)) {
				return s;
			}
		}
		return null;
	}

	/**
	 * Collect blocks from all possible execution paths from 'start' to 'end'
	 */
	public static Set<BlockNode> getAllPathsBlocks(BlockNode start, BlockNode end) {
		Set<BlockNode> set = new HashSet<>();
		set.add(start);
		if (start != end) {
			addPredecessors(set, end, start);
		}
		return set;
	}

	private static void addPredecessors(Set<BlockNode> set, BlockNode from, BlockNode until) {
		set.add(from);
		for (BlockNode pred : from.getPredecessors()) {
			if (pred != until && !set.contains(pred)) {
				addPredecessors(set, pred, until);
			}
		}
	}

	private static boolean traverseSuccessorsUntil(BlockNode from, BlockNode until, BitSet visited, boolean clean) {
		List<BlockNode> nodes = clean ? from.getCleanSuccessors() : from.getSuccessors();
		for (BlockNode s : nodes) {
			if (s == until) {
				return true;
			}
			int id = s.getId();
			if (!visited.get(id)) {
				visited.set(id);
				if (until.isDominator(s)) {
					return true;
				}
				if (traverseSuccessorsUntil(s, until, visited, clean)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isPathExists(BlockNode start, BlockNode end) {
		if (start == end
				|| end.isDominator(start)
				|| start.getCleanSuccessors().contains(end)) {
			return true;
		}
		if (start.getPredecessors().contains(end)) {
			return false;
		}
		return traverseSuccessorsUntil(start, end, new BitSet(), true);
	}

	public static boolean isAnyPathExists(BlockNode start, BlockNode end) {
		if (start == end
				|| end.isDominator(start)
				|| start.getSuccessors().contains(end)) {
			return true;
		}
		if (start.getPredecessors().contains(end)) {
			return false;
		}
		return traverseSuccessorsUntil(start, end, new BitSet(), false);
	}

	public static BlockNode getTopBlock(Collection<BlockNode> blocks) {
		if (blocks.size() == 1) {
			return blocks.iterator().next();
		}
		for (BlockNode from : blocks) {
			boolean top = true;
			for (BlockNode to : blocks) {
				if (from != to && !isAnyPathExists(from, to)) {
					top = false;
					break;
				}
			}
			if (top) {
				return from;
			}
		}
		return null;
	}

	public static boolean isOnlyOnePathExists(BlockNode start, BlockNode end) {
		if (start == end) {
			return true;
		}
		if (!end.isDominator(start)) {
			return false;
		}
		BlockNode currentNode = start;
		while (currentNode.getCleanSuccessors().size() == 1) {
			currentNode = currentNode.getCleanSuccessors().get(0);
			if (currentNode == end) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Search for first node which not dominated by dom, starting from start
	 */
	public static BlockNode traverseWhileDominates(BlockNode dom, BlockNode start) {
		for (BlockNode node : start.getCleanSuccessors()) {
			if (!node.isDominator(dom)) {
				return node;
			} else {
				BlockNode out = traverseWhileDominates(dom, node);
				if (out != null) {
					return out;
				}
			}
		}
		return null;
	}

	public static BlockNode getPathCross(MethodNode mth, BlockNode b1, BlockNode b2) {
		if (b1 == null || b2 == null) {
			return null;
		}
		BitSet b = new BitSet();
		b.or(b1.getDomFrontier());
		b.and(b2.getDomFrontier());
		b.clear(b1.getId());
		b.clear(b2.getId());
		if (b.cardinality() == 1) {
			BlockNode end = mth.getBasicBlocks().get(b.nextSetBit(0));
			if (isPathExists(b1, end) && isPathExists(b2, end)) {
				return end;
			}
		}
		if (isPathExists(b1, b2)) {
			return b2;
		}
		if (isPathExists(b2, b1)) {
			return b1;
		}
		return null;
	}

	/**
	 * Collect all block dominated by 'dominator', starting from 'start'
	 */
	public static List<BlockNode> collectBlocksDominatedBy(BlockNode dominator, BlockNode start) {
		List<BlockNode> result = new ArrayList<>();
		HashSet<BlockNode> visited = new HashSet<BlockNode>();
		collectWhileDominates(dominator, start, result, visited);
		return result;
	}

	private static void collectWhileDominates(BlockNode dominator, BlockNode child, List<BlockNode> result,
			HashSet<BlockNode> visited) {
		if (visited.contains(child)) {
			return;
		}
		visited.add(child);
		for (BlockNode node : child.getCleanSuccessors()) {
			if (node.isDominator(dominator)) {
				result.add(node);
				collectWhileDominates(dominator, node, result, visited);
			}
		}
	}

	public static List<BlockNode> buildSimplePath(BlockNode block) {
		List<BlockNode> list = new LinkedList<>();
		BlockNode currentBlock = block;
		while (currentBlock != null
				&& currentBlock.getCleanSuccessors().size() < 2
				&& currentBlock.getPredecessors().size() == 1) {
			list.add(currentBlock);
			currentBlock = getNextBlock(currentBlock);
		}
		if (list.isEmpty()) {
			return Collections.emptyList();
		}
		return list;
	}

	/**
	 * Set 'SKIP' flag for all synthetic predecessors from start block.
	 */
	public static void skipPredSyntheticPaths(BlockNode block) {
		for (BlockNode pred : block.getPredecessors()) {
			if (pred.contains(AFlag.SYNTHETIC)
					&& !pred.contains(AType.SPLITTER_BLOCK)
					&& pred.getInstructions().isEmpty()) {
				pred.add(AFlag.SKIP);
				skipPredSyntheticPaths(pred);
			}
		}
	}

	/**
	 * Return true if on path from start to end no instructions and no branches.
	 */
	public static boolean isEmptySimplePath(BlockNode start, BlockNode end) {
		if (start == end && start.getInstructions().isEmpty()) {
			return true;
		}
		if (!start.getInstructions().isEmpty() || start.getCleanSuccessors().size() != 1) {
			return false;
		}
		BlockNode block = getNextBlock(start);
		while (block != null
				&& block != end
				&& block.getCleanSuccessors().size() < 2
				&& block.getPredecessors().size() == 1
				&& block.getInstructions().isEmpty()) {
			block = getNextBlock(block);
		}
		return block == end;
	}

	/**
	 * Return successor of synthetic block or same block otherwise.
	 */
	public static BlockNode skipSyntheticSuccessor(BlockNode block) {
		if (block.isSynthetic() && block.getSuccessors().size() == 1) {
			return block.getSuccessors().get(0);
		}
		return block;
	}

	/**
	 * Return predecessor of synthetic block or same block otherwise.
	 */
	public static BlockNode skipSyntheticPredecessor(BlockNode block) {
		if (block.isSynthetic() && block.getPredecessors().size() == 1) {
			return block.getPredecessors().get(0);
		}
		return block;
	}

	public static boolean isAllBlocksEmpty(List<BlockNode> blocks) {
		for (BlockNode block : blocks) {
			if (!block.getInstructions().isEmpty()) {
				return false;
			}
		}
		return true;
	}
}
