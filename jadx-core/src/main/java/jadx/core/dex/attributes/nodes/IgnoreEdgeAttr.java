package jadx.core.dex.attributes.nodes;

import java.util.HashSet;
import java.util.Set;

import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.utils.Utils;

public class IgnoreEdgeAttr implements IJadxAttribute {

	private final Set<BlockNode> blocks = new HashSet<>(3);

	public Set<BlockNode> getBlocks() {
		return blocks;
	}

	public boolean contains(BlockNode block) {
		return blocks.contains(block);
	}

	@Override
	public AType<IgnoreEdgeAttr> getAttrType() {
		return AType.IGNORE_EDGE;
	}

	@Override
	public String toString() {
		return "IGNORE_EDGES: " + Utils.listToString(blocks);
	}
}
