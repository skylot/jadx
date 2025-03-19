package jadx.core.utils.blocks;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.MethodNode;

public class DFSIteration {
	private final Function<BlockNode, List<BlockNode>> nextFunc;
	private final Deque<BlockNode> queue;
	private final BlockSet visited;

	public DFSIteration(MethodNode mth, BlockNode startBlock, Function<BlockNode, List<BlockNode>> next) {
		nextFunc = next;
		queue = new ArrayDeque<>();
		visited = new BlockSet(mth);
		queue.addLast(startBlock);
		visited.add(startBlock);
	}

	public @Nullable BlockNode next() {
		BlockNode current = queue.pollLast();
		if (current == null) {
			return null;
		}
		List<BlockNode> nextBlocks = nextFunc.apply(current);
		int count = nextBlocks.size();
		for (int i = count - 1; i >= 0; i--) { // to preserve order in queue
			BlockNode next = nextBlocks.get(i);
			if (!visited.addChecked(next)) {
				queue.addLast(next);
			}
		}
		return current;
	}
}
