package jadx.core.dex.visitors.finaly.traverser.state;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.finaly.CentralityState;
import jadx.core.dex.visitors.finaly.SameInstructionsStrategy;
import jadx.core.dex.visitors.finaly.traverser.GlobalTraverserSourceState;
import jadx.core.dex.visitors.finaly.traverser.factory.TraverserStateFactory;
import jadx.core.utils.Pair;
import jadx.core.utils.exceptions.JadxRuntimeException;

/**
 * A state used by the traverser controller. For two given branches, the "finally" branch and the
 * "candidate" branch whilst determining similar instructions and blocks, the active path state
 * contains information regarding the current matched instructions and blocks, as well as the
 * current state of both the finally and candidate path being explored.
 */
public class TraverserActivePathState {

	/**
	 * Produces a shallow clone of the {@link TraverserActivePathState}. Since this is a shallow clone,
	 * it should only be used for a singular branch. If a branch is used and this needs to be
	 * duplicated, be sure to use the deep clone duplication on the previous
	 * {@link TraverserActivePathState} before invoking this method on it.
	 *
	 * @param previousTraverserState The previous active path state to create a shallow clone of.
	 * @param finallyStateProducer   The factory responsible for producing the new finally state to be
	 *                               held by the resulting active path state.
	 * @param candidateStateProducer The factory responsible for producing the new candidate state to
	 *                               be held by the resulting active path state.
	 * @return The cloned active path state.
	 */
	public static TraverserActivePathState produceFromFactories(TraverserActivePathState previousTraverserState,
			TraverserStateFactory<?> finallyStateProducer, TraverserStateFactory<?> candidateStateProducer) {
		TraverserActivePathState dState =
				new TraverserActivePathState(previousTraverserState.matchedInsns, previousTraverserState.finallyCompletionMonitor,
						previousTraverserState.candidateCompletionMonitor, previousTraverserState.commonGlobalState,
						previousTraverserState.finallyGlobalState,
						previousTraverserState.candidateGlobalState);

		TraverserState dFinallyState = finallyStateProducer.generateState(dState);
		TraverserState dCandidateState = candidateStateProducer.generateState(dState);
		dState.candidateStateRef.set(dCandidateState);
		dState.finallyStateRef.set(dFinallyState);
		return dState;
	}

	/**
	 * Tracks the comparison state of a given block.
	 * i.e. how many of the instructions have been compared in the Traversal.
	 */
	private static final class BlockCompletionMonitor {
		private final BlockNode block;
		private final Set<Integer> matchedIndices;

		private BlockCompletionMonitor(BlockNode block) {
			this.block = block;
			int insnCount = block.getInstructions().size();
			this.matchedIndices = new HashSet<>(insnCount);
			for (int i = 0; i < insnCount; i++) {
				matchedIndices.add(i);
			}
		}

		private void registerWithBlockInfo(TraverserBlockInfo info, int numberMatched) {
			if (info.getBlock() != block) {
				return;
			}
			int botPointer = info.getBottomOffset();
			for (int i = 0; i < numberMatched; i++) {
				int indexMatched = botPointer + i;
				matchedIndices.remove(indexMatched);
			}
			int bottomImplicitCount = info.getBottomImplicitCount();
			boolean noPathEndInsns = botPointer - bottomImplicitCount == 0;
			if (noPathEndInsns) {
				for (int i = 0; i < bottomImplicitCount; i++) {
					matchedIndices.remove(i);
				}
			}
		}

		private BlockCompletionMonitor duplicate() {
			BlockCompletionMonitor dup = new BlockCompletionMonitor(block);
			dup.matchedIndices.retainAll(matchedIndices);
			return dup;
		}

		private void mergeWith(BlockCompletionMonitor other) {
			if (other.block != block) {
				return;
			}
			matchedIndices.retainAll(other.matchedIndices);
		}

		private boolean isEntireBlock() {
			return matchedIndices.isEmpty();
		}
	}

	private static final class BlockCompletionMonitorMap implements Map<BlockNode, BlockCompletionMonitor> {

		private final Map<BlockNode, BlockCompletionMonitor> underlying;

		public BlockCompletionMonitorMap() {
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
			if (!(value instanceof BlockNode)) {
				return false;
			}
			BlockNode edge = (BlockNode) value;
			return underlying.containsKey(edge);
		}

		@Override
		public Set<Entry<BlockNode, BlockCompletionMonitor>> entrySet() {
			return underlying.entrySet();
		}

		@Override
		public BlockCompletionMonitor get(Object key) {
			return underlying.get(key);
		}

		@Override
		public boolean isEmpty() {
			return underlying.isEmpty();
		}

		@Override
		public Set<BlockNode> keySet() {
			return underlying.keySet();
		}

