package jadx.core.dex.trycatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import jadx.api.plugins.input.data.attributes.IJadxAttrType;
import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.LoopInfo;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.Edge;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.ListUtils;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class TryCatchBlockAttr implements IJadxAttribute {

	public static boolean isImplicitOrMerged(TryCatchBlockAttr tryBlock) {
		return tryBlock.isMerged() || tryBlock.getHandlers().isEmpty();
	}

	private final int id;
	private final List<ExceptionHandler> handlers;
	private List<BlockNode> blocks;

	private TryCatchBlockAttr outerTryBlock;
	private List<TryCatchBlockAttr> innerTryBlocks = Collections.emptyList();
	private boolean merged = false;

	private BlockNode topSplitter;

	public TryCatchBlockAttr(int id, List<ExceptionHandler> handlers, List<BlockNode> blocks) {
		this.id = id;
		this.handlers = handlers;
		this.blocks = blocks;

		handlers.forEach(h -> h.setTryBlock(this));
	}

	public boolean isAllHandler() {
		return handlers.size() == 1 && handlers.get(0).isCatchAll();
	}

	public boolean isThrowOnly() {
		boolean throwFound = false;
		for (BlockNode block : blocks) {
			List<InsnNode> insns = block.getInstructions();
			if (insns.size() != 1) {
				return false;
			}
			InsnNode insn = insns.get(0);
			switch (insn.getType()) {
				case MOVE_EXCEPTION:
				case MONITOR_EXIT:
					// allowed instructions
					break;

				case THROW:
					throwFound = true;
					break;

				default:
					return false;
			}
		}
		return throwFound;
	}

	public int getId() {
		return id;
	}

	public List<ExceptionHandler> getHandlers() {
		return handlers;
	}

	public int getHandlersCount() {
		return handlers.size();
	}

	public List<BlockNode> getBlocks() {
		return blocks;
	}

	public void setBlocks(List<BlockNode> blocks) {
		this.blocks = blocks;
	}

	public void clear() {
		blocks.clear();
		handlers.forEach(ExceptionHandler::markForRemove);
		handlers.clear();
	}

	public void removeBlock(BlockNode block) {
		blocks.remove(block);
	}

	public void removeHandler(ExceptionHandler handler) {
		handlers.remove(handler);
		handler.markForRemove();
	}

	public List<TryCatchBlockAttr> getInnerTryBlocks() {
		return innerTryBlocks;
	}

	public void addInnerTryBlock(TryCatchBlockAttr inner) {
		if (this.innerTryBlocks.isEmpty()) {
			this.innerTryBlocks = new ArrayList<>();
		}
		this.innerTryBlocks.add(inner);
	}

	public TryCatchBlockAttr getOuterTryBlock() {
		return outerTryBlock;
	}

	public void setOuterTryBlock(TryCatchBlockAttr outerTryBlock) {
		this.outerTryBlock = outerTryBlock;
	}

	public BlockNode getTopSplitter() {
		return topSplitter;
	}

	public void setTopSplitter(BlockNode topSplitter) {
		this.topSplitter = topSplitter;
	}

	public boolean isMerged() {
		return merged;
	}

	public void setMerged(boolean merged) {
		this.merged = merged;
	}

	public int id() {
		return id;
	}

	public List<TryEdge> getHandlerTryEdges() {
		List<ExceptionHandler> mergedHandlers = getMergedHandlers();
		List<TryEdge> edges = new ArrayList<>(mergedHandlers.size());
		for (ExceptionHandler handler : mergedHandlers) {
			BlockNode handlerBlock = handler.getHandlerBlock();
			BlockNode handlerSplitter = handler.getBottomSplitter();
			if (handlerSplitter == null) {
				// If we cannot find a bottom splitter, there might be none. In this case, assume that the top
				// splitter of this try catch is the source of the exit.
				List<BlockNode> allChildren = ListUtils.filter(handlerBlock.getPredecessors(), blk -> getBlocks().contains(blk));
				handlerSplitter = BlockUtils.getBottomBlock(allChildren);
				if (handlerSplitter == null) {
					handlerSplitter = getTopSplitter();
				}
			}
			TryEdge edge = new TryEdge(handlerSplitter, handlerBlock, handler);
			edges.add(edge);
		}
		return edges;
	}

	public List<TryEdge> getFallthroughTryEdges() {
		List<TryEdge> edges = new LinkedList<>();
		List<BlockNode> exploredBlocks = new ArrayList<>();
		List<TryCatchBlockAttr> exploredTrys = new LinkedList<>();

		getFallthroughTryEdges(edges, exploredBlocks, exploredTrys);
		return edges;
	}

	public void getFallthroughTryEdges(List<TryEdge> edges, List<BlockNode> exploredBlocks, List<TryCatchBlockAttr> exploredTrys) {
		List<ExceptionHandler> mergedHandlers = getMergedHandlers();
		Set<BlockNode> searchBlocks = new HashSet<>();
		searchBlocks.addAll(getBlocks());
		for (ExceptionHandler handler : mergedHandlers) {
			searchBlocks.removeAll(handler.getBlocks());
		}

		BlockNode sourceBlock = BlockUtils.getTopBlock(new ArrayList<>(searchBlocks));

		exploredTrys.add(this);

		exploreTryPath(edges, sourceBlock, searchBlocks, exploredBlocks, exploredTrys);
	}

	public List<TryEdge> getTryEdges() {
		List<TryEdge> handlerEdges = getHandlerTryEdges();
		List<TryEdge> fallthroughEdges = getFallthroughTryEdges();
		List<TryEdge> edges = new ArrayList<>(handlerEdges.size() + fallthroughEdges.size());
		edges.addAll(handlerEdges);
		edges.addAll(fallthroughEdges);
		return Collections.unmodifiableList(edges);
	}

	private void exploreTryPath(List<TryEdge> edges, BlockNode blk, Set<BlockNode> searchBlocks, List<BlockNode> exploredBlocks,
			List<TryCatchBlockAttr> exploredTrys) {
		for (BlockNode successor : blk.getSuccessors()) {
			// If a separate branch has already explored this block, we don't need to recalculate its exits.
			if (exploredBlocks.contains(successor)) {
				continue;
			}

			// If this is a bottom splitter, ignore - we only care about non-handler edges.
			if (successor.contains(AFlag.EXC_BOTTOM_SPLITTER)) {
				continue;
			}

			exploredBlocks.add(successor);

			if (successor.contains(AFlag.LOOP_END)) {
				final var loopsAttrList = successor.get(AType.LOOP);
				final List<LoopInfo> loops = loopsAttrList.getList();
				final List<BlockNode> loopStartBlocks = new LinkedList<>();
				for (final LoopInfo loop : loops) {
					loopStartBlocks.add(loop.getStart());
					final List<Edge> loopEdges = loop.getExitEdges();
					for (final Edge loopEdge : loopEdges) {
						if (loopEdge.getTarget() == successor) {
							loopStartBlocks.add(loopEdge.getSource());
						}
					}
				}
				final boolean includesAllLoopStart = ListUtils.allMatch(loopStartBlocks, exploredBlocks::contains);
				if (!includesAllLoopStart) {
					edges.add(new TryEdge(blk, successor, TryEdgeType.LOOP_EXIT));
					continue;
				}
			}

			boolean isPathToAnySearchBlock = false;
			for (final BlockNode searchBlock : searchBlocks) {
				if (BlockUtils.isPathExists(successor, searchBlock)) {
					isPathToAnySearchBlock = true;
					break;
				}
			}
			if (!searchBlocks.contains(successor) && !isPathToAnySearchBlock) {
				// This block is not contained within this try's block list. This can either be since it is an exit
				// to the try or it is a block which leads to an exit (for example, an exception handler).

				// If this block (successor) leads to an exit, then the "bottom block" of all try blocks and this
				// block will be
				// equal to the bottom block of all try blocks. If this block is an exit, then either:
				// - a path does not exist from all try blocks to this block, thus making the bottom block null.
				// - a path does exist from all try blocks to this block but no more try blocks follow, thus making
				// the bottom block this block.
				List<BlockNode> allBlocksWithCurrent = new ArrayList<>(getBlocks().size() + 1);
				allBlocksWithCurrent.addAll(getBlocks());
				allBlocksWithCurrent.add(successor);
				BlockNode bottomBlock = BlockUtils.getBottomBlock(allBlocksWithCurrent);

				if (!(bottomBlock == null || bottomBlock == successor)) {
					// This block leads to an exit.
					exploreTryPath(edges, successor, searchBlocks, exploredBlocks, exploredTrys);
					continue;
				}

				BlockNode emptyPathEndOfSuccessor = BlockUtils.followEmptyPath(successor, false, false);

				if (emptyPathEndOfSuccessor.contains(AFlag.EXC_TOP_SPLITTER)) {
					// This block is an exit which enters another try catch. In this case, the next try catch is within
					// the same scope. Thus, we will take all of the edges out of that try and add them to the list of
					// edges of this try.
					Set<TryCatchBlockAttr> nestedTrys = new HashSet<>();
					List<BlockNode> allSuccessorsOnTryBody = ListUtils.filter(emptyPathEndOfSuccessor.getSuccessors(),
							potentialTryBlock -> potentialTryBlock.contains(AFlag.TRY_ENTER));
					for (BlockNode tryBodyEnter : allSuccessorsOnTryBody) {
						TryCatchBlockAttr nestedTry = tryBodyEnter.get(AType.TRY_BLOCK);
						if (nestedTry == null) {
							continue;
						}

						// If we have already added a try's edges, skip over it to avoid infinite recursion.
						if (exploredTrys.contains(nestedTry)) {
							continue;
						}

						// Unsure of why these top splitters have to be the same for them to be "nested" trys, but this
						// seems to work (?)
						if (nestedTry.getTopSplitter() != getTopSplitter()) {
							continue;
						}

						nestedTrys.add(nestedTry);
					}

					// Only will we attempt to add nested inners if there exists any. If none exist, perform normal
					// handling of the edge.
					if (!nestedTrys.isEmpty()) {
						for (TryCatchBlockAttr nestedTry : nestedTrys) {
							nestedTry.getFallthroughTryEdges(edges, exploredBlocks, exploredTrys);
						}
						continue;
					}
				}

				if (bottomBlock == null) {
					// This block is an exit which occurs before all try blocks are logically executed.
					edges.add(new TryEdge(blk, successor, TryEdgeType.PREMATURE_EXIT));
				} else if (bottomBlock == successor) {
					// This block is an exit which occurs after all try blocks are logically executed.
					edges.add(new TryEdge(blk, successor, TryEdgeType.TRUE_FALLTHROUGH));
				} else {
					// All possible cases should have been caught by the above if / else and the preceeding if.
					// If this is hit, any changes made to this algorithm must aptly handle all possible code paths
					// before executing this.
					throw new JadxRuntimeException(
							"Unexpected code execution branch taken during try edge resolution: blk="
									+ blk + ",successor=" + successor);
				}
			} else {
				exploreTryPath(edges, successor, searchBlocks, exploredBlocks, exploredTrys);
			}
		}
	}

	public List<ExceptionHandler> getMergedHandlers() {
		boolean hasInnerBlocks = !getInnerTryBlocks().isEmpty();
		final List<ExceptionHandler> mergedHandlers;
		if (hasInnerBlocks) {
			// collect handlers from this and all inner blocks
			// (intentionally not using recursive collect for now)
			mergedHandlers = new ArrayList<>(getHandlers());
			for (TryCatchBlockAttr innerTryBlock : getInnerTryBlocks()) {
				mergedHandlers.addAll(innerTryBlock.getHandlers());
			}
		} else {
			mergedHandlers = getHandlers();
		}
		return Collections.unmodifiableList(mergedHandlers);
	}

	public Map<TryEdge, BlockNode> getEdgeBlockMap(MethodNode mth) {
		List<TryEdge> edges = getTryEdges();
		Map<TryEdge, BlockNode> blockMap = new HashMap<>();
		for (TryEdge edge : edges) {
			blockMap.put(edge, edge.getTarget());
		}
		return blockMap;
	}

	public TryEdgeScopeGroupMap getExecutionScopeGroups(MethodNode mth) {
		Map<TryEdge, BlockNode> handlerBlocks = getEdgeBlockMap(mth);
		TryEdgeScopeGroupMap scopeGroups = new TryEdgeScopeGroupMap(mth, this, handlerBlocks.size());
		scopeGroups.populateFromEdges(handlerBlocks);

		return scopeGroups;
	}

	public Map<BlockNode, List<TryEdge>> getHandlerFallthroughGroups(MethodNode mth, TryEdgeScopeGroupMap scopeGroups) {
		return scopeGroups.getScopeEnds(mth);
	}

	public List<BlockNode> getSearchBlocksFromFallthroughGroups(MethodNode mth, ExceptionHandler finallyHandler,
			Map<BlockNode, List<TryEdge>> fallthroughGroups) {

		List<BlockNode> searchBlocks = new LinkedList<>();
		for (Map.Entry<BlockNode, List<TryEdge>> entry : fallthroughGroups.entrySet()) {
			BlockNode scopeEndBlock = entry.getKey();
			List<TryEdge> sourceHandlers = entry.getValue();

			for (BlockNode scopeEndPredecessor : scopeEndBlock.getPredecessors()) {
				// Add all predecessors to the scope end which are connected to some handler's scope start
				try (Stream<TryEdge> stream = sourceHandlers.stream()) {
					Object[] matchedHandlerPaths =
							stream.filter(handler -> !(handler.isHandlerExit() && handler.getExceptionHandler() == finallyHandler))
									.map(handler -> handler.getTarget())
									.filter(scopeStart -> BlockUtils.isPathExists(scopeStart, scopeEndPredecessor))
									.toArray();
					if (matchedHandlerPaths.length != 0) {
						searchBlocks.add(scopeEndPredecessor);
					}
				}
			}
		}
		return searchBlocks;
	}

	@Override
	public IJadxAttrType<? extends IJadxAttribute> getAttrType() {
		return AType.TRY_BLOCK;
	}

	@Override
	public int hashCode() {
		return handlers.hashCode() + 31 * blocks.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		TryCatchBlockAttr other = (TryCatchBlockAttr) obj;
		return id == other.id
				&& handlers.equals(other.handlers)
				&& blocks.equals(other.blocks);
	}

	@Override
	public String toString() {
		if (merged) {
			return "Merged into " + outerTryBlock;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("TryCatch #").append(id).append(" {").append(Utils.listToString(handlers));
		sb.append(", blocks: (").append(Utils.listToString(blocks)).append(')');
		if (topSplitter != null) {
			sb.append(", top: ").append(topSplitter);
		}
		if (outerTryBlock != null) {
			sb.append(", outer: #").append(outerTryBlock.id);
		}
		if (!innerTryBlocks.isEmpty()) {
			sb.append(", inners: ").append(Utils.listToString(innerTryBlocks, inner -> "#" + inner.id));
		}
		sb.append(" }");
		return sb.toString();
	}
}
