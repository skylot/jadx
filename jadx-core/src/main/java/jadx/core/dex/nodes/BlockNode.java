package jadx.core.dex.nodes;

import jadx.core.dex.attributes.AttrNode;
import jadx.core.dex.attributes.AttributeFlag;
import jadx.core.dex.attributes.AttributeType;
import jadx.core.dex.attributes.BlockRegState;
import jadx.core.dex.attributes.LoopAttr;
import jadx.core.utils.InsnUtils;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

public class BlockNode extends AttrNode implements IBlock {

	private int id;
	private final int startOffset;
	private final List<InsnNode> instructions = new ArrayList<InsnNode>(2);

	private List<BlockNode> predecessors = new ArrayList<BlockNode>(1);
	private List<BlockNode> successors = new ArrayList<BlockNode>(1);
	private List<BlockNode> cleanSuccessors;

	// all dominators
	private BitSet doms;
	// dominance frontier
	private BitSet domFrontier;
	// immediate dominator
	private BlockNode idom;
	// blocks on which dominates this block
	private final List<BlockNode> dominatesOn = new ArrayList<BlockNode>(1);

	private BlockRegState startState;
	private BlockRegState endState;

	public BlockNode(int id, int offset) {
		this.id = id;
		this.startOffset = offset;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public List<BlockNode> getPredecessors() {
		return predecessors;
	}

	public List<BlockNode> getSuccessors() {
		return successors;
	}

	public List<BlockNode> getCleanSuccessors() {
		return cleanSuccessors;
	}

	public void updateCleanSuccessors() {
		cleanSuccessors = cleanSuccessors(this);
	}

	public void lock() {
		cleanSuccessors = Collections.unmodifiableList(cleanSuccessors);
		successors = Collections.unmodifiableList(successors);
		predecessors = Collections.unmodifiableList(predecessors);
	}

	/**
	 * Return all successor which are not exception handler or followed by loop back edge
	 */
	private static List<BlockNode> cleanSuccessors(BlockNode block) {
		List<BlockNode> sucList = block.getSuccessors();
		List<BlockNode> nodes = new ArrayList<BlockNode>(sucList.size());
		LoopAttr loop = (LoopAttr) block.getAttributes().get(AttributeType.LOOP);
		if (loop == null) {
			for (BlockNode b : sucList) {
				if (!b.getAttributes().contains(AttributeType.EXC_HANDLER)) {
					nodes.add(b);
				}
			}
		} else {
			for (BlockNode b : sucList) {
				if (!b.getAttributes().contains(AttributeType.EXC_HANDLER)) {
					// don't follow back edge
					if (loop.getStart() == b && loop.getEnd() == block) {
						continue;
					}
					nodes.add(b);
				}
			}
		}
		return nodes.size() == sucList.size() ? sucList : nodes;
	}

	@Override
	public List<InsnNode> getInstructions() {
		return instructions;
	}

	public int getStartOffset() {
		return startOffset;
	}

	/**
	 * Check if 'block' dominated on this node
	 */
	public boolean isDominator(BlockNode block) {
		return doms.get(block.getId());
	}

	/**
	 * Dominators of this node (exclude itself)
	 */
	public BitSet getDoms() {
		return doms;
	}

	public void setDoms(BitSet doms) {
		this.doms = doms;
	}

	public BitSet getDomFrontier() {
		return domFrontier;
	}

	public void setDomFrontier(BitSet domFrontier) {
		this.domFrontier = domFrontier;
	}

	/**
	 * Immediate dominator
	 */
	public BlockNode getIDom() {
		return idom;
	}

	public void setIDom(BlockNode idom) {
		this.idom = idom;
	}

	public List<BlockNode> getDominatesOn() {
		return dominatesOn;
	}

	public BlockRegState getStartState() {
		return startState;
	}

	public void setStartState(BlockRegState startState) {
		this.startState = startState;
	}

	public BlockRegState getEndState() {
		return endState;
	}

	public void setEndState(BlockRegState endState) {
		this.endState = endState;
	}

	public boolean isSynthetic() {
		return getAttributes().contains(AttributeFlag.SYNTHETIC);
	}

	public boolean isReturnBlock() {
		return getAttributes().contains(AttributeFlag.RETURN);
	}

	@Override
	public int hashCode() {
		return id; // TODO id can change during reindex
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (hashCode() != obj.hashCode()) {
			return false;
		}
		if (!(obj instanceof BlockNode)) {
			return false;
		}
		BlockNode other = (BlockNode) obj;
		if (id != other.id) {
			return false;
		}
		if (startOffset != other.startOffset) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "B:" + id + ":" + InsnUtils.formatOffset(startOffset);
	}
}
