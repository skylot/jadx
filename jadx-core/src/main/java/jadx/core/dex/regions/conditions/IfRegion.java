package jadx.core.dex.regions.conditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import jadx.api.ICodeWriter;
import jadx.core.codegen.RegionGen;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IBranchRegion;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.regions.AbstractRegion;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.exceptions.CodegenException;

public final class IfRegion extends AbstractRegion implements IBranchRegion {

	private List<BlockNode> conditionBlocks;

	private IfCondition condition;
	private IContainer thenRegion;
	private IContainer elseRegion;

	public IfRegion(IRegion parent) {
		super(parent);
	}

	public IfCondition getCondition() {
		return condition;
	}

	public void setCondition(IfCondition condition) {
		this.condition = condition;
	}

	public IContainer getThenRegion() {
		return thenRegion;
	}

	public void setThenRegion(IContainer thenRegion) {
		this.thenRegion = thenRegion;
	}

	public IContainer getElseRegion() {
		return elseRegion;
	}

	public void setElseRegion(IContainer elseRegion) {
		this.elseRegion = elseRegion;
	}

	public List<BlockNode> getConditionBlocks() {
		return conditionBlocks;
	}

	public void setConditionBlocks(List<BlockNode> conditionBlocks) {
		this.conditionBlocks = conditionBlocks;
	}

	public void setConditionBlocks(Set<BlockNode> conditionBlocks) {
		List<BlockNode> list = new ArrayList<>(conditionBlocks);
		Collections.sort(list);
		this.conditionBlocks = list;
	}

	public boolean simplifyCondition() {
		IfCondition cond = IfCondition.simplify(condition);
		if (cond != condition) {
			condition = cond;
			return true;
		}
		return false;
	}

	public void invert() {
		condition = IfCondition.invert(condition);
		// swap regions
		IContainer tmp = thenRegion;
		thenRegion = elseRegion;
		elseRegion = tmp;
	}

	public int getSourceLine() {
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

	@Override
	public List<IContainer> getSubBlocks() {
		List<IContainer> all = new ArrayList<>(conditionBlocks.size() + 2);
		all.addAll(conditionBlocks);
		if (thenRegion != null) {
			all.add(thenRegion);
		}
		if (elseRegion != null) {
			all.add(elseRegion);
		}
		return Collections.unmodifiableList(all);
	}

	@Override
	public List<IContainer> getBranches() {
		List<IContainer> branches = new ArrayList<>(2);
		branches.add(thenRegion);
		branches.add(elseRegion);
		return Collections.unmodifiableList(branches);
	}

	@Override
	public boolean replaceSubBlock(IContainer oldBlock, IContainer newBlock) {
		if (oldBlock == thenRegion) {
			thenRegion = newBlock;
			updateParent(thenRegion, this);
			return true;
		}
		if (oldBlock == elseRegion) {
			elseRegion = newBlock;
			updateParent(elseRegion, this);
			return true;
		}
		return false;
	}

	@Override
	public void generate(RegionGen regionGen, ICodeWriter code) throws CodegenException {
		regionGen.makeIf(this, code, true);
	}

	@Override
	public String baseString() {
		StringBuilder sb = new StringBuilder();
		if (thenRegion != null) {
			sb.append(thenRegion.baseString());
		}
		if (elseRegion != null) {
			sb.append(elseRegion.baseString());
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return "IF " + conditionBlocks + " THEN: " + thenRegion + " ELSE: " + elseRegion;
	}
}
