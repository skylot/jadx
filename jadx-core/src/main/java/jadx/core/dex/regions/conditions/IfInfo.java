package jadx.core.dex.regions.conditions;

import jadx.core.dex.nodes.BlockNode;

import java.util.HashSet;
import java.util.Set;

public final class IfInfo {
	private final IfCondition condition;
	private final Set<BlockNode> mergedBlocks = new HashSet<BlockNode>();
	private final BlockNode thenBlock;
	private final BlockNode elseBlock;
	private BlockNode outBlock;
	@Deprecated
	private BlockNode ifBlock;

	public IfInfo(IfCondition condition, BlockNode thenBlock, BlockNode elseBlock) {
		this.condition = condition;
		this.thenBlock = thenBlock;
		this.elseBlock = elseBlock;
	}

	public IfInfo(IfCondition condition, IfInfo info) {
		this.condition = condition;
		this.thenBlock = info.getThenBlock();
		this.elseBlock = info.getElseBlock();
		this.mergedBlocks.addAll(info.getMergedBlocks());
	}

	public static IfInfo invert(IfInfo info) {
		IfInfo tmpIf = new IfInfo(IfCondition.invert(info.getCondition()),
				info.getElseBlock(), info.getThenBlock());
		tmpIf.setIfBlock(info.getIfBlock());
		tmpIf.getMergedBlocks().addAll(info.getMergedBlocks());
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
