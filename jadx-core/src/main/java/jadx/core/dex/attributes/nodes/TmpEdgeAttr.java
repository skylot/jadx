package jadx.core.dex.attributes.nodes;

import jadx.api.plugins.input.data.attributes.IJadxAttrType;
import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.nodes.BlockNode;

public class TmpEdgeAttr implements IJadxAttribute {

	private final BlockNode block;

	public TmpEdgeAttr(BlockNode block) {
		this.block = block;
	}

	public BlockNode getBlock() {
		return block;
	}

	@Override
	public IJadxAttrType<? extends IJadxAttribute> getAttrType() {
		return AType.TMP_EDGE;
	}

	@Override
	public String toString() {
		return "TMP_EDGE: " + block;
	}
}
