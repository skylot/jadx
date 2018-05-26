package jadx.core.dex.visitors.regions;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.Region;

public class CleanRegions {

	public static void process(MethodNode mth) {
		if (mth.isNoCode() || mth.getBasicBlocks().isEmpty()) {
			return;
		}
		IRegionVisitor removeEmptyBlocks = new AbstractRegionVisitor() {
			@Override
			public boolean enterRegion(MethodNode mth, IRegion region) {
				if (region instanceof Region) {
					region.getSubBlocks().removeIf(container -> {
						if (container instanceof BlockNode) {
							BlockNode block = (BlockNode) container;
							return block.getInstructions().isEmpty();
						}
						return false;
					});
				}
				return true;
			}
		};
		DepthRegionTraversal.traverse(mth, removeEmptyBlocks);
	}

	private CleanRegions() {
	}
}
