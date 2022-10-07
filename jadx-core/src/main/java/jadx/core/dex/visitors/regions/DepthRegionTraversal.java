package jadx.core.dex.visitors.regions;

import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.utils.exceptions.JadxOverflowException;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class DepthRegionTraversal {

	private static final int ITERATIVE_LIMIT_MULTIPLIER = 5;

	private DepthRegionTraversal() {
	}

	public static void traverse(MethodNode mth, IRegionVisitor visitor) {
		traverseInternal(mth, visitor, mth.getRegion());
	}

	public static void traverse(MethodNode mth, IContainer container, IRegionVisitor visitor) {
		traverseInternal(mth, visitor, container);
	}

	public static void traverseIterative(MethodNode mth, IRegionIterativeVisitor visitor) {
		boolean repeat;
		int k = 0;
		int limit = ITERATIVE_LIMIT_MULTIPLIER * mth.getBasicBlocks().size();
		do {
			repeat = traverseIterativeStepInternal(mth, visitor, mth.getRegion());
			if (k++ > limit) {
				throw new JadxRuntimeException("Iterative traversal limit reached: "
						+ "limit: " + limit + ", visitor: " + visitor.getClass().getName()
						+ ", blocks count: " + mth.getBasicBlocks().size());
			}
		} while (repeat);
	}

	public static void traverseIncludingExcHandlers(MethodNode mth, IRegionIterativeVisitor visitor) {
		boolean repeat;
		int k = 0;
		int limit = ITERATIVE_LIMIT_MULTIPLIER * mth.getBasicBlocks().size();
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
			if (k++ > limit) {
				throw new JadxRuntimeException("Iterative traversal limit reached: "
						+ "limit: " + limit + ", visitor: " + visitor.getClass().getName()
						+ ", blocks count: " + mth.getBasicBlocks().size());
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

	private static boolean traverseIterativeStepInternal(MethodNode mth, IRegionIterativeVisitor visitor, IContainer container) {
		if (container instanceof IRegion) {
			IRegion region = (IRegion) container;
			if (visitor.visitRegion(mth, region)) {
				return true;
			}
			for (IContainer subCont : region.getSubBlocks()) {
				try {
					if (traverseIterativeStepInternal(mth, visitor, subCont)) {
						return true;
					}
				} catch (StackOverflowError overflow) {
					throw new JadxOverflowException("Region traversal failed: Recursive call in traverseIterativeStepInternal method");
				}
			}
		}
		return false;
	}
}
