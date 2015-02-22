package jadx.core.dex.visitors.blocksmaker.helpers;

import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

public final class BlocksRemoveInfo {
	private final Set<BlocksPair> processed = new HashSet<BlocksPair>();
	private final Set<BlocksPair> outs = new HashSet<BlocksPair>();
	private final Map<RegisterArg, RegisterArg> regMap = new HashMap<RegisterArg, RegisterArg>();

	private BlocksPair start;
	private BlocksPair end;

	private int startSplitIndex;
	private int endSplitIndex;

	private BlockNode startPredecessor;

	public BlocksRemoveInfo(BlocksPair start) {
		this.start = start;
	}

	public Set<BlocksPair> getProcessed() {
		return processed;
	}

	public Set<BlocksPair> getOuts() {
		return outs;
	}

	public BlocksPair getStart() {
		return start;
	}

	public void setStart(BlocksPair start) {
		this.start = start;
	}

	public BlocksPair getEnd() {
		return end;
	}

	public void setEnd(BlocksPair end) {
		this.end = end;
	}

	public int getStartSplitIndex() {
		return startSplitIndex;
	}

	public void setStartSplitIndex(int startSplitIndex) {
		this.startSplitIndex = startSplitIndex;
	}

	public int getEndSplitIndex() {
		return endSplitIndex;
	}

	public void setEndSplitIndex(int endSplitIndex) {
		this.endSplitIndex = endSplitIndex;
	}

	public void setStartPredecessor(BlockNode startPredecessor) {
		this.startPredecessor = startPredecessor;
	}

	public BlockNode getStartPredecessor() {
		return startPredecessor;
	}

	public Map<RegisterArg, RegisterArg> getRegMap() {
		return regMap;
	}

	@Nullable
	public BlockNode getByFirst(BlockNode first) {
		for (BlocksPair blocksPair : processed) {
			if (blocksPair.getFirst() == first) {
				return blocksPair.getSecond();
			}
		}
		return null;
	}

	@Nullable
	public BlockNode getBySecond(BlockNode second) {
		for (BlocksPair blocksPair : processed) {
			if (blocksPair.getSecond() == second) {
				return blocksPair.getSecond();
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return "BRI start: " + start
				+ ", end: " + end
				+ ", list: " + processed
				+ ", outs: " + outs
				+ ", regMap: " + regMap
				+ ", split: " + startSplitIndex;
	}
}
