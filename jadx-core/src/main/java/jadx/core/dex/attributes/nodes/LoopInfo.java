package jadx.core.dex.attributes.nodes;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.Edge;
import jadx.core.utils.BlockUtils;

public class LoopInfo {

	private final BlockNode start;
	private final BlockNode end;
	private final Set<BlockNode> loopBlocks;

	private int id;
	private LoopInfo parentLoop;

	public LoopInfo(BlockNode start, BlockNode end) {
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

	public Set<BlockNode> getLoopBlocks() {
		return loopBlocks;
	}

	/**
	 * Return source blocks of exit edges. <br>
	 * Exit nodes belongs to loop (contains in {@code loopBlocks})
	 */
	public Set<BlockNode> getExitNodes() {
		Set<BlockNode> nodes = new HashSet<>();
		Set<BlockNode> blocks = getLoopBlocks();
		for (BlockNode block : blocks) {
			// exit: successor node not from this loop, (don't change to getCleanSuccessors)
			for (BlockNode s : block.getSuccessors()) {
				if (!blocks.contains(s) && !s.contains(AType.EXC_HANDLER)) {
					nodes.add(block);
				}
			}
		}
		return nodes;
	}

	/**
	 * Return loop exit edges.
	 */
	public List<Edge> getExitEdges() {
		List<Edge> edges = new LinkedList<>();
		Set<BlockNode> blocks = getLoopBlocks();
		for (BlockNode block : blocks) {
			for (BlockNode s : block.getSuccessors()) {
				if (!blocks.contains(s) && !s.contains(AType.EXC_HANDLER)) {
					edges.add(new Edge(block, s));
				}
			}
		}
		return edges;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public LoopInfo getParentLoop() {
		return parentLoop;
	}

	public void setParentLoop(LoopInfo parentLoop) {
		this.parentLoop = parentLoop;
	}

	@Override
	public String toString() {
		return "LOOP:" + id + ": " + start + "->" + end;
	}
}
