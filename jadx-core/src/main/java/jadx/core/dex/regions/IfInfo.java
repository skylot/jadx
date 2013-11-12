package jadx.core.dex.regions;

import jadx.core.dex.nodes.BlockNode;

public final class IfInfo {
	IfCondition condition;
	BlockNode ifnode;
	BlockNode thenBlock;
	BlockNode elseBlock;

	public IfCondition getCondition() {
		return condition;
	}

	public void setCondition(IfCondition condition) {
		this.condition = condition;
	}

	public BlockNode getIfnode() {
		return ifnode;
	}

	public void setIfnode(BlockNode ifnode) {
		this.ifnode = ifnode;
	}

	public BlockNode getThenBlock() {
		return thenBlock;
	}

	public void setThenBlock(BlockNode thenBlock) {
		this.thenBlock = thenBlock;
	}

	public BlockNode getElseBlock() {
		return elseBlock;
	}

	public void setElseBlock(BlockNode elseBlock) {
		this.elseBlock = elseBlock;
	}
}
