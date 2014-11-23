package jadx.core.dex.visitors.blocksmaker.helpers;

import jadx.core.dex.nodes.BlockNode;

public final class BlocksPair {
	private final BlockNode first;
	private final BlockNode second;

	public BlocksPair(BlockNode first, BlockNode second) {
		this.first = first;
		this.second = second;
	}

	public BlockNode getFirst() {
		return first;
	}

	public BlockNode getSecond() {
		return second;
	}

	@Override
	public int hashCode() {
		return 31 * first.hashCode() + second.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof BlocksPair)) {
			return false;
		}
		BlocksPair other = (BlocksPair) o;
		return first.equals(other.first) && second.equals(other.second);
	}

	@Override
	public String toString() {
		return "(" + first + ", " + second + ")";
	}
}
