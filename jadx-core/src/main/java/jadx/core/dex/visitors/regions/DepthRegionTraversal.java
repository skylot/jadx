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

	public static void traverseAll(MethodNode mth, IRegionVisitor visitor) {
		traverseInternal(mth, visitor, mth.getRegion());
		for (ExceptionHandler h : mth.getExceptionHandlers()) {
			traverseInternal(mth, visitor, h.getHandlerRegion());
		}
	}

	public static void traverseAllIterative(MethodNode mth, IRegionIterativeVisitor visitor) {
		boolean repeat;
		int k = 0;
		do {
			repeat = traverseAllIterativeInternal(mth, visitor);
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
			visitor.enterRegion(mth, region);
			for (IContainer subCont : region.getSubBlocks()) {
				traverseInternal(mth, visitor, subCont);
			}
			visitor.leaveRegion(mth, region);
		}
	}

	private static boolean traverseAllIterativeInternal(MethodNode mth, IRegionIterativeVisitor visitor) {
		if (traverseIterativeInternal(mth, visitor, mth.getRegion())) {
			return true;
		}
		for (ExceptionHandler h : mth.getExceptionHandlers()) {
			if (traverseIterativeInternal(mth, visitor, h.getHandlerRegion())) {
				return true;
			}
		}
		return false;
	}

	private static boolean traverseIterativeInternal(MethodNode mth, IRegionIterativeVisitor visitor,
			IContainer container) {
		if (container instanceof IRegion) {
			IRegion region = (IRegion) container;
			if (visitor.visitRegion(mth, region)) {
				return true;
			}
			for (IContainer subCont : region.getSubBlocks()) {
				if (traverseIterativeInternal(mth, visitor, subCont)) {
					return true;
				}
			}
		}
		return false;
	}
}
