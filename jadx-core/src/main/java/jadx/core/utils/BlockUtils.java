package jadx.core.utils;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.IgnoreEdgeAttr;
import jadx.core.dex.attributes.nodes.LoopInfo;
import jadx.core.dex.attributes.nodes.PhiListAttr;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.PhiInsn;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.RegisterArg;
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
		if (b.contains(AType.EXC_HANDLER) || b.contains(AFlag.REMOVE)) {
			return true;
		}
		if (b.contains(AFlag.SYNTHETIC)) {
			List<BlockNode> s = b.getSuccessors();
			return s.size() == 1 && s.get(0).contains(AType.EXC_HANDLER);
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

	public static boolean isFollowBackEdge(BlockNode block) {
		if (block == null) {
			return false;
		}
		if (block.contains(AFlag.LOOP_START)) {
			List<BlockNode> predecessors = block.getPredecessors();
			if (predecessors.size() == 1) {
				BlockNode loopEndBlock = predecessors.get(0);
				if (loopEndBlock.contains(AFlag.LOOP_END)) {
					List<LoopInfo> loops = loopEndBlock.getAll(AType.LOOP);
					for (LoopInfo loop : loops) {
						if (loop.getStart().equals(block) && loop.getEnd().equals(loopEndBlock)) {
							return true;
						}
					}
				}
			}
		}
		return false;
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
	public static InsnNode getLastInsn(@Nullable IBlock block) {
		if (block == null) {
			return null;
		}
		List<InsnNode> insns = block.getInstructions();
		if (insns.isEmpty()) {
			return null;
		}
		return insns.get(insns.size() - 1);
	}

	@Nullable
	public static BlockNode getBlockByInsn(MethodNode mth, @Nullable InsnNode insn) {
		if (insn == null) {
			return null;
		}
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

	public static BitSet newBlocksBitSet(MethodNode mth) {
		return new BitSet(mth.getBasicBlocks().size());
	}

	public static BitSet copyBlocksBitSet(MethodNode mth, BitSet bitSet) {
		BitSet copy = new BitSet(mth.getBasicBlocks().size());
		if (!bitSet.isEmpty()) {
			copy.or(bitSet);
		}
		return copy;
	}

	public static BitSet blocksToBitSet(MethodNode mth, Collection<BlockNode> blocks) {
		BitSet bs = new BitSet(mth.getBasicBlocks().size());
		for (BlockNode block : blocks) {
			bs.set(block.getId());
		}
		return bs;
	}

	@Nullable
	public static BlockNode bitSetToOneBlock(MethodNode mth, BitSet bs) {
		if (bs == null || bs.cardinality() != 1) {
			return null;
		}
		return mth.getBasicBlocks().get(bs.nextSetBit(0));
	}

	public static List<BlockNode> bitSetToBlocks(MethodNode mth, BitSet bs) {
		if (bs == null || bs == EmptyBitSet.EMPTY) {
			return Collections.emptyList();
		}
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
		if (b1.getDomFrontier() == null || b2.getDomFrontier() == null) {
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
		collectWhileDominates(dominator, start, result, new HashSet<>(), false);
		return result;
	}

	/**
	 * Collect all block dominated by 'dominator', starting from 'start', include exception handlers
	 */
	public static List<BlockNode> collectBlocksDominatedByWithExcHandlers(BlockNode dominator, BlockNode start) {
		List<BlockNode> result = new ArrayList<>();
		collectWhileDominates(dominator, start, result, new HashSet<>(), true);
		return result;
	}

	private static void collectWhileDominates(BlockNode dominator, BlockNode child, List<BlockNode> result,
			Set<BlockNode> visited, boolean includeExcHandlers) {
		if (visited.contains(child)) {
			return;
		}
		visited.add(child);
		List<BlockNode> successors = includeExcHandlers ? child.getSuccessors() : child.getCleanSuccessors();
		for (BlockNode node : successors) {
			if (node.isDominator(dominator)) {
				result.add(node);
				collectWhileDominates(dominator, node, result, visited, includeExcHandlers);
			}
		}
	}

	public static List<BlockNode> buildSimplePath(BlockNode block) {
		if (block == null) {
			return Collections.emptyList();
		}
		List<BlockNode> list = new ArrayList<>();
		if (block.getCleanSuccessors().size() >= 2) {
			return Collections.emptyList();
		}
		list.add(block);

		BlockNode currentBlock = getNextBlock(block);
		while (currentBlock != null
				&& currentBlock.getCleanSuccessors().size() < 2
				&& currentBlock.getPredecessors().size() == 1) {
			list.add(currentBlock);
			currentBlock = getNextBlock(currentBlock);
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
				pred.add(AFlag.DONT_GENERATE);
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

	public static List<InsnNode> collectAllInsns(List<BlockNode> blocks) {
		List<InsnNode> insns = new ArrayList<>();
		blocks.forEach(block -> insns.addAll(block.getInstructions()));
		return insns;
	}

	public static boolean isFirstInsn(MethodNode mth, InsnNode insn) {
		BlockNode enterBlock = mth.getEnterBlock();
		if (enterBlock == null || enterBlock.getInstructions().isEmpty()) {
			return false;
		}
		return enterBlock.getInstructions().get(0) == insn;
	}

	/**
	 * Replace insn by index i in block,
	 * for proper copy attributes, assume attributes are not overlap
	 */
	public static void replaceInsn(MethodNode mth, BlockNode block, int i, InsnNode insn) {
		InsnNode prevInsn = block.getInstructions().get(i);
		insn.copyAttributesFrom(prevInsn);
		insn.setSourceLine(prevInsn.getSourceLine());
		insn.setOffset(prevInsn.getOffset());
		block.getInstructions().set(i, insn);

		RegisterArg result = insn.getResult();
		RegisterArg prevResult = prevInsn.getResult();
		if (result != null && prevResult != null && result.sameRegAndSVar(prevResult)) {
			// Don't unbind result for same register.
			// Unbind will remove arg from PHI and not add it back on rebind.
			InsnRemover.unbindAllArgs(mth, prevInsn);
		} else {
			InsnRemover.unbindInsn(mth, prevInsn);
		}
		insn.rebindArgs();
	}

	public static boolean replaceInsn(MethodNode mth, BlockNode block, InsnNode oldInsn, InsnNode newInsn) {
		List<InsnNode> instructions = block.getInstructions();
		int size = instructions.size();
		for (int i = 0; i < size; i++) {
			InsnNode instruction = instructions.get(i);
			if (instruction == oldInsn) {
				replaceInsn(mth, block, i, newInsn);
				return true;
			}
		}
		return false;
	}

	public static boolean insertBeforeInsn(BlockNode block, InsnNode insn, InsnNode newInsn) {
		int index = getInsnIndexInBlock(block, insn);
		if (index == -1) {
			return false;
		}
		block.getInstructions().add(index, newInsn);
		return true;
	}

	public static boolean insertAfterInsn(BlockNode block, InsnNode insn, InsnNode newInsn) {
		int index = getInsnIndexInBlock(block, insn);
		if (index == -1) {
			return false;
		}
		block.getInstructions().add(index + 1, newInsn);
		return true;
	}

	public static int getInsnIndexInBlock(BlockNode block, InsnNode insn) {
		List<InsnNode> instructions = block.getInstructions();
		int size = instructions.size();
		for (int i = 0; i < size; i++) {
			if (instructions.get(i) == insn) {
				return i;
			}
		}
		return -1;
	}

	public static boolean replaceInsn(MethodNode mth, InsnNode oldInsn, InsnNode newInsn) {
		for (BlockNode block : mth.getBasicBlocks()) {
			if (replaceInsn(mth, block, oldInsn, newInsn)) {
				return true;
			}
		}
		return false;
	}

	public static Map<BlockNode, BitSet> calcPostDominance(MethodNode mth) {
		return calcPartialPostDominance(mth, mth.getBasicBlocks(), mth.getExitBlocks().get(0));
	}

	public static Map<BlockNode, BitSet> calcPartialPostDominance(MethodNode mth, Collection<BlockNode> blockNodes, BlockNode exitBlock) {
		int blocksCount = mth.getBasicBlocks().size();
		Map<BlockNode, BitSet> map = new HashMap<>(blocksCount);

		BitSet initSet = new BitSet(blocksCount);
		for (BlockNode block : blockNodes) {
			initSet.set(block.getId());
		}

		for (BlockNode block : blockNodes) {
			BitSet postDoms = new BitSet(blocksCount);
			postDoms.or(initSet);
			map.put(block, postDoms);
		}
		BitSet exitBitSet = map.get(exitBlock);
		exitBitSet.clear();
		exitBitSet.set(exitBlock.getId());

		BitSet domSet = new BitSet(blocksCount);
		boolean changed;
		do {
			changed = false;
			for (BlockNode block : blockNodes) {
				if (block == exitBlock) {
					continue;
				}
				BitSet d = map.get(block);
				if (!changed) {
					domSet.clear();
					domSet.or(d);
				}
				for (BlockNode scc : block.getSuccessors()) {
					BitSet scPDoms = map.get(scc);
					if (scPDoms != null) {
						d.and(scPDoms);
					}
				}
				d.set(block.getId());
				if (!changed && !d.equals(domSet)) {
					changed = true;
					map.put(block, d);
				}
			}
		} while (changed);

		blockNodes.forEach(block -> {
			BitSet postDoms = map.get(block);
			postDoms.clear(block.getId());
			if (postDoms.isEmpty()) {
				map.put(block, EmptyBitSet.EMPTY);
			}
		});
		return map;
	}

	@Nullable
	public static BlockNode calcImmediatePostDominator(MethodNode mth, BlockNode block) {
		BlockNode oneSuccessor = Utils.getOne(block.getSuccessors());
		if (oneSuccessor != null) {
			return oneSuccessor;
		}
		return calcImmediatePostDominator(mth, block, calcPostDominance(mth));
	}

	@Nullable
	public static BlockNode calcPartialImmediatePostDominator(MethodNode mth, BlockNode block,
			Collection<BlockNode> blockNodes, BlockNode exitBlock) {
		BlockNode oneSuccessor = Utils.getOne(block.getSuccessors());
		if (oneSuccessor != null) {
			return oneSuccessor;
		}
		Map<BlockNode, BitSet> pDomsMap = calcPartialPostDominance(mth, blockNodes, exitBlock);
		return calcImmediatePostDominator(mth, block, pDomsMap);
	}

	@Nullable
	public static BlockNode calcImmediatePostDominator(MethodNode mth, BlockNode block, Map<BlockNode, BitSet> postDomsMap) {
		BlockNode oneSuccessor = Utils.getOne(block.getSuccessors());
		if (oneSuccessor != null) {
			return oneSuccessor;
		}
		List<BlockNode> basicBlocks = mth.getBasicBlocks();
		BitSet postDoms = postDomsMap.get(block);
		BitSet bs = copyBlocksBitSet(mth, postDoms);
		for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
			BlockNode pdomBlock = basicBlocks.get(i);
			BitSet pdoms = postDomsMap.get(pdomBlock);
			if (pdoms != null) {
				bs.andNot(pdoms);
			}
		}
		return bitSetToOneBlock(mth, bs);
	}
}
