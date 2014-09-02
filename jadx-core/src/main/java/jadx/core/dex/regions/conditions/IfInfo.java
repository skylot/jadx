package jadx.core.dex.regions.conditions;

import jadx.core.dex.nodes.BlockNode;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public final class IfInfo {
	private final IfCondition condition;
	private final Set<BlockNode> mergedBlocks;
	private final BlockNode thenBlock;
	private final BlockNode elseBlock;
	private final List<BlockNode> skipBlocks;
	private BlockNode outBlock;
	@Deprecated
	private BlockNode ifBlock;

	public IfInfo(IfCondition condition, BlockNode thenBlock, BlockNode elseBlock) {
		this(condition, thenBlock, elseBlock, new HashSet<BlockNode>(), new LinkedList<BlockNode>());
	}

	public IfInfo(IfCondition condition, IfInfo info) {
		this(condition, info.getThenBlock(), info.getElseBlock(), info.getMergedBlocks(), info.getSkipBlocks());
	}

	public IfInfo(IfInfo info, BlockNode thenBlock, BlockNode elseBlock) {
		this(info.getCondition(), thenBlock, elseBlock, info.getMergedBlocks(), info.getSkipBlocks());
	}

	private IfInfo(IfCondition condition, BlockNode thenBlock, BlockNode elseBlock,
	              Set<BlockNode> mergedBlocks, List<BlockNode> skipBlocks) {
		this.condition = condition;
		this.thenBlock = thenBlock;
		this.elseBlock = elseBlock;
		this.mergedBlocks = mergedBlocks;
		this.skipBlocks = skipBlocks;
	}

	public static IfInfo invert(IfInfo info) {
		IfCondition invertedCondition = IfCondition.invert(info.getCondition());
		IfInfo tmpIf = new IfInfo(invertedCondition,
				info.getElseBlock(), info.getThenBlock(),
				info.getMergedBlocks(), info.getSkipBlocks());
		tmpIf.setIfBlock(info.getIfBlock());
		return tmpIf;
	}

	public IfCondition getCondition() {
		return condition;
	}

	public Set<BlockNode> getMergedBlocks() {
		return mergedBlocks;
	}

	public BlockNode getThenBlock() {
		return thenBlock;
	}

	public BlockNode getElseBlock() {
		return elseBlock;
	}

	public BlockNode getOutBlock() {
		return outBlock;
	}

	public void setOutBlock(BlockNode outBlock) {
		this.outBlock = outBlock;
	}

	public List<BlockNode> getSkipBlocks() {
		return skipBlocks;
	}

	public BlockNode getIfBlock() {
		return ifBlock;
	}

	public void setIfBlock(BlockNode ifBlock) {
		this.ifBlock = ifBlock;
	}

	@Override
	public String toString() {
		return "IfInfo: " + condition + ", then: " + thenBlock + ", else: " + elseBlock;
	}
}
