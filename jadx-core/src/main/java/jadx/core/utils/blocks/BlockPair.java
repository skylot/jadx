package jadx.core.utils.blocks;

import jadx.core.dex.nodes.BlockNode;

public class BlockPair {
	private final BlockNode first;
	private final BlockNode second;

	public BlockPair(BlockNode first, BlockNode second) {
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
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof BlockPair)) {
			return false;
		}
		BlockPair other = (BlockPair) o;
		return first.equals(other.first) && second.equals(other.second);
	}

	@Override
	public int hashCode() {
		return first.hashCode() + 31 * second.hashCode();
	}

	@Override
	public String toString() {
		return "(" + first + ", " + second + ')';
	}
}
