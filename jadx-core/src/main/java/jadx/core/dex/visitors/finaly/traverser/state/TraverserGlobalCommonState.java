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

	public TraverserGlobalCommonState(MethodNode mth) {
		this.mth = mth;
		this.searchedStates = new HashMap<>();
	}

	public void addCachedStateFor(BlockNode finallyBlock, BlockNode candidateBlock, List<TraverserActivePathState> state) {
		Pair<BlockNode> blocks = new Pair<>(finallyBlock, candidateBlock);
		searchedStates.put(blocks, state);
	}

	@Nullable
	public List<TraverserActivePathState> getCachedStateFor(BlockNode finallyBlock, BlockNode candidateBlock) {
		Pair<BlockNode> blocks = new Pair<>(finallyBlock, candidateBlock);
		return searchedStates.get(blocks);
	}

	public boolean hasBlocksBeenCached(BlockNode finallyBlock, BlockNode candidateBlock) {
		Pair<BlockNode> blocks = new Pair<>(finallyBlock, candidateBlock);
		return searchedStates.containsKey(blocks);
	}

	public MethodNode getMethodNode() {
		return mth;
	}
}
