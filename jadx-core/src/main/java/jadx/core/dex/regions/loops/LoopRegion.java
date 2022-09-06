package jadx.core.dex.regions.loops;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.api.ICodeWriter;
import jadx.core.codegen.RegionGen;
import jadx.core.dex.attributes.nodes.LoopInfo;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.regions.conditions.ConditionRegion;
import jadx.core.dex.regions.conditions.IfCondition;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.InsnUtils;
import jadx.core.utils.exceptions.CodegenException;

public final class LoopRegion extends ConditionRegion {

	private final LoopInfo info;
	private final boolean conditionAtEnd;
	private final @Nullable BlockNode header;
	// instruction which must be executed before condition in every loop
	private @Nullable BlockNode preCondition;

	private IRegion body;
	private LoopType type;

	public LoopRegion(IRegion parent, LoopInfo info, @Nullable BlockNode header, boolean reversed) {
		super(parent);
		this.info = info;
		this.header = header;
		this.conditionAtEnd = reversed;
		if (header != null) {
			updateCondition(header);
		}
	}

	public LoopInfo getInfo() {
		return info;
	}

	@Nullable
	public BlockNode getHeader() {
		return header;
	}

	public boolean isEndless() {
		return header == null;
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

	/**
	 * Check if pre-conditions can be inlined into loop condition
	 */
	public boolean checkPreCondition() {
		List<InsnNode> insns = preCondition.getInstructions();
		if (insns.isEmpty()) {
			return true;
		}
		IfCondition condition = getCondition();
		if (condition == null) {
			return false;
		}
		List<RegisterArg> conditionArgs = condition.getRegisterArgs();
		if (conditionArgs.isEmpty()) {
			return false;
		}
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
				if (insns.get(i).containsVar(res)) {
					found = true;
				}
			}
			// or in if insn
			if (!found && InsnUtils.containsVar(conditionArgs, res)) {
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
		if (preCondition != null && header != null) {
			List<InsnNode> condInsns = header.getInstructions();
			List<InsnNode> preCondInsns = preCondition.getInstructions();
			preCondInsns.addAll(condInsns);
			condInsns.clear();
			condInsns.addAll(preCondInsns);
			preCondInsns.clear();
			preCondition = null;
		}
	}

	public int getSourceLine() {
		InsnNode lastInsn = BlockUtils.getLastInsn(header);
		int headerLine = lastInsn == null ? 0 : lastInsn.getSourceLine();
		if (headerLine != 0) {
			return headerLine;
		}
		return getConditionSourceLine();
	}

	public LoopType getType() {
		return type;
	}

	public void setType(LoopType type) {
		this.type = type;
	}

	@Override
	public List<IContainer> getSubBlocks() {
		List<IContainer> all = new ArrayList<>(2 + getConditionBlocks().size());
		if (preCondition != null) {
			all.add(preCondition);
		}
		all.addAll(getConditionBlocks());
		if (body != null) {
			all.add(body);
		}
		return all;
	}

	@Override
	public boolean replaceSubBlock(IContainer oldBlock, IContainer newBlock) {
		return false;
	}

	@Override
	public void generate(RegionGen regionGen, ICodeWriter code) throws CodegenException {
		regionGen.makeLoop(this, code);
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
