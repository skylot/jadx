package jadx.core.dex.trycatch;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.Pair;
import jadx.core.utils.exceptions.JadxRuntimeException;

/**
 * A map which stores the information of how try edges correlate with each other.
 * K is a try edge and V contains all other try edges whose who share the same logical scope.
 */
public final class TryEdgeScopeGroupMap implements Map<TryEdge, Map<TryEdge, BlockNode>> {

	private static final class TryEdgeScope {

		private final TryEdge edge;
		private final BlockNode block;

		public TryEdgeScope(TryEdge edge, BlockNode block) {
			this.edge = edge;
			this.block = block;
		}
	}

	private final List<Pair<TryEdge>> mergedEdges = new ArrayList<>();
	private final TryCatchBlockAttr tryCatch;
	private final Map<TryEdge, Map<TryEdge, BlockNode>> underlyingMap;

	public TryEdgeScopeGroupMap(MethodNode mth, TryCatchBlockAttr tryCatch, int initialCapacity) {
		this.tryCatch = tryCatch;
		underlyingMap = new HashMap<>(initialCapacity);
	}

	@Override
	public void clear() {
		underlyingMap.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		return underlyingMap.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		if (!(value instanceof TryEdge)) {
			return false;
		}

		TryEdge edge = (TryEdge) value;
		return underlyingMap.containsKey(edge);
	}

	@Override
	public Set<Entry<TryEdge, Map<TryEdge, BlockNode>>> entrySet() {
		return underlyingMap.entrySet();
	}

	@Override
	public Map<TryEdge, BlockNode> get(Object key) {
		return underlyingMap.get(key);
	}

	@Override
	public boolean isEmpty() {
		return underlyingMap.isEmpty();
	}

	@Override
	public Set<TryEdge> keySet() {
		return underlyingMap.keySet();
	}

	@Override
	public Map<TryEdge, BlockNode> put(TryEdge key, Map<TryEdge, BlockNode> value) {
		return underlyingMap.put(key, value);
	}

	@Override
	public void putAll(Map<? extends TryEdge, ? extends Map<TryEdge, BlockNode>> otherMap) {
		underlyingMap.putAll(otherMap);
	}

	@Override
	public Map<TryEdge, BlockNode> remove(Object key) {
		return underlyingMap.remove(key);
	}

	@Override
	public int size() {
		return underlyingMap.size();
	}

	@Override
	public Collection<Map<TryEdge, BlockNode>> values() {
		return underlyingMap.values();
	}

	public boolean hasMergedEdges() {
		return !mergedEdges.isEmpty();
	}

	public List<Pair<TryEdge>> getMergedScopes() {
		return mergedEdges;
	}

	public void populateFromEdges(Map<TryEdge, BlockNode> edges) {
		mergeSameScopes(edges);

		for (TryEdge edge : edges.keySet()) {
			BlockNode edgeBlock = edges.get(edge);

			Map<TryEdge, BlockNode> handlerFallthroughMap = createEdgeTerminusMap(edges, edge, edgeBlock);
			put(edge, handlerFallthroughMap);
		}
	}

