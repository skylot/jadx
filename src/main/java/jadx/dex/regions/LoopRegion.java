package jadx.dex.regions;

import jadx.dex.instructions.IfNode;
import jadx.dex.instructions.args.RegisterArg;
import jadx.dex.nodes.BlockNode;
import jadx.dex.nodes.IContainer;
import jadx.dex.nodes.IRegion;
import jadx.dex.nodes.InsnNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LoopRegion extends AbstractRegion {

	// loop header contains one 'if' insn, equals null for infinite loop
	private final IfCondition condition;
	private final BlockNode conditionBlock;
	// instruction which must be executed before condition in every loop
	private BlockNode preCondition = null;
	private IContainer body;
	private final boolean conditionAtEnd;

	public LoopRegion(IRegion parent, BlockNode header, boolean reversed) {
		super(parent);
		this.conditionBlock = header;
		this.condition = IfCondition.fromIfBlock(header);
		this.conditionAtEnd = reversed;
	}

	public IfCondition getCondition() {
		return condition;
	}

	public BlockNode getHeader() {
		return conditionBlock;
	}

	public IContainer getBody() {
		return body;
	}

	public void setBody(IContainer body) {
		this.body = body;
	}

	public boolean isConditionAtEnd() {
		return conditionAtEnd;
	}

	/**
	 * Set instructions which must be executed before condition in every loop
	 */
	public void setPreCondition(BlockNode preCondition) {
		this.preCondition = preCondition;
	}

	private IfNode getIfInsn() {
		return (IfNode) conditionBlock.getInstructions().get(conditionBlock.getInstructions().size() - 1);
	}

	/**
	 * Check if pre-conditions can be inlined into loop condition
	 */
	public boolean checkPreCondition() {
		List<InsnNode> insns = preCondition.getInstructions();
		if (insns.isEmpty())
			return true;

		IfNode ifInsn = getIfInsn();
		int size = insns.size();
		for (int i = 0; i < size; i++) {
			InsnNode insn = insns.get(i);
			if (insn.getResult() == null) {
				return false;
			} else {
				RegisterArg res = insn.getResult();
				if (res.getTypedVar().getUseList().size() > 2)
					return false;

				boolean found = false;
				// search result arg in other insns
				for (int j = i + 1; j < size; j++) {
					if (insns.get(i).containsArg(res))
						found = true;
				}
				// or in if insn
				if (!found && ifInsn.containsArg(res))
					found = true;

				if (!found)
					return false;
			}
		}
		return true;
	}

	/**
	 * Move all preCondition block instructions before conditionBlock instructions
	 */
	public void mergePreCondition() {
		if (preCondition != null && conditionBlock != null) {
			preCondition.getInstructions().addAll(conditionBlock.getInstructions());
			conditionBlock.getInstructions().clear();
			conditionBlock.getInstructions().addAll(preCondition.getInstructions());
			preCondition.getInstructions().clear();
		}
	}

	@Override
	public List<IContainer> getSubBlocks() {
		List<IContainer> all = new ArrayList<IContainer>(3);
		if (preCondition != null)
			all.add(preCondition);
		if (conditionBlock != null)
			all.add(conditionBlock);
		all.add(body);
		return Collections.unmodifiableList(all);
	}

	@Override
	public String toString() {
		return "LOOP";
	}
}
