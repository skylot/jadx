package jadx.core.dex.visitors.finaly;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.dex.trycatch.TryCatchBlockAttr;
import jadx.core.dex.trycatch.TryEdge;
import jadx.core.dex.trycatch.TryEdgeScopeGroupMap;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.ListUtils;

/**
 * A map containing all edges within a try catch block as the key and all blocks that the
 * respective edge can traverse to within the "scope" of that edge (relative to the
 * entire try / catch).
 */
public final class TryCatchEdgeBlockMap implements Map<TryEdge, List<BlockNode>> {

	public static boolean anyBlockHasNonImplicitTry(List<BlockNode> blocks) {
		List<BlockNode> blocksWithTries = ListUtils.filter(blocks, blk -> blk.contains(AFlag.EXC_TOP_SPLITTER));
		if (blocksWithTries.isEmpty()) {
			return false;
		}
		for (BlockNode topSplitter : blocksWithTries) {
			TryCatchBlockAttr block = null;
			for (BlockNode topSplitterSuccessor : topSplitter.getCleanSuccessors()) {
				if (topSplitterSuccessor.contains(AType.TRY_BLOCK)) {
					block = topSplitterSuccessor.get(AType.TRY_BLOCK);
				}
			}
			if (block == null) {
				continue;
			}
			if (!TryCatchBlockAttr.isImplicitOrMerged(block)) {
				return true;
			}
		}
		return false;
	}

	public static TryCatchEdgeBlockMap getAllInScope(MethodNode mth, TryCatchBlockAttr tryCatch,
			TryEdgeScopeGroupMap scopeGroups, ExceptionHandler finallyHandler,
			Map<BlockNode, List<TryEdge>> scopeTerminusGroups) {
		Map<TryEdge, BlockNode> edgeBlocks = tryCatch.getEdgeBlockMap();
		TryCatchEdgeBlockMap result = new TryCatchEdgeBlockMap();
		for (BlockNode scopeTerminus : scopeTerminusGroups.keySet()) {
			List<TryEdge> sourceEdges = scopeTerminusGroups.get(scopeTerminus);
			for (TryEdge sourceEdge : sourceEdges) {
				BlockNode edgeBlock = edgeBlocks.get(sourceEdge);
				boolean useClean = !(sourceEdge.isNotHandlerExit()
						&& ListUtils.anyMatch(scopeGroups.getMergedScopes(), pair -> pair.getSecond().isNotHandlerExit()));
				List<BlockNode> allBlocks = BlockUtils.collectAllSuccessorsUntil(mth, edgeBlock, useClean, block -> block == scopeTerminus);
				boolean anyBlockHasTry = anyBlockHasNonImplicitTry(allBlocks);
				if (anyBlockHasTry && useClean) {
					// If there's a try edge in the found blocks, collect all successors, not just clean successors.
					allBlocks = BlockUtils.collectAllSuccessorsUntil(mth, edgeBlock, false, block -> block == scopeTerminus);
				}
				if (sourceEdge.isNotHandlerExit()) {
					// If source edge is a fallthrough case, add the try body.
					allBlocks = new ArrayList<>(allBlocks);
					allBlocks.addAll(tryCatch.getBlocks());
				}
				result.put(sourceEdge, allBlocks);
			}
		}

		List<BlockNode> finallyBlocks = result.getBlocksForHandler(finallyHandler);
		if (finallyBlocks != null) {
			for (TryEdge edge : result.keySet()) {
				if (edge.isHandlerExit() && edge.getExceptionHandler() == finallyHandler) {
					continue;
				}
				List<BlockNode> blocks = result.get(edge);
				blocks.removeAll(finallyBlocks);
			}
		}
		return result;
	}

	private final Map<TryEdge, List<BlockNode>> underlying;

	public TryCatchEdgeBlockMap() {
		underlying = new HashMap<>();
	}

	@Override
	public void clear() {
		underlying.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		return underlying.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		if (!(value instanceof TryEdge)) {
			return false;
		}
		TryEdge edge = (TryEdge) value;
		return underlying.containsKey(edge);
	}

	@Override
	public Set<Entry<TryEdge, List<BlockNode>>> entrySet() {
		return underlying.entrySet();
	}

	@Override
	public List<BlockNode> get(Object key) {
		return underlying.get(key);
	}

	@Override
	public boolean isEmpty() {
		return underlying.isEmpty();
	}

	@Override
	public Set<TryEdge> keySet() {
		return underlying.keySet();
	}

	@Override
	public List<BlockNode> put(TryEdge key, List<BlockNode> value) {
		return underlying.put(key, value);
	}

	@Override
	public void putAll(Map<? extends TryEdge, ? extends List<BlockNode>> otherMap) {
		underlying.putAll(otherMap);
	}

	@Override
	public List<BlockNode> remove(Object key) {
		return underlying.remove(key);
	}

	@Override
	public int size() {
		return underlying.size();
	}

	@Override
	public Collection<List<BlockNode>> values() {
		return underlying.values();
	}

	@Nullable
	public List<BlockNode> getBlocksForHandler(ExceptionHandler handler) {
		TryEdge edgeWithHandler = null;
		for (TryEdge edge : keySet()) {
			if (edge.isNotHandlerExit()) {
				continue;
			}
			if (!edge.getExceptionHandler().equals(handler)) {
				continue;
			}
			edgeWithHandler = edge;
			break;
		}
		if (edgeWithHandler == null) {
			return null;
		}
		return get(edgeWithHandler);
	}

	public List<BlockNode> getBlocksForAllFallthroughs() {
		List<BlockNode> blks = new ArrayList<>();
		for (TryEdge edge : keySet()) {
			if (edge.isHandlerExit()) {
				continue;
			}

			blks.addAll(get(edge));
		}
		return blks;
	}
}
