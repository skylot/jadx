package jadx.core.dex.visitors.regions;

import java.util.ArrayDeque;
import java.util.Deque;

import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.MethodNode;

public abstract class TracedRegionVisitor implements IRegionVisitor {

	protected final Deque<IRegion> regionStack = new ArrayDeque<>();

	@Override
	public boolean enterRegion(MethodNode mth, IRegion region) {
		regionStack.push(region);
		return true;
	}

	@Override
	public void processBlock(MethodNode mth, IBlock block) {
		IRegion curRegion = regionStack.peek();
		processBlockTraced(mth, block, curRegion);
	}

	public abstract void processBlockTraced(MethodNode mth, IBlock block, IRegion parentRegion);

	@Override
	public void leaveRegion(MethodNode mth, IRegion region) {
		regionStack.pop();
	}
}
