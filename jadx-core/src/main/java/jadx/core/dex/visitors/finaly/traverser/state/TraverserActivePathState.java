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
	public static TraverserActivePathState produceFromFactories(final TraverserActivePathState previousTraverserState,
			final TraverserStateFactory<?> finallyStateProducer, final TraverserStateFactory<?> candidateStateProducer) {
		final TraverserActivePathState dState =
				new TraverserActivePathState(previousTraverserState.matchedInsns, previousTraverserState.finallyCompletionMonitor,
						previousTraverserState.candidateCompletionMonitor, previousTraverserState.commonGlobalState,
						previousTraverserState.finallyGlobalState,
						previousTraverserState.candidateGlobalState);

		final TraverserState dFinallyState = finallyStateProducer.generateState(dState);
		final TraverserState dCandidateState = candidateStateProducer.generateState(dState);

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
		private final int insnCount;

		private BlockCompletionMonitor(final BlockNode block) {
			this.block = block;
			this.insnCount = block.getInstructions().size();
			this.matchedIndices = new HashSet<>(insnCount);
			for (int i = 0; i < insnCount; i++) {
				matchedIndices.add(i);
			}
		}

		private void registerWithBlockInfo(final TraverserBlockInfo info, final int numberMatched) {
			if (info.getBlock() != block) {
				return;
			}

			final int botPointer = info.getBottomOffset();
			for (int i = 0; i < numberMatched; i++) {
				final int indexMatched = botPointer + i;
				matchedIndices.remove(indexMatched);
			}

			final int bottomImplicitCount = info.getBottomImplicitCount();
			final boolean noPathEndInsns = botPointer - bottomImplicitCount == 0;
			if (noPathEndInsns) {
				for (int i = 0; i < bottomImplicitCount; i++) {
					matchedIndices.remove(i);
				}
			}
		}

		private BlockCompletionMonitor duplicate() {
			final BlockCompletionMonitor dup = new BlockCompletionMonitor(block);
			dup.matchedIndices.retainAll(matchedIndices);
			return dup;
		}

		private void mergeWith(final BlockCompletionMonitor other) {
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
		public final void clear() {
			underlying.clear();
		}

		@Override
		public final boolean containsKey(Object key) {
			return underlying.containsKey(key);
		}

		@Override
		public final boolean containsValue(Object value) {
			if (!(value instanceof BlockNode)) {
				return false;
			}

			final BlockNode edge = (BlockNode) value;
			return underlying.containsKey(edge);
		}

		@Override
		public final Set<Entry<BlockNode, BlockCompletionMonitor>> entrySet() {
			return underlying.entrySet();
		}

		@Override
		public final BlockCompletionMonitor get(Object key) {
			return underlying.get(key);
		}

		@Override
		public final boolean isEmpty() {
			return underlying.isEmpty();
		}

		@Override
		public final Set<BlockNode> keySet() {
			return underlying.keySet();
		}

		@Override
		public final BlockCompletionMonitor put(BlockNode key, BlockCompletionMonitor value) {
			return underlying.put(key, value);
		}

		@Override
		public final void putAll(Map<? extends BlockNode, ? extends BlockCompletionMonitor> otherMap) {
			underlying.putAll(otherMap);
		}

		@Override
		public final BlockCompletionMonitor remove(Object key) {
			return underlying.remove(key);
		}

		@Override
		public final int size() {
			return underlying.size();
		}

		@Override
		public final Collection<BlockCompletionMonitor> values() {
			return underlying.values();
		}

		private void registerWithBlockInfo(final TraverserBlockInfo info, final int numberMatched) {
			final BlockNode block = info.getBlock();
			if (containsKey(block)) {
				get(block).registerWithBlockInfo(info, numberMatched);
			} else {
				final BlockCompletionMonitor monitor = new BlockCompletionMonitor(block);
				monitor.registerWithBlockInfo(info, numberMatched);
				put(block, monitor);
			}
		}

		private void mergeEntry(final BlockCompletionMonitor other) {
			final BlockNode block = other.block;
			if (containsKey(block)) {
				get(block).mergeWith(other);
			} else {
				final BlockCompletionMonitor monitor = other.duplicate();
				put(block, monitor);
			}
		}

		private void mergeMap(final BlockCompletionMonitorMap other) {
			for (final BlockCompletionMonitor monitor : other.values()) {
				mergeEntry(monitor);
			}
		}

		private BlockCompletionMonitorMap duplicate() {
			final BlockCompletionMonitorMap dup = new BlockCompletionMonitorMap();
			for (final BlockNode sourceBlock : keySet()) {
				final BlockCompletionMonitor monitor = get(sourceBlock);
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
	 *
	 * @param mth
	 * @param sameInstructionsStrategy
	 * @param finallyBlockTerminus
	 * @param candidateBlockTerminus
	 * @param finallyBlocks
	 * @param candidateBlocks
	 */
	public TraverserActivePathState(final MethodNode mth, final SameInstructionsStrategy sameInstructionsStrategy,
			final BlockNode finallyBlockTerminus, final BlockNode candidateBlockTerminus, final List<BlockNode> finallyBlocks,
			final List<BlockNode> candidateBlocks) {
		final boolean shouldFinallyAllowFirstBlockSkip = !finallyBlockTerminus.getInstructions().isEmpty();
		final boolean shouldCandidateAllowFirstBlockSkip = !candidateBlockTerminus.getInstructions().isEmpty();
		final CentralityState finallyCentralityState = new CentralityState(sameInstructionsStrategy, shouldFinallyAllowFirstBlockSkip);
		final CentralityState candidateCentralityState = new CentralityState(sameInstructionsStrategy, shouldCandidateAllowFirstBlockSkip);

		final TraverserBlockInfo finallyBlockInfo = new TraverserBlockInfo(finallyBlockTerminus);
		final TraverserBlockInfo candidateBlockInfo = new TraverserBlockInfo(candidateBlockTerminus);

		final TraverserState finallyState = new NewBlockTraverserState(this, finallyCentralityState, finallyBlockInfo);
		final TraverserState candidateState = new NewBlockTraverserState(this, candidateCentralityState, candidateBlockInfo);

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
	 *
	 * @param matchedInsns
	 * @param finallyCompletionMonitor
	 * @param candidateCompletionMonitor
	 * @param commonGlobalState
	 * @param finallyGlobalState
	 * @param candidateGlobalState
	 */
	private TraverserActivePathState(final Set<Pair<InsnNode>> matchedInsns, final BlockCompletionMonitorMap finallyCompletionMonitor,
			final BlockCompletionMonitorMap candidateCompletionMonitor, final TraverserGlobalCommonState commonGlobalState,
			final GlobalTraverserSourceState finallyGlobalState, final GlobalTraverserSourceState candidateGlobalState) {
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
		final Set<Pair<InsnNode>> dMatchedInsns = new HashSet<>(matchedInsns);
		final BlockCompletionMonitorMap dFinallyCompletionMonitor = finallyCompletionMonitor.duplicate();
		final BlockCompletionMonitorMap dCandidateCompletionMonitor = candidateCompletionMonitor.duplicate();
		final TraverserActivePathState dState =
				new TraverserActivePathState(dMatchedInsns, dFinallyCompletionMonitor, dCandidateCompletionMonitor,
						commonGlobalState, finallyGlobalState, candidateGlobalState);

		final TraverserState dFinallyState = getFinallyState().duplicate(dState);
		final TraverserState dCandidateState = getCandidateState().duplicate(dState);

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
	public final AtomicReference<TraverserState> getReferenceForState(final TraverserState state) {
		if (finallyStateRef.get() == state) {
			return finallyStateRef;
		} else if (candidateStateRef.get() == state) {
			return candidateStateRef;
		} else {
			return null;
		}
	}

	public final GlobalTraverserSourceState getGlobalStateFor(final TraverserState state) {
		if (finallyStateRef.get() == state) {
			return finallyGlobalState;
		} else if (candidateStateRef.get() == state) {
			return candidateGlobalState;
		} else {
			throw new JadxRuntimeException("Orphaned TraverserState node");
		}
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

	public final void mergeWith(final List<TraverserActivePathState> otherStates) {
		for (final TraverserActivePathState otherState : otherStates) {
			matchedInsns.addAll(otherState.getMatchedInsns());

			finallyCompletionMonitor.mergeMap(otherState.finallyCompletionMonitor);
			candidateCompletionMonitor.mergeMap(otherState.candidateCompletionMonitor);
		}
	}

	public final void registerWithBlockInfo(final TraverserBlockInfo info, final int numberMatched) {
		final BlockNode block = info.getBlock();
		final boolean isFinallyBlock = finallyGlobalState.isBlockContained(block);
		final BlockCompletionMonitorMap monitorMap;
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

	private Set<BlockNode> getAllFullyMatchedBlocks(final BlockCompletionMonitorMap monitorMap) {
		final Set<BlockNode> matches = new HashSet<>();

		for (final BlockCompletionMonitor monitor : monitorMap.values()) {
			if (!monitor.isEntireBlock()) {
				continue;
			}

			matches.add(monitor.block);
		}

		return matches;
	}
}
