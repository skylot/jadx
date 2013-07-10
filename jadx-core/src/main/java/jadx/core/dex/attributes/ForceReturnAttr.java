package jadx.core.dex.attributes;

import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.Utils;

public class ForceReturnAttr implements IAttribute {

	private final InsnNode returnInsn;

	public ForceReturnAttr(InsnNode retInsn) {
		this.returnInsn = retInsn;
	}

	public InsnNode getReturnInsn() {
		return returnInsn;
	}

	@Override
	public AttributeType getType() {
		return AttributeType.FORCE_RETURN;
	}

	@Override
	public String toString() {
		return "FORCE_RETURN " + Utils.listToString(returnInsn.getArguments());
	}

}
