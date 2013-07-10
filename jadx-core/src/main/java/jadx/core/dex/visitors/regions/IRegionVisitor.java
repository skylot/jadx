package jadx.core.dex.visitors.regions;

import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.MethodNode;

public interface IRegionVisitor {

	public void processBlock(MethodNode mth, IBlock container);

	public void enterRegion(MethodNode mth, IRegion region);

	public void leaveRegion(MethodNode mth, IRegion region);

}
