package jadx.core.dex.attributes.nodes;

import jadx.api.plugins.input.data.attributes.PinnedAttribute;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.nodes.MethodNode;

public class MethodBridgeAttr extends PinnedAttribute {

	private final MethodNode bridgeMth;

	public MethodBridgeAttr(MethodNode bridgeMth) {
		this.bridgeMth = bridgeMth;
	}

	public MethodNode getBridgeMth() {
		return bridgeMth;
	}

	@Override
	public AType<MethodBridgeAttr> getAttrType() {
		return AType.BRIDGED_BY;
	}

	@Override
	public String toString() {
		return "BRIDGED_BY: " + bridgeMth;
	}
}
