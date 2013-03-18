package jadx.dex.instructions;

import jadx.dex.nodes.InsnNode;
import jadx.dex.nodes.MethodNode;
import jadx.utils.InsnUtils;

public class GotoNode extends InsnNode {

	protected int target;

	public GotoNode(MethodNode mth, int target) {
		this(mth, InsnType.GOTO, target);
	}

	protected GotoNode(MethodNode mth, InsnType type, int target) {
		super(mth, type);
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
