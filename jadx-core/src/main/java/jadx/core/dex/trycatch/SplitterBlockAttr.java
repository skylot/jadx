package jadx.core.dex.trycatch;

import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.nodes.BlockNode;

public class SplitterBlockAttr implements IJadxAttribute {

	private final BlockNode block;

	public SplitterBlockAttr(BlockNode block) {
		this.block = block;
	}

	public BlockNode getBlock() {
		return block;
	}

	@Override
	public AType<SplitterBlockAttr> getAttrType() {
		return AType.SPLITTER_BLOCK;
	}

	@Override
	public String toString() {
		return "Splitter:" + block;
	}
}
