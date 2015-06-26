package jadx.core.dex.visitors.regions;

import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.MethodNode;

public interface IRegionVisitor {

	void processBlock(MethodNode mth, IBlock container);

	/**
	 * @return true for traverse sub-blocks, false otherwise.
	 */
	boolean enterRegion(MethodNode mth, IRegion region);

	void leaveRegion(MethodNode mth, IRegion region);

}