		@Override
		public BlockCompletionMonitor put(BlockNode key, BlockCompletionMonitor value) {
			return underlying.put(key, value);
		}

		@Override
		public void putAll(Map<? extends BlockNode, ? extends BlockCompletionMonitor> otherMap) {
			underlying.putAll(otherMap);
		}

		@Override
		public BlockCompletionMonitor remove(Object key) {
			return underlying.remove(key);
		}

		@Override
		public int size() {
			return underlying.size();
		}

		@Override
		public Collection<BlockCompletionMonitor> values() {
			return underlying.values();
		}

		private void registerWithBlockInfo(TraverserBlockInfo info, int numberMatched) {
			BlockNode block = info.getBlock();
			if (containsKey(block)) {
				get(block).registerWithBlockInfo(info, numberMatched);
			} else {
				BlockCompletionMonitor monitor = new BlockCompletionMonitor(block);
				monitor.registerWithBlockInfo(info, numberMatched);
				put(block, monitor);
			}
		}

		private void mergeEntry(BlockCompletionMonitor other) {
			BlockNode block = other.block;
			if (containsKey(block)) {
				get(block).mergeWith(other);
			} else {
				BlockCompletionMonitor monitor = other.duplicate();
				put(block, monitor);
			}
		}

		private void mergeMap(BlockCompletionMonitorMap other) {
			for (BlockCompletionMonitor monitor : other.values()) {
				mergeEntry(monitor);
			}
		}

		private BlockCompletionMonitorMap duplicate() {
			BlockCompletionMonitorMap dup = new BlockCompletionMonitorMap();
			for (BlockNode sourceBlock : keySet()) {
				BlockCompletionMonitor monitor = get(sourceBlock);
				dup.put(sourceBlock, monitor.duplicate());
			}
			return dup;
		}
	}

	private final AtomicReference<TraverserState> finallyStateRef;
	private final AtomicReference<TraverserState> candidateStateRef;
	private final GlobalTraverserSourceState finallyGlobalState;
	private final GlobalTraverserSourceState candidateGlobalState;
	private final TraverserGlobalCommonState commonGlobalState;

	private final Set<Pair<InsnNode>> matchedInsns;
	private final BlockCompletionMonitorMap finallyCompletionMonitor;
	private final BlockCompletionMonitorMap candidateCompletionMonitor;

	/**
	 * Creates a new instance of a traversal active path. This constructor is used to create a new
	 * path to be used by the traverser controller to begin a new traversal.
	 */
	public TraverserActivePathState(MethodNode mth, SameInstructionsStrategy sameInstructionsStrategy,
			BlockNode finallyBlockTerminus, BlockNode candidateBlockTerminus, List<BlockNode> finallyBlocks,
			List<BlockNode> candidateBlocks) {
		boolean shouldFinallyAllowFirstBlockSkip = !finallyBlockTerminus.getInstructions().isEmpty();
		boolean shouldCandidateAllowFirstBlockSkip = !candidateBlockTerminus.getInstructions().isEmpty();
		CentralityState finallyCentralityState = new CentralityState(sameInstructionsStrategy, shouldFinallyAllowFirstBlockSkip);
		CentralityState candidateCentralityState = new CentralityState(sameInstructionsStrategy, shouldCandidateAllowFirstBlockSkip);

		TraverserBlockInfo finallyBlockInfo = new TraverserBlockInfo(finallyBlockTerminus);
		TraverserBlockInfo candidateBlockInfo = new TraverserBlockInfo(candidateBlockTerminus);

		TraverserState finallyState = new NewBlockTraverserState(this, finallyCentralityState, finallyBlockInfo);
		TraverserState candidateState = new NewBlockTraverserState(this, candidateCentralityState, candidateBlockInfo);

		this.finallyGlobalState = new GlobalTraverserSourceState(new HashSet<>(finallyBlocks));
		this.candidateGlobalState = new GlobalTraverserSourceState(new HashSet<>(candidateBlocks));
		this.commonGlobalState = new TraverserGlobalCommonState(mth);

		this.finallyStateRef = new AtomicReference<>(finallyState);
		this.candidateStateRef = new AtomicReference<>(candidateState);
		this.matchedInsns = new HashSet<>();
		this.finallyCompletionMonitor = new BlockCompletionMonitorMap();
		this.candidateCompletionMonitor = new BlockCompletionMonitorMap();
	}

