package jadx.core.dex.visitors.finaly.traverser.state;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.Pair;

public final class TraverserGlobalCommonState {

	private final MethodNode mth;
	private final Map<Pair<BlockNode>, List<TraverserActivePathState>> searchedStates;

	public TraverserGlobalCommonState(final MethodNode mth) {
		this.mth = mth;
		this.searchedStates = new HashMap<>();
	}

	public final void addCachedStateFor(final BlockNode finallyBlock, final BlockNode candidateBlock,
			final List<TraverserActivePathState> state) {
		final Pair<BlockNode> blocks = new Pair<>(finallyBlock, candidateBlock);
		searchedStates.put(blocks, state);
	}

	@Nullable
	public final List<TraverserActivePathState> getCachedStateFor(final BlockNode finallyBlock, final BlockNode candidateBlock) {
		final Pair<BlockNode> blocks = new Pair<>(finallyBlock, candidateBlock);
		return searchedStates.get(blocks);
	}

	public final boolean hasBlocksBeenCached(final BlockNode finallyBlock, final BlockNode candidateBlock) {
		final Pair<BlockNode> blocks = new Pair<>(finallyBlock, candidateBlock);
		return searchedStates.containsKey(blocks);
	}

	public final MethodNode getMethodNode() {
		return mth;
	}
}
