package jadx.core.dex.nodes;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.AttrNode;
import jadx.core.dex.attributes.nodes.IgnoreEdgeAttr;
import jadx.core.dex.attributes.nodes.LoopInfo;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.EmptyBitSet;
import jadx.core.utils.InsnUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static jadx.core.utils.Utils.lockList;

public class BlockNode extends AttrNode implements IBlock {

	private int id;
	private final int startOffset;
	private final List<InsnNode> instructions = new ArrayList<>(2);

	private List<BlockNode> predecessors = new ArrayList<>(1);
	private List<BlockNode> successors = new ArrayList<>(1);
	private List<BlockNode> cleanSuccessors;

	// all dominators
	private BitSet doms = EmptyBitSet.EMPTY;
	// dominance frontier
	private BitSet domFrontier;
	// immediate dominator
	private BlockNode idom;
	// blocks on which dominates this block
	private List<BlockNode> dominatesOn = new ArrayList<>(3);

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
		cleanSuccessors = lockList(cleanSuccessors);
		successors = lockList(successors);
		predecessors = lockList(predecessors);
		dominatesOn = lockList(dominatesOn);
		if (domFrontier == null) {
			throw new JadxRuntimeException("Dominance frontier not set for block: " + this);
		}
	}

	/**
	 * Return all successor which are not exception handler or followed by loop back edge
	 */
	private static List<BlockNode> cleanSuccessors(BlockNode block) {
		List<BlockNode> sucList = block.getSuccessors();
		if (sucList.isEmpty()) {
			return sucList;
		}
		List<BlockNode> toRemove = new ArrayList<>(sucList.size());
		for (BlockNode b : sucList) {
			if (BlockUtils.isBlockMustBeCleared(b)) {
				toRemove.add(b);
			}
		}
		if (block.contains(AFlag.LOOP_END)) {
			List<LoopInfo> loops = block.getAll(AType.LOOP);
			for (LoopInfo loop : loops) {
				toRemove.add(loop.getStart());
			}
		}
		IgnoreEdgeAttr ignoreEdgeAttr = block.get(AType.IGNORE_EDGE);
		if (ignoreEdgeAttr != null) {
			toRemove.addAll(ignoreEdgeAttr.getBlocks());
		}
		if (toRemove.isEmpty()) {
			return sucList;
		}
		List<BlockNode> result = new ArrayList<>(sucList);
		result.removeAll(toRemove);
		return result;
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

	public void addDominatesOn(BlockNode block) {
		dominatesOn.add(block);
	}

	public boolean isSynthetic() {
		return contains(AFlag.SYNTHETIC);
	}

	public boolean isReturnBlock() {
		return contains(AFlag.RETURN);
	}

	@Override
	public int hashCode() {
		return startOffset;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof BlockNode)) {
			return false;
		}
		BlockNode other = (BlockNode) obj;
		return id == other.id && startOffset == other.startOffset;
	}

	@Override
	public String baseString() {
		return Integer.toString(id);
	}

	@Override
	public String toString() {
		return "B:" + id + ":" + InsnUtils.formatOffset(startOffset);
	}
}
