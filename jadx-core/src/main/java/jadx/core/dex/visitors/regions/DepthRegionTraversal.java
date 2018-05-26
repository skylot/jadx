package jadx.core.dex.visitors.regions;

import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.utils.exceptions.JadxOverflowException;

public class DepthRegionTraversal {

	private static final int ITERATIVE_LIMIT = 500;

	private DepthRegionTraversal() {
	}

	public static void traverse(MethodNode mth, IRegionVisitor visitor) {
		traverseInternal(mth, visitor, mth.getRegion());
	}

	public static void traverseIterative(MethodNode mth, IRegionIterativeVisitor visitor) {
		boolean repeat;
		int k = 0;
		do {
			repeat = traverseIterativeStepInternal(mth, visitor, mth.getRegion());
			if (k++ > ITERATIVE_LIMIT) {
				throw new JadxOverflowException("Iterative traversal limit reached, method: " + mth);
			}
		} while (repeat);
	}

	public static void traverseIncludingExcHandlers(MethodNode mth, IRegionIterativeVisitor visitor) {
		boolean repeat;
		int k = 0;
		do {
			repeat = traverseIterativeStepInternal(mth, visitor, mth.getRegion());
			if (!repeat) {
				for (ExceptionHandler h : mth.getExceptionHandlers()) {
					repeat = traverseIterativeStepInternal(mth, visitor, h.getHandlerRegion());
					if (repeat) {
						break;
					}
				}
			}
			if (k++ > ITERATIVE_LIMIT) {
				throw new JadxOverflowException("Iterative traversal limit reached, method: " + mth);
			}
		} while (repeat);
	}

	private static void traverseInternal(MethodNode mth, IRegionVisitor visitor, IContainer container) {
		if (container instanceof IBlock) {
			visitor.processBlock(mth, (IBlock) container);
		} else if (container instanceof IRegion) {
			IRegion region = (IRegion) container;
			if (visitor.enterRegion(mth, region)) {
				region.getSubBlocks().forEach(subCont -> traverseInternal(mth, visitor, subCont));
			}
			visitor.leaveRegion(mth, region);
		}
	}

	private static boolean traverseIterativeStepInternal(MethodNode mth, IRegionIterativeVisitor visitor,
			IContainer container) {
		if (container instanceof IRegion) {
			IRegion region = (IRegion) container;
			if (visitor.visitRegion(mth, region)) {
				return true;
			}
			for (IContainer subCont : region.getSubBlocks()) {
				if (traverseIterativeStepInternal(mth, visitor, subCont)) {
					return true;
				}
			}
		}
		return false;
	}
}
