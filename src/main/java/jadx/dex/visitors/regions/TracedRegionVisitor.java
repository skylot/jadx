package jadx.dex.visitors.regions;

import jadx.dex.nodes.IBlock;
import jadx.dex.nodes.IRegion;
import jadx.dex.nodes.MethodNode;

import java.util.ArrayDeque;
import java.util.Deque;

public abstract class TracedRegionVisitor implements IRegionVisitor {

	protected final Deque<IRegion> regionStack = new ArrayDeque<IRegion>();

	@Override
	public void enterRegion(MethodNode mth, IRegion region) {
		regionStack.push(region);
	}

	@Override
	public void processBlock(MethodNode mth, IBlock container) {
		final IRegion curRegion = regionStack.peek();
		processBlockTraced(mth, container, curRegion);
	}

	public abstract void processBlockTraced(MethodNode mth, IBlock container, IRegion currentRegion);

	@Override
	public void leaveRegion(MethodNode mth, IRegion region) {
		regionStack.pop();
	}
}