	/**
	 * Returns a map of all points where edges meet with each other, dictating the end of that
	 * edge's scope.
	 */
	public Map<BlockNode, List<TryEdge>> getScopeEnds(MethodNode mth) {
		Map<BlockNode, List<TryEdge>> groups = new HashMap<>();

		// A list containing pairs of edges where there are no shared common clean successors between the
		// two handlers. This usually indicates that these edge pairs must be processed differently.
		List<TryEdge> isolatedEdgePairs = new LinkedList<>();

		for (TryEdge mergeEdgeA : keySet()) {
			Pair<TryEdge> edgeMergedPair = getMergedNodeFromEdge(mergeEdgeA);

			if (edgeMergedPair != null) {
				continue;
			}

			Map<TryEdge, BlockNode> handlerRelations = get(mergeEdgeA);

			List<BlockNode> scopeEnds = new ArrayList<>(handlerRelations.size());
			for (TryEdge mergeEdgeB : handlerRelations.keySet()) {
				Pair<TryEdge> mergedPairFromRelation = getMergedNodeFromEdge(mergeEdgeB);
				if (mergedPairFromRelation != null && mergedPairFromRelation.getFirst() == mergeEdgeA) {
					continue;
				}

				BlockNode sharedTerminator = handlerRelations.get(mergeEdgeB);

				if (sharedTerminator == null) {
					// There are no common clean succesors between the two handlers.
					isolatedEdgePairs.add(mergeEdgeB);
				} else {
					scopeEnds.add(sharedTerminator);
				}
			}

			if (scopeEnds.isEmpty()) {
				// Isolated edge pairs found - we will deal with them later
				continue;
			}

			BlockNode topGrouping = BlockUtils.getTopBlock(scopeEnds);

			if (groups.containsKey(topGrouping)) {
				groups.get(topGrouping).add(mergeEdgeA);
			} else {
				List<TryEdge> groupingHandlers = new LinkedList<>();
				groupingHandlers.add(mergeEdgeA);
				groups.put(topGrouping, groupingHandlers);
			}
		}

		for (TryEdge isolatedEdge : isolatedEdgePairs) {
			boolean isInList = false;
			for (List<TryEdge> foundEdges : groups.values()) {
				if (foundEdges.contains(isolatedEdge)) {
					isInList = true;
					break;
				}
			}

			if (isInList) {
				// The isolated edge is not isolated with another handler - we can ignore this edge.
				break;
			}

			// If an isolated edge has not been added to the groupings, we will add it now.
			// This will be added by locating the point where the search for a common successor stops.
			// Since a common successor of all blocks which do have some clean path can be found in the method
			// exit node, the mentioned point will be the farthest successor of the edge target which has no
			// clean successors.

			BlockNode target = isolatedEdge.getTarget();
			List<BlockNode> successorBlocks = BlockUtils.collectAllSuccessors(mth, target, true);
			BlockNode cleanSuccessorEnd = BlockUtils.getBottomBlock(successorBlocks);
			if (cleanSuccessorEnd == null) {
				throw new JadxRuntimeException("Could not find bottom clean successor for isolated try edge");
			}

			List<TryEdge> scopeTerminusList;
			if (groups.containsKey(cleanSuccessorEnd)) {
				scopeTerminusList = groups.get(cleanSuccessorEnd);
			} else {
				scopeTerminusList = new LinkedList<>();
				groups.put(cleanSuccessorEnd, scopeTerminusList);
			}
			scopeTerminusList.add(isolatedEdge);
		}

		if (groups.size() == 1) {
			for (Pair<TryEdge> pair : mergedEdges) {
				TryEdge keptEdge = pair.getFirst();
				TryEdge removedEdge = pair.getSecond();

				if (keptEdge.isHandlerExit() && !tryCatch.getHandlers().contains(keptEdge.getExceptionHandler())) {
					continue;
				}
				if (removedEdge.isHandlerExit() && !tryCatch.getHandlers().contains(removedEdge.getExceptionHandler())) {
					continue;
				}

				// If both handlers are not handler exits, we can assume that the code paths merge at some Phi node
				// which begins the finally duplicated code.
				if (keptEdge.isNotHandlerExit() && removedEdge.isNotHandlerExit()) {
					continue;
				}

				for (List<TryEdge> edgesWithTerminus : groups.values()) {
					if (edgesWithTerminus.contains(keptEdge)) {
						edgesWithTerminus.remove(keptEdge);
					}
				}

				BlockNode terminus = get(keptEdge).get(removedEdge);
				List<TryEdge> terminusEdges;
				if (!groups.containsKey(terminus)) {
					terminusEdges = new LinkedList<>();
					terminusEdges.add(keptEdge);
					groups.put(terminus, terminusEdges);
				} else {
					terminusEdges = groups.get(terminus);
				}
				terminusEdges.add(removedEdge);
			}
		}

		return groups;
	}

	@Nullable
	private Pair<TryEdge> getMergedNodeFromEdge(TryEdge edge) {
		for (Pair<TryEdge> pair : mergedEdges) {
			if (pair.getSecond() == edge) {
				return pair;
			}
		}
		return null;
	}

