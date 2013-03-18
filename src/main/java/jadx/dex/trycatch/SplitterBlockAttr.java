package jadx.dex.trycatch;

import jadx.dex.attributes.AttributeType;
import jadx.dex.attributes.IAttribute;
import jadx.dex.nodes.BlockNode;

public class SplitterBlockAttr implements IAttribute {

	private final BlockNode block;

	public SplitterBlockAttr(BlockNode block) {
		this.block = block;
	}

	public BlockNode getBlock() {
		return block;
	}

	@Override
	public AttributeType getType() {
		return AttributeType.SPLITTER_BLOCK;
	}

	@Override
	public String toString() {
		return "Splitter: " + block;
	}

}
