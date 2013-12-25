package jadx.core.dex.visitors.regions;

import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.LoopRegion;

public class ProcessLoopRegions extends AbstractRegionVisitor {

	@Override
	public void enterRegion(MethodNode mth, IRegion region) {
		if (region instanceof LoopRegion) {
			LoopRegion loop = (LoopRegion) region;
			loop.mergePreCondition();
		}
	}
}
