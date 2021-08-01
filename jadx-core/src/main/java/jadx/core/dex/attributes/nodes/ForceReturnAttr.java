package jadx.core.dex.attributes.nodes;

import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.Utils;

public class ForceReturnAttr implements IJadxAttribute {

	private final InsnNode returnInsn;

	public ForceReturnAttr(InsnNode retInsn) {
		this.returnInsn = retInsn;
	}

	public InsnNode getReturnInsn() {
		return returnInsn;
	}

	@Override
	public AType<ForceReturnAttr> getAttrType() {
		return AType.FORCE_RETURN;
	}

	@Override
	public String toString() {
		return "FORCE_RETURN " + Utils.listToString(returnInsn.getArguments());
	}
}
