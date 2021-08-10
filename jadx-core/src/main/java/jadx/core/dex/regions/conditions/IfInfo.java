package jadx.core.dex.regions.conditions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;

public final class IfInfo {
	private final MethodNode mth;
	private final IfCondition condition;
	private final List<BlockNode> mergedBlocks;
	private final BlockNode thenBlock;
	private final BlockNode elseBlock;
	private final Set<BlockNode> skipBlocks;
	private final List<InsnNode> forceInlineInsns;
	private BlockNode outBlock;

	public IfInfo(MethodNode mth, IfCondition condition, BlockNode thenBlock, BlockNode elseBlock) {
		this(mth, condition, thenBlock, elseBlock, new ArrayList<>(), new HashSet<>(), new ArrayList<>());
	}

	public IfInfo(IfInfo info, BlockNode thenBlock, BlockNode elseBlock) {
		this(info.getMth(), info.getCondition(), thenBlock, elseBlock,
				info.getMergedBlocks(), info.getSkipBlocks(), info.getForceInlineInsns());
	}

	private IfInfo(MethodNode mth, IfCondition condition, BlockNode thenBlock, BlockNode elseBlock,
			List<BlockNode> mergedBlocks, Set<BlockNode> skipBlocks, List<InsnNode> forceInlineInsns) {
		this.mth = mth;
		this.condition = condition;
		this.thenBlock = thenBlock;
		this.elseBlock = elseBlock;
		this.mergedBlocks = mergedBlocks;
		this.skipBlocks = skipBlocks;
		this.forceInlineInsns = forceInlineInsns;
	}

	public static IfInfo invert(IfInfo info) {
		return new IfInfo(info.getMth(),
				IfCondition.invert(info.getCondition()),
				info.getElseBlock(), info.getThenBlock(),
				info.getMergedBlocks(), info.getSkipBlocks(), info.getForceInlineInsns());
	}

	public void merge(IfInfo... arr) {
		for (IfInfo info : arr) {
			mergedBlocks.addAll(info.getMergedBlocks());
			skipBlocks.addAll(info.getSkipBlocks());
			addInsnsForForcedInline(info.getForceInlineInsns());
		}
	}

	@Deprecated
	public BlockNode getFirstIfBlock() {
		return mergedBlocks.get(0);
	}

	public MethodNode getMth() {
		return mth;
	}

	public IfCondition getCondition() {
		return condition;
	}

	public List<BlockNode> getMergedBlocks() {
		return mergedBlocks;
	}

	public Set<BlockNode> getSkipBlocks() {
		return skipBlocks;
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

	public List<InsnNode> getForceInlineInsns() {
		return forceInlineInsns;
	}

	public void resetForceInlineInsns() {
		forceInlineInsns.clear();
	}

	public void addInsnsForForcedInline(List<InsnNode> insns) {
		forceInlineInsns.addAll(insns);
	}

	@Override
	public String toString() {
		return "IfInfo: then: " + thenBlock + ", else: " + elseBlock;
	}
}
