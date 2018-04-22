package jadx.core.dex.regions.loops;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.attributes.nodes.LoopInfo;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.regions.AbstractRegion;
import jadx.core.dex.regions.conditions.IfCondition;

public final class LoopRegion extends AbstractRegion {

	private final LoopInfo info;
	/**
	 * loop header contains one 'if' insn, equals null for infinite loop
	 */
	@Nullable
	private IfCondition condition;
	private final BlockNode conditionBlock;
	// instruction which must be executed before condition in every loop
	private BlockNode preCondition;
	private IRegion body;
	private final boolean conditionAtEnd;

	private LoopType type;

	public LoopRegion(IRegion parent, LoopInfo info, @Nullable BlockNode header, boolean reversed) {
		super(parent);
		this.info = info;
		this.conditionBlock = header;
		this.condition = IfCondition.fromIfBlock(header);
		this.conditionAtEnd = reversed;
	}

	public LoopInfo getInfo() {
		return info;
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

	public IRegion getBody() {
		return body;
	}

	public void setBody(IRegion body) {
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
			}
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

	public int getConditionSourceLine() {
		if (conditionBlock != null) {
			List<InsnNode> condInsns = conditionBlock.getInstructions();
			if (!condInsns.isEmpty()) {
				return condInsns.get(0).getSourceLine();
			}
		}
		return 0;
	}

	public LoopType getType() {
		return type;
	}

	public void setType(LoopType type) {
		this.type = type;
	}

	@Override
	public List<IContainer> getSubBlocks() {
		List<IContainer> all = new ArrayList<>(3);
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
	public boolean replaceSubBlock(IContainer oldBlock, IContainer newBlock) {
		return false;
	}

	@Override
	public String baseString() {
		return body == null ? "-" : body.baseString();
	}

	@Override
	public String toString() {
		return "LOOP:" + info.getId() + ": " + baseString();
	}
}
