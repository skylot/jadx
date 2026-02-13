package jadx.core.dex.visitors.finaly.traverser;

import java.util.Set;

import jadx.core.dex.nodes.BlockNode;

/**
 * A state used by the traverser controller for storing information regarding an entire path to
 * take during traversal. This should be static amongst all states following the same path.
 */
public final class GlobalTraverserSourceState {

	private final Set<BlockNode> containedBlocks;

	public GlobalTraverserSourceState(final Set<BlockNode> containedBlocks) {
		this.containedBlocks = containedBlocks;
	}

	public final boolean isBlockContained(final BlockNode block) {
		return containedBlocks.contains(block);
	}

	public final Set<BlockNode> getContainedBlocks() {
		return containedBlocks;
	}
}
