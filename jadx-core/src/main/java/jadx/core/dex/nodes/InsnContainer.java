package jadx.core.dex.nodes;

import jadx.core.dex.attributes.AttrNode;

import java.util.List;

public class InsnContainer extends AttrNode implements IBlock {

	private final List<InsnNode> insns;

	public InsnContainer(List<InsnNode> insns) {
		this.insns = insns;
	}

	@Override
	public List<InsnNode> getInstructions() {
		return insns;
	}

}
