package jadx.core.dex.visitors.regions;

import jadx.core.dex.nodes.IRegion;

public interface IRegionIterativeVisitor {

	/**
	 * If return 'true' traversal will be restarted.
	 */
	boolean visitRegion(IRegion region);
}
