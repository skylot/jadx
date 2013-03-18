package jadx.dex.regions;

import jadx.dex.nodes.BlockNode;
import jadx.dex.nodes.IContainer;
import jadx.dex.nodes.IRegion;

import java.util.ArrayList;
import java.util.List;

public final class Region extends AbstractRegion {

	private final List<IContainer> blocks;

	public Region(IRegion parent) {
		super(parent);
		this.blocks = new ArrayList<IContainer>(1);
	}

	@Override
	public List<IContainer> getSubBlocks() {
		return blocks;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("R:");
		sb.append(blocks.size());
		if (blocks.size() != 0) {
			for (IContainer cont : blocks) {
				if (cont instanceof BlockNode)
					sb.append(((BlockNode) cont).getId());
			}
		}
		return sb.toString();
	}

}
