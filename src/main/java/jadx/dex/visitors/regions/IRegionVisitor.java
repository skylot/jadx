package jadx.dex.visitors.regions;

import jadx.dex.nodes.IBlock;
import jadx.dex.nodes.IRegion;
import jadx.dex.nodes.MethodNode;

public interface IRegionVisitor {

	public void processBlock(MethodNode mth, IBlock container);

	public void enterRegion(MethodNode mth, IRegion region);

	public void leaveRegion(MethodNode mth, IRegion region);

}
