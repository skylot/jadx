package jadx.core.dex.regions;

import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LoopRegion extends AbstractRegion {

	// loop header contains one 'if' insn, equals null for infinite loop
	private IfCondition condition;
	private final BlockNode conditionBlock;
	// instruction which must be executed before condition in every loop
	private BlockNode preCondition;
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

	public void setCondition(IfCondition condition) {
		this.condition = condition;
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
		return (IfNode) conditionBlock.getInstructions().get(0);
	}

	/**
	 * Check if pre-conditions can be inlined into loop condition
	 */
	public boolean checkPreCondition() {
		List<InsnNode> insns = preCondition.getInstructions();
		if (insns.isEmpty()) {
			return true;
		}
		IfNode ifInsn = getIfInsn();
		int size = insns.size();
		for (int i = 0; i < size; i++) {
			InsnNode insn = insns.get(i);
			if (insn.getResult() == null) {
				return false;
			} else {
				RegisterArg res = insn.getResult();
				if (res.getSVar().getUseCount() > 1) {
					return false;
				}
				boolean found = false;
				// search result arg in other insns
				for (int j = i + 1; j < size; j++) {
					if (insns.get(i).containsArg(res)) {
						found = true;
					}
				}
				// or in if insn
				if (!found && ifInsn.containsArg(res)) {
					found = true;
				}
				if (!found) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Move all preCondition block instructions before conditionBlock instructions
	 */
	public void mergePreCondition() {
		if (preCondition != null && conditionBlock != null) {
			List<InsnNode> condInsns = conditionBlock.getInstructions();
			List<InsnNode> preCondInsns = preCondition.getInstructions();
			preCondInsns.addAll(condInsns);
			condInsns.clear();
			condInsns.addAll(preCondInsns);
			preCondInsns.clear();
			preCondition = null;
		}
	}

	@Override
	public List<IContainer> getSubBlocks() {
		List<IContainer> all = new ArrayList<IContainer>(3);
		if (preCondition != null) {
			all.add(preCondition);
		}
		if (conditionBlock != null) {
			all.add(conditionBlock);
		}
		if (body != null) {
			all.add(body);
		}
		return Collections.unmodifiableList(all);
	}

	@Override
	public String baseString() {
		return body.baseString();
	}

	@Override
	public String toString() {
		return "LOOP";
	}
}
