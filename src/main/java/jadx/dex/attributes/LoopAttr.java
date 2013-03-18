package jadx.dex.attributes;

import jadx.dex.nodes.BlockNode;
import jadx.utils.BlockUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class LoopAttr implements IAttribute {

	private final BlockNode start;
	private final BlockNode end;
	private final Set<BlockNode> loopBlocks;

	public LoopAttr(BlockNode start, BlockNode end) {
		this.start = start;
		this.end = end;
		this.loopBlocks = Collections.unmodifiableSet(BlockUtils.getAllPathsBlocks(start, end));
	}

	public BlockNode getStart() {
		return start;
	}

	public BlockNode getEnd() {
		return end;
	}

	@Override
	public AttributeType getType() {
		return AttributeType.LOOP;
	}

	public Set<BlockNode> getLoopBlocks() {
		return loopBlocks;
	}

	/**
	 * Return block nodes with exit edges from loop <br>
	 * Exit nodes belongs to loop (contains in {@code loopBlocks})
	 */
	public Set<BlockNode> getExitNodes() {
		Set<BlockNode> nodes = new HashSet<BlockNode>();
		Set<BlockNode> inloop = getLoopBlocks();
		for (BlockNode block : inloop) {
			// exit: successor node not from this loop, (don't change to getCleanSuccessors)
			for (BlockNode s : block.getSuccessors())
				if (!inloop.contains(s) && !s.getAttributes().contains(AttributeType.EXC_HANDLER))
					nodes.add(block);
		}
		return nodes;
	}

	@Override
	public String toString() {
		return "LOOP: " + start + "->" + end;
	}
}
