package jadx.core.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
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
import jadx.core.dex.nodes.Edge;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.conditions.IfCondition;
import jadx.core.dex.trycatch.CatchAttr;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.utils.blocks.BlockSet;
import jadx.core.utils.blocks.DFSIteration;
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

	public static @Nullable BlockNode getBlockByInsn(MethodNode mth, @Nullable InsnNode insn) {
		return getBlockByInsn(mth, insn, mth.getBasicBlocks());
	}

	public static @Nullable BlockNode getBlockByInsn(MethodNode mth, @Nullable InsnNode insn, List<BlockNode> blocks) {
		if (insn == null) {
			return null;
		}
		if (insn instanceof PhiInsn) {
			return searchBlockWithPhi(mth, (PhiInsn) insn);
		}
		if (insn.contains(AFlag.WRAPPED)) {
			return getBlockByWrappedInsn(mth, insn);
		}
		for (BlockNode bn : blocks) {
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
	 * Return predecessor on path from 'pathStart' block
	 */
	public static @Nullable BlockNode getPrevBlockOnPath(MethodNode mth, BlockNode block, BlockNode pathStart) {
		BlockSet preds = BlockSet.from(mth, block.getPredecessors());
		if (preds.contains(pathStart)) {
			return pathStart;
		}
		DFSIteration dfs = new DFSIteration(mth, pathStart, BlockNode::getCleanSuccessors);
		while (true) {
			BlockNode next = dfs.next();
			if (next == null) {
				return null;
			}
			if (preds.contains(next)) {
				return next;
			}
		}
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

	public static List<BlockNode> collectAllPredecessors(MethodNode mth, BlockNode startBlock) {
		List<BlockNode> list = new ArrayList<>(mth.getBasicBlocks().size());
		Function<BlockNode, List<BlockNode>> nextFunc = BlockNode::getPredecessors;
		visitDFS(mth, startBlock, nextFunc, list::add);
		return list;
	}

	public static List<BlockNode> collectAllSuccessors(MethodNode mth, BlockNode startBlock, boolean clean) {
		List<BlockNode> list = new ArrayList<>(mth.getBasicBlocks().size());
		Function<BlockNode, List<BlockNode>> nextFunc = clean ? BlockNode::getCleanSuccessors : BlockNode::getSuccessors;
		visitDFS(mth, startBlock, nextFunc, list::add);
		return list;
	}

	public static List<BlockNode> collectAllSuccessorsUntil(MethodNode mth, BlockNode startBlock, boolean clean,
			Predicate<BlockNode> stopCondition) {
		List<BlockNode> blocks = new ArrayList<>();
		collectAllSuccessorsUntil(mth, blocks, startBlock, clean, stopCondition);
		return blocks;
	}

	private static void collectAllSuccessorsUntil(MethodNode mth, List<BlockNode> blocks, BlockNode currentBlock, boolean clean,
			Predicate<BlockNode> stopCondition) {
		if (blocks.contains(currentBlock)) {
			return;
		}

		blocks.add(currentBlock);

		if (stopCondition.test(currentBlock)) {
			return;
		}

		List<BlockNode> successors = clean ? currentBlock.getCleanSuccessors() : currentBlock.getSuccessors();
		for (BlockNode successor : successors) {
			collectAllSuccessorsUntil(mth, blocks, successor, clean, stopCondition);
		}
	}

	@Nullable
	public static BlockNode getBottomCommonPredecessor(MethodNode mth, List<BlockNode> blocks, Set<BlockNode> containedBlocks) {
		return getBottomCommonPredecessor(mth, blocks, containedBlocks, false);
	}

	@Nullable
	public static BlockNode getBottomCommonPredecessor(MethodNode mth, List<BlockNode> blocks, Set<BlockNode> containedBlocks,
			boolean addTopBlock) {
		if (blocks.isEmpty()) {
			return null;
		}

		Set<BlockNode> visitedPredecessorsByAll = new HashSet<>(collectAllPredecessors(mth, blocks.get(0)));

		if (addTopBlock) {
			BlockNode topBlock = BlockUtils.getBottomBlock(blocks);
			if (topBlock == null) {
				// TODO: These nodes are not connected so there will be no common successor ????
				// return null;
			} else {
				visitedPredecessorsByAll.add(topBlock);
			}
		}

		for (int i = 1; i < blocks.size(); i++) {
			BlockNode nextBlock = blocks.get(i);
			List<BlockNode> predecessors = collectAllPredecessors(mth, nextBlock);
			visitedPredecessorsByAll.retainAll(predecessors);
		}

		return BlockUtils.getBottomBlock(new ArrayList<>(visitedPredecessorsByAll));
	}

	@Nullable
	public static BlockNode getTopCommonSuccessor(MethodNode mth, List<BlockNode> blocks, boolean cleanOnly) {
		return getTopCommonSuccessor(mth, blocks, cleanOnly, false);
	}

	@Nullable
	public static BlockNode getTopCommonSuccessor(MethodNode mth, List<BlockNode> blocks, boolean cleanOnly, boolean addTopBlock) {
		if (blocks.isEmpty()) {
			return null;
		}

		Set<BlockNode> visitedSuccessorsByAll = new HashSet<>(collectAllSuccessors(mth, blocks.get(0), cleanOnly));

		if (addTopBlock) {
			BlockNode topBlock = BlockUtils.getTopBlock(blocks);
			if (topBlock == null) {
				// TODO: These nodes are not connected so there will be no common successor ????
				// return null;
			} else {
				visitedSuccessorsByAll.add(topBlock);
			}
		}

		for (int i = 1; i < blocks.size(); i++) {
			BlockNode nextBlock = blocks.get(i);
			List<BlockNode> successors = collectAllSuccessors(mth, nextBlock, cleanOnly);
			visitedSuccessorsByAll.retainAll(successors);
		}

		return BlockUtils.getTopBlock(new ArrayList<>(visitedSuccessorsByAll));
	}

	public static void visitDFS(MethodNode mth, Consumer<BlockNode> visitor) {
		visitDFS(mth, mth.getEnterBlock(), BlockNode::getSuccessors, visitor);
	}

	public static void visitReverseDFS(MethodNode mth, Consumer<BlockNode> visitor) {
		visitDFS(mth, mth.getExitBlock(), BlockNode::getPredecessors, visitor);
	}

	private static void visitDFS(MethodNode mth, BlockNode startBlock,
			Function<BlockNode, List<BlockNode>> nextFunc, Consumer<BlockNode> visitor) {
		DFSIteration dfsIteration = new DFSIteration(mth, startBlock, nextFunc);
		while (true) {
			BlockNode next = dfsIteration.next();
			if (next == null) {
				return;
			}
			visitor.accept(next);
		}
	}

	public static List<BlockNode> collectPredecessors(MethodNode mth, BlockNode start, Collection<BlockNode> stopBlocks) {
		BitSet bs = newBlocksBitSet(mth);
		if (!stopBlocks.isEmpty()) {
			bs.or(blocksToBitSet(mth, stopBlocks));
		}
		List<BlockNode> list = new ArrayList<>();
		traversePredecessors(start, bs, block -> {
			list.add(block);
			return false;
		});
		return list;
	}

	public static void visitPredecessorsUntil(MethodNode mth, BlockNode start, Predicate<BlockNode> visitor) {
		traversePredecessors(start, newBlocksBitSet(mth), visitor);
	}

	/**
	 * Up BFS.
	 * To stop return true from predicate
	 */
	private static void traversePredecessors(BlockNode start, BitSet visited, Predicate<BlockNode> visitor) {
		Queue<BlockNode> queue = new ArrayDeque<>();
		queue.add(start);
		while (true) {
			BlockNode current = queue.poll();
			if (current == null || visitor.test(current)) {
				return;
			}
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

	/**
	 * Collect blocks from one possible execution path from 'start' to 'end' containing no instructions
	 */
	public static List<BlockNode> getOneEmptyPath(BlockNode start, BlockNode end) {
		return collectPathUntil(start, end, false, b -> {
			return b.getInstructions().isEmpty() || b.equals(end);
		});
	}

	/**
	 * Collect blocks from one possible execution path from 'start' to 'end'
	 */
	public static List<BlockNode> getOnePath(BlockNode start, BlockNode end) {
		return collectPathUntil(start, end, false, b -> true);
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
		return traverseSuccessorsUntil(from, until, visited, clean, b -> true);
	}

	/**
	 *
	 * Traverse succcessors until a node is found
	 *
	 * @param from    the source node to begin traversing
	 * @param until   the destination node to halt traversing
	 * @param visited the set of visited blocks so far
	 * @param clean   use only clean successors
	 * @param pred    a predicate that must be true to traverse a block (until or a reachable dominator
	 *                of until must satisfy pred)
	 * @return true if there is a path from `from` to `until` or a dominator of `until` through blocks
	 *         that satisfy `pred`, false otherwise
	 */
	private static boolean traverseSuccessorsUntil(BlockNode from, BlockNode until, BitSet visited, boolean clean,
			Predicate<BlockNode> pred) {
		List<BlockNode> nodes = clean ? from.getCleanSuccessors() : from.getSuccessors();
		for (BlockNode s : nodes) {
			if (!pred.test(s)) {
				// Only explore blocks such that the predicate holds
				continue;
			}
			if (s == until) {
				return true;
			}
			if (s == from) {
				// ignore possible block self loop
				continue;
			}
			int id = s.getPos();
			if (!visited.get(id)) {
				visited.set(id);
				if (until.isDominator(s)) {
					return true;
				}
				if (traverseSuccessorsUntil(s, until, visited, clean, pred)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 *
	 * Traverse succcessors until a node is found, collecting the path to the node
	 *
	 * @param from  the source node to begin traversing
	 * @param until the destination node to halt traversing
	 * @param clean use only clean successors
	 * @param pred  a predicate that must be true to traverse a block (until must satisfy pred)
	 * @return the list of blocks satisfying pred on a path between from and until (inclusive), or null
	 *         if no such path exists
	 */
	public static List<BlockNode> collectPathUntil(BlockNode from, BlockNode until, boolean clean, Predicate<BlockNode> pred) {
		List<BlockNode> path = internalCollectPathUntil(from, until, new BitSet(), clean, pred);
		if (path == null) {
			return path;
		}
		path.add(from);
		Collections.reverse(path);
		return path;
	}

	/**
	 *
	 * Traverse succcessors until a node is found, collecting the path to the node
	 *
	 * @param from    the source node to begin traversing
	 * @param until   the destination node to halt traversing
	 * @param visited the set of visited blocks so far
	 * @param clean   use only clean successors
	 * @param pred    a predicate that must be true to traverse a block (until must satisfy pred)
	 * @return the list of blocks satisfying pred on a path between from (exclusive) and until
	 *         (inclusive) in reverse order, or null if no such path exists
	 */
	private static List<BlockNode> internalCollectPathUntil(BlockNode from, BlockNode until, BitSet visited, boolean clean,
			Predicate<BlockNode> pred) {
		List<BlockNode> nodes = clean ? from.getCleanSuccessors() : from.getSuccessors();
		for (BlockNode s : nodes) {
			if (!pred.test(s)) {
				// Only explore blocks such that the predicate holds
				continue;
			}
			if (s == until) {
				List<BlockNode> path = new ArrayList<>();
				path.add(s);
				return path;
			}
			int id = s.getPos();
			if (!visited.get(id)) {
				visited.set(id);
				List<BlockNode> path = internalCollectPathUntil(s, until, visited, clean, pred);
				if (path != null) {
					path.add(s);
					return path;
				}
			}
		}
		return null;
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
				|| start.getCleanSuccessors().contains(end)) {
			return true;
		}
		return traverseSuccessorsUntil(start, end, new BitSet(), true);
	}

	public static boolean isAnyPathExists(BlockNode start, BlockNode end) {
		if (start == end
				|| end.isDominator(start)
				|| start.getSuccessors().contains(end)) {
			return true;
		}
		return traverseSuccessorsUntil(start, end, new BitSet(), false);
	}

	public static boolean isPathExists(BlockNode start, BlockNode end, Predicate<BlockNode> pred) {
		if (start == end) {
			return true;
		}
		return traverseSuccessorsUntil(start, end, new BitSet(), false, pred);
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
		return getBottomBlock(blocks, false);
	}

	public static BlockNode getBottomBlock(List<BlockNode> blocks, boolean clean) {
		if (blocks.size() == 1) {
			return blocks.get(0);
		}
		// attempt 1: look for a block dominated by every other block
		// don't do this if clean, since dominators always consider all successors
		if (!clean) {
			for (BlockNode bottomCandidate : blocks) {
				boolean bottom = true;
				for (BlockNode from : blocks) {
					if (bottomCandidate != from && !bottomCandidate.isDominator(from)) {
						bottom = false;
						break;
					}
				}
				if (bottom) {
					return bottomCandidate;
				}
			}
		}

		// attempt 2: look for a block with a path from every other block
		for (BlockNode bottomCandidate : blocks) {
			boolean bottom = true;
			for (BlockNode from : blocks) {
				if (clean) {
					if (bottomCandidate != from && !isPathExists(from, bottomCandidate)) {
						bottom = false;
						break;
					}
				} else {
					if (bottomCandidate != from && !isAnyPathExists(from, bottomCandidate)) {
						bottom = false;
						break;
					}
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
	 * Return the dominace frontier of an edge - the blocks for which any path to the block must pass
	 * through this edge
	 */
	public static BitSet getDomFrontierThroughEdge(Edge edge) {
		BlockNode target = edge.getTarget();

		if (target.getPredecessors().size() > 1) {
			// If the target node has other incoming edges, the dominance frontier is a single block
			BitSet dominanceFrontier = new BitSet();
			dominanceFrontier.set(target.getPos());
			return dominanceFrontier;
		} else {
			// Otherwise the dominance frontier is equivalent to the domiance frontier of the target
			return target.getDomFrontier();
		}
	}

	/**
	 * Return common cross block for input set.
	 *
	 * @return could be one of the giving blocks. null if cross is a method exit block.
	 */
	@Nullable
	public static BlockNode getPathCross(MethodNode mth, Collection<BlockNode> blocks) {
		BitSet domFrontBS = newBlocksBitSet(mth);
		BitSet tmpBS = newBlocksBitSet(mth); // store block itself and its domFrontier
		boolean first = true;
		for (BlockNode b : blocks) {
			tmpBS.clear();
			tmpBS.set(b.getId());
			tmpBS.or(b.getDomFrontier());
			if (first) {
				domFrontBS.or(tmpBS);
				first = false;
			} else {
				domFrontBS.and(tmpBS);
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
			mth.getExceptionHandlers().forEach(h -> addExcHandler(mth, h, excluded));
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

	private static void addExcHandler(MethodNode mth, ExceptionHandler handler, BitSet set) {
		BlockNode handlerBlock = handler.getHandlerBlock();
		if (handlerBlock == null) {
			mth.addDebugComment("Null handler block in: " + handler);
			return;
		}
		set.set(handlerBlock.getId());
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
		return followEmptyPath(start, false);
	}

	public static BlockNode followEmptyPath(BlockNode start, Boolean reverse) {
		return followEmptyPath(start, reverse, true);
	}

	public static BlockNode followEmptyPath(BlockNode start, Boolean reverse, boolean cleanOnly) {
		while (true) {
			BlockNode next = getNextBlockOnEmptyPath(start, reverse, cleanOnly);
			if (next == null) {
				return start;
			}
			start = next;
		}
	}

	public static List<BlockNode> followEmptyUpPathWithinSet(BlockNode start, Collection<BlockNode> traversableBlocks) {
		List<BlockNode> results = new LinkedList<>();
		followEmptyUpPathWithinSet(results, start, traversableBlocks, new HashSet<>());
		return results;
	}

	public static void followEmptyUpPathWithinSet(List<BlockNode> results, BlockNode start, Collection<BlockNode> traversableBlocks,
			Collection<BlockNode> traversedBlocks) {
		List<BlockNode> predecessors = ListUtils.filter(start.getPredecessors(), traversableBlocks::contains);
		for (BlockNode predecessor : predecessors) {
			if (!traversableBlocks.contains(predecessor) || traversedBlocks.contains(predecessor)) {
				continue;
			}
			traversedBlocks.add(predecessor);
			if (predecessor.getInstructions().isEmpty()) {
				followEmptyUpPathWithinSet(results, start, traversableBlocks, traversedBlocks);
			} else {
				results.add(predecessor);
			}
			start = predecessor;
		}
	}

	public static void visitBlocksOnEmptyPath(BlockNode start, Consumer<BlockNode> visitor) {
		visitBlocksOnEmptyPath(start, visitor, false);
	}

	public static void visitBlocksOnEmptyPath(BlockNode start, Consumer<BlockNode> visitor, boolean reverse) {
		while (true) {
			BlockNode next = getNextBlockOnEmptyPath(start, reverse);
			if (next == null) {
				return;
			}
			visitor.accept(next);
			start = next;
		}
	}

	@Nullable
	private static BlockNode getNextBlockOnEmptyPath(BlockNode block) {
		return getNextBlockOnEmptyPath(block, false);
	}

	@Nullable
	private static BlockNode getNextBlockOnEmptyPath(BlockNode block, Boolean reverse) {
		return getNextBlockOnEmptyPath(block, reverse, true);
	}

	@Nullable
	private static BlockNode getNextBlockOnEmptyPath(BlockNode block, Boolean reverse, boolean cleanOnly) {
		if (!block.getInstructions().isEmpty() || (!reverse && block.getPredecessors().size() > 1)
				|| (reverse && block.getCleanSuccessors().size() > 1)) {
			return null;
		}
		List<BlockNode> nextBlocks = reverse ? block.getPredecessors() : (cleanOnly ? block.getCleanSuccessors() : block.getSuccessors());
		if (nextBlocks.size() != 1) {
			return null;
		}
		return nextBlocks.get(0);
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
		if (Utils.isEmpty(blocks)) {
			return true;
		}
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

	public static void removeInstructions(List<IBlock> blocks) {
		for (IBlock block : blocks) {
			block.getInstructions().clear();
		}
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

	public static BlockNode getTopSplitterForHandler(BlockNode handlerBlock) {
		BlockNode block = getBlockWithFlag(handlerBlock.getPredecessors(), AFlag.EXC_TOP_SPLITTER);
		if (block == null) {
			throw new JadxRuntimeException("Can't find top splitter block for handler:" + handlerBlock);
		}
		return block;
	}

	/**
	 * Return out block of try catch, by finding where try branch meets catch branch.
	 * It traverse domFrontier start from handler block, find the first frontier
	 * whose predecessor is try end.
	 * <br>
	 * It could return null if they never meets, but this doesn't mean that catch
	 * ends at the method exit.
	 * (see TestSwitchWithTryCatch and ExcHandlersRegionMaker#processExcHandler).
	 */
	@Nullable
	public static BlockNode getTryAndHandlerCrossBlock(MethodNode mth, ExceptionHandler handler) {
		BlockNode start = handler.getHandlerBlock();
		BlockNode topSplitter = BlockUtils.getTopSplitterForHandler(start);
		List<ExceptionHandler> allHandlers = handler.getTryBlock().getHandlers();
		List<BlockNode> handlerExitsCandidate = new ArrayList<>(BlockUtils.bitSetToBlocks(mth, start.getDomFrontier()));
		BitSet visited = newBlocksBitSet(mth);
		while (!handlerExitsCandidate.isEmpty()) {
			BlockNode frontier = handlerExitsCandidate.remove(0);
			if (visited.get(frontier.getPos())) {
				continue;
			}
			visited.set(frontier.getPos());
			// In some cases, handler's domFrontier is in the half of catch block
			// instead of the end, so we need to make sure frontier's predecessor
			// comes from try branch end:
			// 1. not from handler branch, doesn't exist path from handler to pred
			// 2. from try branch, exists path from topSplitter to pred
			// 3. skip method exit
			for (BlockNode pred : frontier.getPredecessors()) {
				boolean predFromHandler = allHandlers.stream().anyMatch(h -> isPathExists(h.getHandlerBlock(), pred));
				if (!predFromHandler && BlockUtils.isPathExists(topSplitter, pred)
						&& frontier != mth.getExitBlock()) {
					return frontier;
				}
			}
			// if not found, add this frontier's frontier to candidate list
			handlerExitsCandidate.addAll(BlockUtils.bitSetToBlocks(mth, frontier.getDomFrontier()));
		}
		return null;
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

	public static boolean isEqualPaths(BlockNode b1, BlockNode b2) {
		if (b1 == b2) {
			return true;
		}
		if (b1 == null || b2 == null) {
			return false;
		}
		return isEqualReturnBlocks(b1, b2) || isEmptySyntheticPath(b1, b2) || isDuplicateBlockPath(b1, b2);
	}

	private static boolean isEmptySyntheticPath(BlockNode b1, BlockNode b2) {
		BlockNode n1 = followEmptyPath(b1);
		BlockNode n2 = followEmptyPath(b2);
		return n1 == n2 || isEqualReturnBlocks(n1, n2);
	}

	public static boolean isEqualReturnBlocks(BlockNode b1, BlockNode b2) {
		if (!b1.isReturnBlock() || !b2.isReturnBlock()) {
			return false;
		}
		List<InsnNode> b1Insns = b1.getInstructions();
		List<InsnNode> b2Insns = b2.getInstructions();
		if (b1Insns.size() != 1 || b2Insns.size() != 1) {
			return false;
		}
		InsnNode i1 = b1Insns.get(0);
		InsnNode i2 = b2Insns.get(0);
		if (i1.getArgsCount() != i2.getArgsCount()) {
			return false;
		}
		if (i1.getArgsCount() == 0) {
			return true;
		}
		InsnArg firstArg = i1.getArg(0);
		InsnArg secondArg = i2.getArg(0);
		if (firstArg.isSameConst(secondArg)) {
			return true;
		}
		if (i1.getSourceLine() != i2.getSourceLine()) {
			return false;
		}
		return firstArg.equals(secondArg);
	}

	public static boolean isDuplicateBlockPath(BlockNode first, BlockNode second) {
		if (first.getSuccessors().size() == 1 && second.getSuccessors().size() == 1
				&& first.getSuccessors().get(0).equals(second.getSuccessors().get(0))) {
			return isSameInsnsBlocks(first, second);
		}
		return false;
	}

	public static boolean isSameInsnsBlocks(BlockNode first, BlockNode second) {
		List<InsnNode> firstInsns = first.getInstructions();
		List<InsnNode> secondInsns = second.getInstructions();
		if (firstInsns.size() != secondInsns.size()) {
			return false;
		}
		int len = firstInsns.size();
		for (int i = 0; i < len; i++) {
			InsnNode firstInsn = firstInsns.get(i);
			InsnNode secondInsn = secondInsns.get(i);
			if (!isInsnDeepEquals(firstInsn, secondInsn)) {
				return false;
			}
		}
		return true;
	}

	private static boolean isInsnDeepEquals(InsnNode first, InsnNode second) {
		if (first == second) {
			return true;
		}
		return first.isSame(second)
				&& Objects.equals(first.getArguments(), second.getArguments())
				&& resultIsSameReg(first.getResult(), second.getResult());
	}

	private static boolean resultIsSameReg(RegisterArg first, RegisterArg second) {
		if (first == null || second == null) {
			return first == second;
		}
		return first.getRegNum() == second.getRegNum();
	}
}
