package jadx.dex.nodes;

import jadx.dex.attributes.AttrNode;

import java.util.List;

public class InsnContainer extends AttrNode implements IBlock {

	private List<InsnNode> insns;

	public void setInstructions(List<InsnNode> insns) {
		this.insns = insns;
	}

	@Override
	public List<InsnNode> getInstructions() {
		return insns;
	}

}
