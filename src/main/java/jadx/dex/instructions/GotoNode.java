package jadx.dex.instructions;

import jadx.dex.nodes.InsnNode;
import jadx.utils.InsnUtils;

public class GotoNode extends InsnNode {

	protected int target;

	public GotoNode(int target) {
		this(InsnType.GOTO, target);
	}

	protected GotoNode(InsnType type, int target) {
		super(type);
		this.target = target;
	}

	public int getTarget() {
		return target;
	}

	@Override
	public String toString() {
		return super.toString() + "-> " + InsnUtils.formatOffset(target);
	}
}
