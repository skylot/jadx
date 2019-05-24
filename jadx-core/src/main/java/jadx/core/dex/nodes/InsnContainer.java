package jadx.core.dex.nodes;

import java.util.Collections;
import java.util.List;

import jadx.core.dex.attributes.AttrNode;

/**
 * Lightweight replacement for BlockNode in regions.
 * Use with caution! Some passes still expect BlockNode in method blocks list (mth.getBlockNodes())
 */
public final class InsnContainer extends AttrNode implements IBlock {

	private final List<InsnNode> insns;

	public InsnContainer(InsnNode insn) {
		this.insns = Collections.singletonList(insn);
	}

	public InsnContainer(List<InsnNode> insns) {
		this.insns = insns;
	}

	@Override
	public List<InsnNode> getInstructions() {
		return insns;
	}

	@Override
	public String baseString() {
		return Integer.toString(insns.size());
	}

	@Override
	public String toString() {
		return "InsnContainer:" + insns.size();
	}
}
