package jadx.core.dex.regions.conditions;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IConditionRegion;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.regions.AbstractRegion;
import jadx.core.utils.BlockUtils;

public abstract class ConditionRegion extends AbstractRegion implements IConditionRegion {

	@Nullable
	private IfCondition condition;
	private List<BlockNode> conditionBlocks = Collections.emptyList();

	public ConditionRegion(IRegion parent) {
		super(parent);
	}

	@Override
	@Nullable
	public IfCondition getCondition() {
		return condition;
	}

	@Override
	public List<BlockNode> getConditionBlocks() {
		return conditionBlocks;
	}

	@Override
	public void invertCondition() {
		if (condition != null) {
			condition = IfCondition.invert(condition);
		}
	}

	@Override
	public boolean simplifyCondition() {
		if (condition == null) {
			return false;
		}
		IfCondition updated = IfCondition.simplify(condition);
		if (updated != condition) {
			condition = updated;
			return true;
		}
		return false;
	}

	@Override
	public int getConditionSourceLine() {
		for (BlockNode block : conditionBlocks) {
			InsnNode lastInsn = BlockUtils.getLastInsn(block);
			if (lastInsn != null) {
				int sourceLine = lastInsn.getSourceLine();
				if (sourceLine != 0) {
					return sourceLine;
				}
			}
		}
		return 0;
	}

	/**
	 * Prefer way for update condition info
	 */
	public void updateCondition(IfInfo info) {
		this.condition = info.getCondition();
		this.conditionBlocks = info.getMergedBlocks();
	}

	public void updateCondition(IfCondition condition, List<BlockNode> conditionBlocks) {
		this.condition = condition;
		this.conditionBlocks = conditionBlocks;
	}

	public void updateCondition(BlockNode block) {
		this.condition = IfCondition.fromIfBlock(block);
		this.conditionBlocks = Collections.singletonList(block);
	}
}
