package jadx.core.dex.visitors.regions;

import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.MethodNode;

public interface IRegionIterativeVisitor {

	/**
	 * If return 'true' traversal will be restarted.
	 */
	boolean visitRegion(MethodNode mth, IRegion region);
}
