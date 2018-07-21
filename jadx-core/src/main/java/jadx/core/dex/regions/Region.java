package jadx.core.dex.regions;

import java.util.ArrayList;
import java.util.List;

import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.utils.Utils;

public final class Region extends AbstractRegion {

	private final List<IContainer> blocks;

	public Region(IRegion parent) {
		super(parent);
		this.blocks = new ArrayList<>(1);
	}

	@Override
	public List<IContainer> getSubBlocks() {
		return blocks;
	}

	public void add(IContainer region) {
		updateParent(region, this);
		blocks.add(region);
	}

	@Override
	public boolean replaceSubBlock(IContainer oldBlock, IContainer newBlock) {
		int i = blocks.indexOf(oldBlock);
		if (i != -1) {
			blocks.set(i, newBlock);
			updateParent(newBlock, this);
			return true;
		}
		return false;
	}

	@Override
	public String baseString() {
		StringBuilder sb = new StringBuilder();
		int size = blocks.size();
		sb.append('(');
		sb.append(size);
		if (size > 0) {
			sb.append(':');
			Utils.listToString(sb, blocks, "|", IContainer::baseString);
		}
		sb.append(')');
		return sb.toString();
	}

	@Override
	public String toString() {
		return "R" + baseString();
	}
}