	/**
	 * Creates a new instance of a traversal active path. This constructor is used to duplicate a
	 * state between a previous traverser controller and is a liaison for initialising non-null
	 * final fields for the {@link TraverserActivePathState#produceFromFactories} function.
	 */
	private TraverserActivePathState(Set<Pair<InsnNode>> matchedInsns, BlockCompletionMonitorMap finallyCompletionMonitor,
			BlockCompletionMonitorMap candidateCompletionMonitor, TraverserGlobalCommonState commonGlobalState,
			GlobalTraverserSourceState finallyGlobalState, GlobalTraverserSourceState candidateGlobalState) {
		this.finallyStateRef = new AtomicReference<>();
		this.candidateStateRef = new AtomicReference<>();
		this.matchedInsns = matchedInsns;
		this.finallyGlobalState = finallyGlobalState;
		this.candidateGlobalState = candidateGlobalState;
		this.commonGlobalState = commonGlobalState;
		this.finallyCompletionMonitor = finallyCompletionMonitor;
		this.candidateCompletionMonitor = candidateCompletionMonitor;
	}

	public final TraverserActivePathState duplicate() {
		Set<Pair<InsnNode>> dMatchedInsns = new HashSet<>(matchedInsns);
		BlockCompletionMonitorMap dFinallyCompletionMonitor = finallyCompletionMonitor.duplicate();
		BlockCompletionMonitorMap dCandidateCompletionMonitor = candidateCompletionMonitor.duplicate();
		TraverserActivePathState dState =
				new TraverserActivePathState(dMatchedInsns, dFinallyCompletionMonitor, dCandidateCompletionMonitor,
						commonGlobalState, finallyGlobalState, candidateGlobalState);

		TraverserState dFinallyState = getFinallyState().duplicate(dState);
		TraverserState dCandidateState = getCandidateState().duplicate(dState);
		dState.candidateStateRef.set(dCandidateState);
		dState.finallyStateRef.set(dFinallyState);
		return dState;
	}

	public final TraverserState getFinallyState() {
		return finallyStateRef.get();
	}

	public final TraverserState getCandidateState() {
		return candidateStateRef.get();
	}

	public final AtomicReference<TraverserState> getFinallyStateRef() {
		return finallyStateRef;
	}

	public final AtomicReference<TraverserState> getCandidateStateRef() {
		return candidateStateRef;
	}

	public final Set<Pair<InsnNode>> getMatchedInsns() {
		return matchedInsns;
	}

	@Nullable
	public final AtomicReference<TraverserState> getReferenceForState(TraverserState state) {
		if (finallyStateRef.get() == state) {
			return finallyStateRef;
		}
		if (candidateStateRef.get() == state) {
			return candidateStateRef;
		}
		return null;
	}

	public final GlobalTraverserSourceState getGlobalStateFor(TraverserState state) {
		if (finallyStateRef.get() == state) {
			return finallyGlobalState;
		}
		if (candidateStateRef.get() == state) {
			return candidateGlobalState;
		}
		throw new JadxRuntimeException("Orphaned TraverserState node");
	}

	public final GlobalTraverserSourceState getFinallyGlobalState() {
		return finallyGlobalState;
	}

	public final GlobalTraverserSourceState getCandidateGlobalState() {
		return candidateGlobalState;
	}

	public final TraverserGlobalCommonState getGlobalCommonState() {
		return commonGlobalState;
	}

	public final void mergeWith(List<TraverserActivePathState> otherStates) {
		for (TraverserActivePathState otherState : otherStates) {
			matchedInsns.addAll(otherState.getMatchedInsns());

			finallyCompletionMonitor.mergeMap(otherState.finallyCompletionMonitor);
			candidateCompletionMonitor.mergeMap(otherState.candidateCompletionMonitor);
		}
	}

	public final void registerWithBlockInfo(TraverserBlockInfo info, int numberMatched) {
		BlockNode block = info.getBlock();
		boolean isFinallyBlock = finallyGlobalState.isBlockContained(block);
		BlockCompletionMonitorMap monitorMap;
		if (isFinallyBlock) {
			monitorMap = finallyCompletionMonitor;
		} else {
			monitorMap = candidateCompletionMonitor;
		}
		monitorMap.registerWithBlockInfo(info, numberMatched);
	}

	public final Set<BlockNode> getAllFullyMatchedFinallyBlocks() {
		return getAllFullyMatchedBlocks(finallyCompletionMonitor);
	}

	public final Set<BlockNode> getAllFullyMatchedCandidateBlocks() {
		return getAllFullyMatchedBlocks(candidateCompletionMonitor);
	}

	private Set<BlockNode> getAllFullyMatchedBlocks(BlockCompletionMonitorMap monitorMap) {
		Set<BlockNode> matches = new HashSet<>();
		for (BlockCompletionMonitor monitor : monitorMap.values()) {
			if (!monitor.isEntireBlock()) {
				continue;
			}
			matches.add(monitor.block);
		}
		return matches;
	}
}
