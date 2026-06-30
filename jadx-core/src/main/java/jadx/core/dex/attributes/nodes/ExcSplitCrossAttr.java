package jadx.core.dex.attributes.nodes;

import jadx.api.plugins.input.data.attributes.IJadxAttrType;
import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.nodes.BlockNode;

/**
 * This attribute is set on the new synthetic node that BlockExceptionHandler creates at the bottom
 * of certain try regions. It stores a reference to the original path cross of the bottom of the try
 * region, so that blocks can be restructured to not pass through it when that would create an
 * erroneous loop.
 */
public class ExcSplitCrossAttr implements IJadxAttribute {

	private final BlockNode originalPathCross;

	public ExcSplitCrossAttr(BlockNode originalPathCross) {
		this.originalPathCross = originalPathCross;
	}

	public BlockNode getOriginalPathCross() {
		return this.originalPathCross;
	}

	@Override
	public IJadxAttrType<? extends IJadxAttribute> getAttrType() {
		return AType.EXC_SPLIT_CROSS;
	}

	@Override
	public String toString() {
		return "ExcSplitCross -> " + originalPathCross.toString();
	}
}
