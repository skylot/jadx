package jadx.core.dex.instructions;

import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.InsnUtils;

public class GotoNode extends InsnNode {

	protected int target;

	public GotoNode(int target) {
		this(InsnType.GOTO, target, 0);
	}

	protected GotoNode(InsnType type, int target, int argsCount) {
		super(type, argsCount);
		this.target = target;
	}

	public int getTarget() {
		return target;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof GotoNode) || !super.equals(obj)) {
			return false;
		}
		GotoNode gotoNode = (GotoNode) obj;
		return target == gotoNode.target;
	}

	@Override
	public int hashCode() {
		return 31 * super.hashCode() + target;
	}

	@Override
	public String toString() {
		return super.toString() + "-> " + InsnUtils.formatOffset(target);
	}
}
