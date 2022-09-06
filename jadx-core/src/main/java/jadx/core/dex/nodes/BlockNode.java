package jadx.core.dex.nodes;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.AttrNode;
import jadx.core.dex.attributes.nodes.LoopInfo;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.EmptyBitSet;
import jadx.core.utils.InsnUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static jadx.core.utils.Utils.lockList;

public final class BlockNode extends AttrNode implements IBlock, Comparable<BlockNode> {

	/**
	 * Const ID
	 */
	private final int cid;

	/**
	 * ID linked to position in blocks list (easier to use BitSet)
	 * TODO: rename to avoid confusion
	 */
	private int id;

	/**
	 * Offset in methods bytecode
	 */
	private final int startOffset;

	private final List<InsnNode> instructions = new ArrayList<>(2);

	private List<BlockNode> predecessors = new ArrayList<>(1);
	private List<BlockNode> successors = new ArrayList<>(1);
	private List<BlockNode> cleanSuccessors;

	/**
	 * All dominators, excluding self
	 */
	private BitSet doms = EmptyBitSet.EMPTY;

	/**
	 * Dominance frontier
	 */
	private BitSet domFrontier;

	/**
	 * Immediate dominator
	 */
	private BlockNode idom;

	/**
	 * Blocks on which dominates this block
	 */
	private List<BlockNode> dominatesOn = new ArrayList<>(3);

	public BlockNode(int cid, int id, int offset) {
		this.cid = cid;
		this.id = id;
		this.startOffset = offset;
	}

	public int getCId() {
		return cid;
	}

	void setId(int id) {
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
		return this.cleanSuccessors;
	}

	public void updateCleanSuccessors() {
		cleanSuccessors = cleanSuccessors(this);
	}

	public void lock() {
		try {
			List<BlockNode> successorsList = successors;
			successors = lockList(successorsList);
			cleanSuccessors = successorsList == cleanSuccessors ? this.successors : lockList(cleanSuccessors);
			predecessors = lockList(predecessors);
			dominatesOn = lockList(dominatesOn);
			if (domFrontier == null) {
				throw new JadxRuntimeException("Dominance frontier not set for block: " + this);
			}
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to lock block: " + this, e);
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
			if (BlockUtils.isExceptionHandlerPath(b)) {
				toRemove.add(b);
			}
		}
		if (block.contains(AFlag.LOOP_END)) {
			List<LoopInfo> loops = block.getAll(AType.LOOP);
			for (LoopInfo loop : loops) {
				toRemove.add(loop.getStart());
			}
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

	public boolean isEmpty() {
		return instructions.isEmpty();
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
		return cid == other.cid && startOffset == other.startOffset;
	}

	@Override
	public int compareTo(@NotNull BlockNode o) {
		return Integer.compare(cid, o.cid);
	}

	@Override
	public String baseString() {
		return Integer.toString(id);
	}

	@Override
	public String toString() {
		return "B:" + cid + ':' + InsnUtils.formatOffset(startOffset);
	}
}
