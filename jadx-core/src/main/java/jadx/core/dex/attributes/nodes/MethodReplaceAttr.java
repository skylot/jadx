package jadx.core.dex.attributes.nodes;

import jadx.api.plugins.input.data.attributes.PinnedAttribute;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.nodes.MethodNode;

/**
 * Calls of method should be replaced by provided method (used for synthetic methods redirect)
 */
public class MethodReplaceAttr extends PinnedAttribute {

	private final MethodNode replaceMth;

	public MethodReplaceAttr(MethodNode replaceMth) {
		this.replaceMth = replaceMth;
	}

	public MethodNode getReplaceMth() {
		return replaceMth;
	}

	@Override
	public AType<MethodReplaceAttr> getAttrType() {
		return AType.METHOD_REPLACE;
	}

	@Override
	public String toString() {
		return "REPLACED_BY: " + replaceMth;
	}
}
