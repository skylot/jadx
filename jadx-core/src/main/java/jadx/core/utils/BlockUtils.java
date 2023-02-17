package jadx.core.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
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
import jadx.core.dex.trycatch.CatchAttr;
import jadx.core.dex.trycatch.ExceptionHandler;
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

	public static boolean isExceptionHandlerPath(BlockNode b) {
		if (b.contains(AType.EXC_HANDLER)
				|| b.contains(AFlag.EXC_BOTTOM_SPLITTER)
				|| b.contains(AFlag.REMOVE)) {
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
			if (!isExceptionHandlerPath(block)) {
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
			if (isExceptionHandlerPath(block)) {
				bs.clear(i);
			}
		}
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

	public static boolean checkFirstInsn(IBlock block, Predicate<InsnNode> predicate) {
		InsnNode insn = getFirstInsn(block);
		return insn != null && predicate.test(insn);
	}

	public static boolean checkLastInsnType(IBlock block, InsnType expectedType) {
		InsnNode insn = getLastInsn(block);
		return insn != null && insn.getType() == expectedType;
	}

	public static InsnNode getLastInsnWithType(IBlock block, InsnType expectedType) {
		InsnNode insn = getLastInsn(block);
		if (insn != null && insn.getType() == expectedType) {
			return insn;
		}
		return null;
	}

	public static int getFirstSourceLine(IBlock block) {
		for (InsnNode insn : block.getInstructions()) {
			int line = insn.getSourceLine();
			if (line != 0) {
				return line;
			}
		}
		return 0;
	}

	@Nullable
	public static InsnNode getFirstInsn(@Nullable IBlock block) {
		if (block == null) {
			return null;
		}
		List<InsnNode> insns = block.getInstructions();
		if (insns.isEmpty()) {
			return null;
		}
		return insns.get(0);
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

	public static boolean isExitBlock(MethodNode mth, BlockNode block) {
		if (block == mth.getExitBlock()) {
			return true;
		}
		return isExitBlock(block);
	}

	public static boolean isExitBlock(BlockNode block) {
		List<BlockNode> successors = block.getSuccessors();
		if (successors.isEmpty()) {
			return true;
		}
		if (successors.size() == 1) {
			BlockNode next = successors.get(0);
			return next.getSuccessors().isEmpty();
		}
		return false;
	}

	public static boolean containsExitInsn(IBlock block) {
		InsnNode lastInsn = BlockUtils.getLastInsn(block);
		if (lastInsn == null) {
			return false;
		}
		InsnType type = lastInsn.getType();
		return type == InsnType.RETURN
				|| type == InsnType.THROW
				|| type == InsnType.BREAK
				|| type == InsnType.CONTINUE;
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
		BitSet bs = newBlocksBitSet(mth);
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

	public static void forEachBlockFromBitSet(MethodNode mth, BitSet bs, Consumer<BlockNode> consumer) {
		if (bs == null || bs == EmptyBitSet.EMPTY || bs.isEmpty()) {
			return;
		}
		List<BlockNode> blocks = mth.getBasicBlocks();
		for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
			consumer.accept(blocks.get(i));
		}
	}

	/**
	 * Return first successor which not exception handler and not follow loop back edge
	 */
	@Nullable
	public static BlockNode getNextBlock(BlockNode block) {
		List<BlockNode> s = block.getCleanSuccessors();
		return s.isEmpty() ? null : s.get(0);
	}

	@Nullable
	public static BlockNode getPrevBlock(BlockNode block) {
		List<BlockNode> preds = block.getPredecessors();
		return preds.size() == 1 ? preds.get(0) : null;
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
	 * Visit blocks on any path from start to end.
	 * Only one path will be visited!
	 */
	public static boolean visitBlocksOnPath(MethodNode mth, BlockNode start, BlockNode end, Consumer<BlockNode> visitor) {
		visitor.accept(start);
		if (start == end) {
			return true;
		}
		if (start.getCleanSuccessors().contains(end)) {
			visitor.accept(end);
			return true;
		}
		// DFS on clean successors
		BitSet visited = newBlocksBitSet(mth);
		Deque<BlockNode> queue = new ArrayDeque<>();
		queue.addLast(start);
		while (true) {
			BlockNode current = queue.peekLast();
			if (current == null) {
				return false;
			}
			boolean added = false;
			for (BlockNode next : current.getCleanSuccessors()) {
				if (next == end) {
					queue.removeFirst(); // start already visited
					queue.addLast(next);
					queue.forEach(visitor);
					return true;
				}
				int id = next.getId();
				if (!visited.get(id)) {
					visited.set(id);
					queue.addLast(next);
					added = true;
					break;
				}
			}
			if (!added) {
				queue.pollLast();
				if (queue.isEmpty()) {
					return false;
				}
			}
		}
	}

	public static void dfsVisit(MethodNode mth, Consumer<BlockNode> visitor) {
		BitSet visited = newBlocksBitSet(mth);
		Deque<BlockNode> queue = new ArrayDeque<>();
		BlockNode enterBlock = mth.getEnterBlock();
		queue.addLast(enterBlock);
		visited.set(mth.getEnterBlock().getId());
		while (true) {
			BlockNode current = queue.pollLast();
			if (current == null) {
				return;
			}
			visitor.accept(current);
			List<BlockNode> successors = current.getSuccessors();
			int count = successors.size();
			for (int i = count - 1; i >= 0; i--) { // to preserve order in queue
				BlockNode next = successors.get(i);
				int nextId = next.getId();
				if (!visited.get(nextId)) {
					queue.addLast(next);
					visited.set(nextId);
				}
			}
		}
	}

	public static List<BlockNode> collectPredecessors(MethodNode mth, BlockNode start, Collection<BlockNode> stopBlocks) {
		BitSet bs = newBlocksBitSet(mth);
		if (!stopBlocks.isEmpty()) {
			bs.or(blocksToBitSet(mth, stopBlocks));
		}
		List<BlockNode> list = new ArrayList<>();
		traversePredecessors(start, bs, list::add);
		return list;
	}

	/**
	 * Up BFS
	 */
	private static void traversePredecessors(BlockNode start, BitSet visited, Consumer<BlockNode> visitor) {
		Queue<BlockNode> queue = new ArrayDeque<>();
		queue.add(start);
		while (true) {
			BlockNode current = queue.poll();
			if (current == null) {
				return;
			}
			visitor.accept(current);
			for (BlockNode next : current.getPredecessors()) {
				int id = next.getId();
				if (!visited.get(id)) {
					visited.set(id);
					queue.add(next);
				}
			}
		}
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

	/**
	 * Search at least one path from startBlocks to end
	 */
	public static boolean atLeastOnePathExists(Collection<BlockNode> startBlocks, BlockNode end) {
		for (BlockNode startBlock : startBlocks) {
			if (isPathExists(startBlock, end)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if exist path from every startBlocks to end
	 */
	public static boolean isAllPathExists(Collection<BlockNode> startBlocks, BlockNode end) {
		for (BlockNode startBlock : startBlocks) {
			if (!isPathExists(startBlock, end)) {
				return false;
			}
		}
		return true;
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

	public static BlockNode getTopBlock(List<BlockNode> blocks) {
		if (blocks.size() == 1) {
			return blocks.get(0);
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

	/**
	 * Search last block in control flow graph from input set.
	 */
	@Nullable
	public static BlockNode getBottomBlock(List<BlockNode> blocks) {
		if (blocks.size() == 1) {
			return blocks.get(0);
		}
		for (BlockNode bottomCandidate : blocks) {
			boolean bottom = true;
			for (BlockNode from : blocks) {
				if (bottomCandidate != from && !isAnyPathExists(from, bottomCandidate)) {
					bottom = false;
					break;
				}
			}
			if (bottom) {
				return bottomCandidate;
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

	/**
	 * Search the lowest common ancestor in dominator tree for input set.
	 */
	@Nullable
	public static BlockNode getCommonDominator(MethodNode mth, List<BlockNode> blocks) {
		BitSet doms = newBlocksBitSet(mth);
		// collect all dominators from input set
		doms.set(0, mth.getBasicBlocks().size());
		blocks.forEach(b -> doms.and(b.getDoms()));
		// exclude all dominators of immediate dominator (including self)
		BitSet combine = newBlocksBitSet(mth);
		combine.or(doms);
		forEachBlockFromBitSet(mth, doms, block -> {
			BlockNode idom = block.getIDom();
			if (idom != null) {
				combine.andNot(idom.getDoms());
				combine.clear(idom.getId());
			}
		});
		return bitSetToOneBlock(mth, combine);
	}

	/**
	 * Return common cross block for input set.
	 *
	 * @return null if cross is a method exit block.
	 */
	@Nullable
	public static BlockNode getPathCross(MethodNode mth, Collection<BlockNode> blocks) {
		BitSet domFrontBS = newBlocksBitSet(mth);
		boolean first = true;
		for (BlockNode b : blocks) {
			if (first) {
				domFrontBS.or(b.getDomFrontier());
				first = false;
			} else {
				domFrontBS.and(b.getDomFrontier());
			}
		}
		domFrontBS.clear(mth.getExitBlock().getId());
		if (domFrontBS.isEmpty()) {
			return null;
		}
		BlockNode oneBlock = bitSetToOneBlock(mth, domFrontBS);
		if (oneBlock != null) {
			return oneBlock;
		}
		BitSet excluded = newBlocksBitSet(mth);
		// exclude method exit and loop start blocks
		excluded.set(mth.getExitBlock().getId());
		// exclude loop start blocks
		mth.getLoops().forEach(l -> excluded.set(l.getStart().getId()));
		if (!mth.isNoExceptionHandlers()) {
			// exclude exception handlers paths
			mth.getExceptionHandlers().forEach(h -> mergeExcHandlerDomFrontier(mth, h, excluded));
		}
		domFrontBS.andNot(excluded);
		oneBlock = bitSetToOneBlock(mth, domFrontBS);
		if (oneBlock != null) {
			return oneBlock;
		}
		BitSet combinedDF = newBlocksBitSet(mth);
		int k = mth.getBasicBlocks().size();
		while (true) {
			// collect dom frontier blocks from current set until only one block left
			forEachBlockFromBitSet(mth, domFrontBS, block -> {
				BitSet domFrontier = block.getDomFrontier();
				if (!domFrontier.isEmpty()) {
					combinedDF.or(domFrontier);
					combinedDF.clear(block.getId());
				}
			});
			combinedDF.andNot(excluded);
			int cardinality = combinedDF.cardinality();
			if (cardinality == 1) {
				return bitSetToOneBlock(mth, combinedDF);
			}
			if (cardinality == 0) {
				return null;
			}
			if (k-- < 0) {
				mth.addWarnComment("Path cross not found for " + blocks + ", limit reached: " + mth.getBasicBlocks().size());
				return null;
			}
			// replace domFrontBS with combinedDF
			domFrontBS.clear();
			domFrontBS.or(combinedDF);
			combinedDF.clear();
		}
	}

	private static void mergeExcHandlerDomFrontier(MethodNode mth, ExceptionHandler handler, BitSet set) {
		BlockNode handlerBlock = handler.getHandlerBlock();
		if (handlerBlock == null) {
			mth.addDebugComment("Null handler block in: " + handler);
			return;
		}
		BitSet domFrontier = handlerBlock.getDomFrontier();
		if (domFrontier == null) {
			mth.addDebugComment("Null dom frontier in handler: " + handler);
			return;
		}
		set.or(domFrontier);
	}

	public static BlockNode getPathCross(MethodNode mth, BlockNode b1, BlockNode b2) {
		if (b1 == b2) {
			return b1;
		}
		if (b1 == null || b2 == null) {
			return null;
		}
		return getPathCross(mth, Arrays.asList(b1, b2));
	}

	/**
	 * Collect all block dominated by 'dominator', starting from 'start'
	 */
	public static List<BlockNode> collectBlocksDominatedBy(MethodNode mth, BlockNode dominator, BlockNode start) {
		List<BlockNode> result = new ArrayList<>();
		collectWhileDominates(dominator, start, result, newBlocksBitSet(mth), false);
		return result;
	}

	/**
	 * Collect all block dominated by 'dominator', starting from 'start', including exception handlers
	 */
	public static Set<BlockNode> collectBlocksDominatedByWithExcHandlers(MethodNode mth, BlockNode dominator, BlockNode start) {
		Set<BlockNode> result = new LinkedHashSet<>();
		collectWhileDominates(dominator, start, result, newBlocksBitSet(mth), true);
		return result;
	}

	private static void collectWhileDominates(BlockNode dominator, BlockNode child, Collection<BlockNode> result,
			BitSet visited, boolean includeExcHandlers) {
		if (visited.get(child.getId())) {
			return;
		}
		visited.set(child.getId());
		List<BlockNode> successors = includeExcHandlers ? child.getSuccessors() : child.getCleanSuccessors();
		for (BlockNode node : successors) {
			if (node.isDominator(dominator)) {
				result.add(node);
				collectWhileDominates(dominator, node, result, visited, includeExcHandlers);
			}
		}
	}

	/**
	 * Visit blocks on path without branching or merging paths.
	 */
	public static void visitSinglePath(BlockNode startBlock, Consumer<BlockNode> visitor) {
		if (startBlock == null) {
			return;
		}
		visitor.accept(startBlock);
		BlockNode next = getNextSinglePathBlock(startBlock);
		while (next != null) {
			visitor.accept(next);
			next = getNextSinglePathBlock(next);
		}
	}

	@Nullable
	public static BlockNode getNextSinglePathBlock(BlockNode block) {
		if (block == null || block.getPredecessors().size() > 1) {
			return null;
		}
		List<BlockNode> successors = block.getSuccessors();
		return successors.size() == 1 ? successors.get(0) : null;
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
					&& !pred.contains(AFlag.EXC_TOP_SPLITTER)
					&& !pred.contains(AFlag.EXC_BOTTOM_SPLITTER)
					&& pred.getInstructions().isEmpty()) {
				pred.add(AFlag.DONT_GENERATE);
				skipPredSyntheticPaths(pred);
			}
		}
	}

	/**
	 * Follow empty blocks and return end of path block (first not empty).
	 * Return start block if no such path.
	 */
	public static BlockNode followEmptyPath(BlockNode start) {
		while (true) {
			BlockNode next = getNextBlockOnEmptyPath(start);
			if (next == null) {
				return start;
			}
			start = next;
		}
	}

	public static void visitBlocksOnEmptyPath(BlockNode start, Consumer<BlockNode> visitor) {
		while (true) {
			BlockNode next = getNextBlockOnEmptyPath(start);
			if (next == null) {
				return;
			}
			visitor.accept(next);
			start = next;
		}
	}

	@Nullable
	private static BlockNode getNextBlockOnEmptyPath(BlockNode block) {
		if (!block.getInstructions().isEmpty() || block.getPredecessors().size() > 1) {
			return null;
		}
		List<BlockNode> successors = block.getCleanSuccessors();
		if (successors.size() != 1) {
			return null;
		}
		return successors.get(0);
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
	 * Return predecessor of synthetic block or same block otherwise.
	 */
	public static BlockNode skipSyntheticPredecessor(BlockNode block) {
		if (block.isSynthetic()
				&& block.getInstructions().isEmpty()
				&& block.getPredecessors().size() == 1) {
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

	/**
	 * Return limited number of instructions from method.
	 * Return empty list if method contains more than limit.
	 */
	public static List<InsnNode> collectInsnsWithLimit(List<BlockNode> blocks, int limit) {
		List<InsnNode> insns = new ArrayList<>(limit);
		for (BlockNode block : blocks) {
			List<InsnNode> blockInsns = block.getInstructions();
			int blockSize = blockInsns.size();
			if (blockSize == 0) {
				continue;
			}
			if (insns.size() + blockSize > limit) {
				return Collections.emptyList();
			}
			insns.addAll(blockInsns);
		}
		return insns;
	}

	/**
	 * Return insn if it is only one instruction in this method. Return null otherwise.
	 */
	@Nullable
	public static InsnNode getOnlyOneInsnFromMth(MethodNode mth) {
		if (mth.isNoCode()) {
			return null;
		}
		InsnNode insn = null;
		for (BlockNode block : mth.getBasicBlocks()) {
			List<InsnNode> blockInsns = block.getInstructions();
			int blockSize = blockInsns.size();
			if (blockSize == 0) {
				continue;
			}
			if (blockSize > 1) {
				return null;
			}
			if (insn != null) {
				return null;
			}
			insn = blockInsns.get(0);
		}
		return insn;
	}

	public static boolean isFirstInsn(MethodNode mth, InsnNode insn) {
		BlockNode startBlock = followEmptyPath(mth.getEnterBlock());
		if (startBlock != null && !startBlock.getInstructions().isEmpty()) {
			return startBlock.getInstructions().get(0) == insn;
		}
		// handle branching with empty blocks
		BlockNode block = getBlockByInsn(mth, insn);
		if (block == null) {
			throw new JadxRuntimeException("Insn not found in method: " + insn);
		}
		if (block.getInstructions().get(0) != insn) {
			return false;
		}
		Set<BlockNode> allPathsBlocks = getAllPathsBlocks(mth.getEnterBlock(), block);
		for (BlockNode pathBlock : allPathsBlocks) {
			if (!pathBlock.getInstructions().isEmpty() && pathBlock != block) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Replace insn by index i in block,
	 * for proper copy attributes, assume attributes are not overlap
	 */
	public static void replaceInsn(MethodNode mth, BlockNode block, int i, InsnNode insn) {
		InsnNode prevInsn = block.getInstructions().get(i);
		insn.copyAttributesFrom(prevInsn);
		insn.inheritMetadata(prevInsn);
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
		return calcPartialPostDominance(mth, mth.getBasicBlocks(), mth.getPreExitBlocks().get(0));
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

	public static BlockNode getTopSplitterForHandler(BlockNode handlerBlock) {
		BlockNode block = getBlockWithFlag(handlerBlock.getPredecessors(), AFlag.EXC_TOP_SPLITTER);
		if (block == null) {
			throw new JadxRuntimeException("Can't find top splitter block for handler:" + handlerBlock);
		}
		return block;
	}

	@Nullable
	public static BlockNode getBlockWithFlag(List<BlockNode> blocks, AFlag flag) {
		for (BlockNode block : blocks) {
			if (block.contains(flag)) {
				return block;
			}
		}
		return null;
	}

	public static @Nullable CatchAttr getCatchAttrForInsn(MethodNode mth, InsnNode insn) {
		CatchAttr catchAttr = insn.get(AType.EXC_CATCH);
		if (catchAttr != null) {
			return catchAttr;
		}
		BlockNode block = getBlockByInsn(mth, insn);
		if (block == null) {
			return null;
		}
		return block.get(AType.EXC_CATCH);
	}
}
