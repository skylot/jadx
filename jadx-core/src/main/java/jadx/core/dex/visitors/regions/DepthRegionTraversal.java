package jadx.core.dex.visitors.regions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnContainer;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.utils.ListUtils;
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

	public static <R> @Nullable R traversePartial(MethodNode mth, IRegionPartialVisitor<R> visitor) {
		return traversePartialInternal(mth, visitor, mth.getRegion());
	}

	public static <R> @Nullable R traversePartial(MethodNode mth, IContainer container, IRegionPartialVisitor<R> visitor) {
		return traversePartialInternal(mth, visitor, container);
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

	private static final IContainer LEAVE_REGION_MARK = new InsnContainer(Collections.emptyList());

	private static void traverseInternal(MethodNode mth, IRegionVisitor visitor, IContainer startContainer) {
		List<IContainer> stack = new ArrayList<>();
		List<IRegion> regionLeaveStack = new ArrayList<>();
		stack.add(startContainer);
		while (true) {
			IContainer current = ListUtils.removeLast(stack);
			if (current == null) {
				return;
			}
			if (current == LEAVE_REGION_MARK) {
				IRegion region = ListUtils.removeLast(regionLeaveStack);
				visitor.leaveRegion(mth, Objects.requireNonNull(region));
			} else if (current instanceof IBlock) {
				visitor.processBlock(mth, (IBlock) current);
			} else if (current instanceof IRegion) {
				IRegion region = (IRegion) current;
				boolean visitRegion = visitor.enterRegion(mth, region);
				stack.add(LEAVE_REGION_MARK);
				regionLeaveStack.add(region);
				if (visitRegion) {
					addSubBlocksToStack(stack, region);
				}
			}
		}
	}

	private static <R> @Nullable R traversePartialInternal(MethodNode mth, IRegionPartialVisitor<R> visitor, IContainer startContainer) {
		List<IContainer> stack = new ArrayList<>();
		stack.add(startContainer);
		while (true) {
			IContainer current = ListUtils.removeLast(stack);
			if (current == null) {
				return null;
			}
			R result = visitor.visit(mth, current);
			if (result != null) {
				return result;
			}
			if (current instanceof IRegion) {
				addSubBlocksToStack(stack, (IRegion) current);
			}
		}
	}

	private static void addSubBlocksToStack(List<IContainer> stack, IRegion region) {
		List<IContainer> subBlocks = region.getSubBlocks();
		// add in reverse order to keep original order during visit
		for (int i = subBlocks.size() - 1; i >= 0; i--) {
			stack.add(subBlocks.get(i));
		}
	}

	private static boolean traverseIterativeStepInternal(MethodNode mth, IRegionIterativeVisitor visitor, IRegion startRegion) {
		List<IRegion> stack = new ArrayList<>();
		stack.add(startRegion);
		while (true) {
			IRegion region = ListUtils.removeLast(stack);
			if (region == null) {
				return false;
			}
			if (visitor.visitRegion(mth, region)) {
				return true;
			}
			List<IContainer> subBlocks = region.getSubBlocks();
			// add in reverse order to keep original order during visit
			for (int i = subBlocks.size() - 1; i >= 0; i--) {
				IContainer subBlock = subBlocks.get(i);
				if (subBlock instanceof IRegion) {
					stack.add((IRegion) subBlock);
				}
			}
		}
	}
}
