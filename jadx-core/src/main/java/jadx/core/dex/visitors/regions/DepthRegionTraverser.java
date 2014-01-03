package jadx.core.dex.visitors.regions;

import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.trycatch.ExceptionHandler;

public class DepthRegionTraverser {

	public static void traverse(MethodNode mth, IRegionVisitor visitor, IContainer container) {
		if (container instanceof IBlock) {
			visitor.processBlock(mth, (IBlock) container);
		} else if (container instanceof IRegion) {
			IRegion region = (IRegion) container;
			visitor.enterRegion(mth, region);
			for (IContainer subCont : region.getSubBlocks()) {
				traverse(mth, visitor, subCont);
			}
			visitor.leaveRegion(mth, region);
		}
	}

	public static void traverseAll(MethodNode mth, IRegionVisitor visitor) {
		traverse(mth, visitor, mth.getRegion());

		if (mth.getExceptionHandlers() != null) {
			for (ExceptionHandler h : mth.getExceptionHandlers()) {
				traverse(mth, visitor, h.getHandlerRegion());
			}
		}
	}
}