	private Map<TryEdge, BlockNode> createEdgeTerminusMap(Map<TryEdge, BlockNode> edgeStartMap, TryEdge edge,
			BlockNode edgeStart) {
		Map<TryEdge, BlockNode> scopeRelations = new HashMap<>(edgeStartMap.size() - 1);
		for (TryEdge otherEdge : edgeStartMap.keySet()) {
			if (edge == otherEdge) {
				continue;
			}

			BlockNode otherEdgeStart = edgeStartMap.get(otherEdge);

			boolean eitherEdgeIsHandler = edge.isHandlerExit() || otherEdge.isHandlerExit();
			if (otherEdgeStart == edgeStart && eitherEdgeIsHandler) {
				continue;
			}

			if (otherEdgeStart.isMthExitBlock()) {
				scopeRelations.put(otherEdge, otherEdgeStart);
				// Everything leads to the exit node so merged edges are no longer needed
				mergedEdges.clear();
				continue;
			}
			if (edgeStart.isMthExitBlock()) {
				scopeRelations.put(otherEdge, edgeStart);
				// Everything leads to the exit node so merged edges are no longer needed
				mergedEdges.clear();
				continue;
			}

			BitSet sharedPostDominators = (BitSet) edgeStart.getPostDoms().clone();
			BitSet otherPostDoms = otherEdgeStart.getPostDoms();
			if (sharedPostDominators.isEmpty() || otherPostDoms.isEmpty()) {
				continue;
			}
			sharedPostDominators.and(otherPostDoms);

			List<BlockNode> postDomHandler = new LinkedList<>();
			BlockNode currentBlock = edgeStart;
			while (currentBlock != null) {
				postDomHandler.add(currentBlock);
				currentBlock = currentBlock.getIPostDom();
			}

			BlockNode commonPostDom = null;
			currentBlock = otherEdgeStart;
			while (currentBlock != null) {
				if (postDomHandler.contains(currentBlock)) {
					commonPostDom = currentBlock;
					break;
				}
				currentBlock = currentBlock.getIPostDom();
			}

			BlockNode scopeEnd = commonPostDom;
			scopeRelations.put(otherEdge, scopeEnd);
		}
		return scopeRelations;
	}

	/**
	 * If two scopes ever merge, as in if one edge leads to the same execution point as the target of
	 * another edge, this function will record it.
	 *
	 * @param handlers
	 * @return
	 */
	private Map<TryEdge, BlockNode> mergeSameScopes(Map<TryEdge, BlockNode> handlers) {
		List<Entry<TryEdge, BlockNode>> exceptionHandlers = new ArrayList<>(handlers.entrySet());

		List<Pair<TryEdgeScope>> handlerPairs = new LinkedList<>();
		for (int i = 0; i < exceptionHandlers.size(); i++) {
			for (int j = i + 1; j < exceptionHandlers.size(); j++) {
				TryEdgeScope a = new TryEdgeScope(exceptionHandlers.get(i).getKey(), exceptionHandlers.get(i).getValue());
				TryEdgeScope b = new TryEdgeScope(exceptionHandlers.get(j).getKey(), exceptionHandlers.get(j).getValue());
				handlerPairs.add(new Pair<>(a, b));
			}
		}

		Map<TryEdge, BlockNode> simplifiedScopes = new HashMap<>(handlers);

		int i = 0;
		while (i < handlerPairs.size()) {
			Pair<TryEdgeScope> handlerPair = handlerPairs.get(i);

			TryEdgeScope edgeScopeA = handlerPair.getFirst();
			TryEdgeScope edgeScopeB = handlerPair.getSecond();
			BlockNode edgeBlockA = edgeScopeA.block;
			BlockNode edgeBlockB = edgeScopeB.block;
			boolean pathExists = BlockUtils.isPathExists(edgeBlockA, edgeBlockB) || BlockUtils.isPathExists(edgeBlockB, edgeBlockA);
			if (pathExists) {
				BlockNode bottomBlock = BlockUtils.getBottomBlock(List.of(edgeBlockA, edgeBlockB));
				// The two blocks are within the same scope - remove these from the matrix
				TryEdge removeHandler = edgeBlockA != bottomBlock ? edgeScopeA.edge : edgeScopeB.edge;
				TryEdge keepHandler = edgeBlockA == bottomBlock ? edgeScopeA.edge : edgeScopeB.edge;
				simplifiedScopes.remove(removeHandler);
				handlerPairs.remove(i);

				mergedEdges.add(new Pair<>(keepHandler, removeHandler));
			} else {
				i++;
			}
		}

		return simplifiedScopes;
	}
}
