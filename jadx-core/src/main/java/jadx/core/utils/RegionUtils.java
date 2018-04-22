package jadx.core.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IBranchRegion;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.trycatch.CatchAttr;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.dex.trycatch.TryCatchBlock;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class RegionUtils {

	private RegionUtils() {
	}

	public static boolean hasExitEdge(IContainer container) {
		if (container instanceof IBlock) {
			InsnNode lastInsn = BlockUtils.getLastInsn((IBlock) container);
			if (lastInsn == null) {
				return false;
			}
			InsnType type = lastInsn.getType();
			return type == InsnType.RETURN
					|| type == InsnType.CONTINUE
					|| type == InsnType.BREAK
					|| type == InsnType.THROW;
		} else if (container instanceof IBranchRegion) {
			for (IContainer br : ((IBranchRegion) container).getBranches()) {
				if (br == null || !hasExitEdge(br)) {
					return false;
				}
			}
			return true;
		} else if (container instanceof IRegion) {
			IRegion region = (IRegion) container;
			List<IContainer> blocks = region.getSubBlocks();
			return !blocks.isEmpty() && hasExitEdge(blocks.get(blocks.size() - 1));
		} else {
			throw new JadxRuntimeException(unknownContainerType(container));
		}
	}

	public static InsnNode getLastInsn(IContainer container) {
		if (container instanceof IBlock) {
			IBlock block = (IBlock) container;
			List<InsnNode> insnList = block.getInstructions();
			if (insnList.isEmpty()) {
				return null;
			}
			return insnList.get(insnList.size() - 1);
		} else if (container instanceof IBranchRegion) {
			return null;
		} else if (container instanceof IRegion) {
			IRegion region = (IRegion) container;
			List<IContainer> blocks = region.getSubBlocks();
			if (blocks.isEmpty()) {
				return null;
			}
			return getLastInsn(blocks.get(blocks.size() - 1));
		} else {
			throw new JadxRuntimeException(unknownContainerType(container));
		}
	}

	public static IBlock getLastBlock(IContainer container) {
		if (container instanceof IBlock) {
			return (IBlock) container;
		} else if (container instanceof IBranchRegion) {
			return null;
		} else if (container instanceof IRegion) {
			List<IContainer> blocks = ((IRegion) container).getSubBlocks();
			if (blocks.isEmpty()) {
				return null;
			}
			return getLastBlock(blocks.get(blocks.size() - 1));
		} else {
			throw new JadxRuntimeException(unknownContainerType(container));
		}
	}

	/**
	 * Return true if last block in region has no successors
	 */
	public static boolean hasExitBlock(IContainer container) {
		if (container instanceof BlockNode) {
			return ((BlockNode) container).getSuccessors().isEmpty();
		} else if (container instanceof IBlock) {
			return true;
		} else if (container instanceof IRegion) {
			List<IContainer> blocks = ((IRegion) container).getSubBlocks();
			return !blocks.isEmpty()
					&& hasExitBlock(blocks.get(blocks.size() - 1));
		} else {
			throw new JadxRuntimeException(unknownContainerType(container));
		}
	}

	public static boolean hasBreakInsn(IContainer container) {
		if (container instanceof IBlock) {
			return BlockUtils.checkLastInsnType((IBlock) container, InsnType.BREAK);
		} else if (container instanceof IRegion) {
			List<IContainer> blocks = ((IRegion) container).getSubBlocks();
			return !blocks.isEmpty()
					&& hasBreakInsn(blocks.get(blocks.size() - 1));
		} else {
			throw new JadxRuntimeException("Unknown container type: " + container);
		}
	}

	public static int insnsCount(IContainer container) {
		if (container instanceof IBlock) {
			return ((IBlock) container).getInstructions().size();
		} else if (container instanceof IRegion) {
			IRegion region = (IRegion) container;
			int count = 0;
			for (IContainer block : region.getSubBlocks()) {
				count += insnsCount(block);
			}
			return count;
		} else {
			throw new JadxRuntimeException(unknownContainerType(container));
		}
	}

	public static boolean isEmpty(IContainer container) {
		return !notEmpty(container);
	}

	public static boolean notEmpty(IContainer container) {
		if (container instanceof IBlock) {
			return !((IBlock) container).getInstructions().isEmpty();
		} else if (container instanceof IRegion) {
			IRegion region = (IRegion) container;
			for (IContainer block : region.getSubBlocks()) {
				if (notEmpty(block)) {
					return true;
				}
			}
			return false;
		} else {
			throw new JadxRuntimeException(unknownContainerType(container));
		}
	}

	public static void getAllRegionBlocks(IContainer container, Set<IBlock> blocks) {
		if (container instanceof IBlock) {
			blocks.add((IBlock) container);
		} else if (container instanceof IRegion) {
			IRegion region = (IRegion) container;
			for (IContainer block : region.getSubBlocks()) {
				getAllRegionBlocks(block, blocks);
			}
		} else {
			throw new JadxRuntimeException(unknownContainerType(container));
		}
	}

	public static boolean isRegionContainsBlock(IContainer container, BlockNode block) {
		if (container instanceof IBlock) {
			return container == block;
		} else if (container instanceof IRegion) {
			IRegion region = (IRegion) container;
			for (IContainer b : region.getSubBlocks()) {
				if (isRegionContainsBlock(b, block)) {
					return true;
				}
			}
			return false;
		} else {
			throw new JadxRuntimeException(unknownContainerType(container));
		}
	}

	public static List<IContainer> getExcHandlersForRegion(IContainer region) {
		CatchAttr cb = region.get(AType.CATCH_BLOCK);
		if (cb != null) {
			TryCatchBlock tb = cb.getTryBlock();
			List<IContainer> list = new ArrayList<>(tb.getHandlersCount());
			for (ExceptionHandler eh : tb.getHandlers()) {
				list.add(eh.getHandlerRegion());
			}
			return list;
		}
		return Collections.emptyList();
	}

	private static boolean isRegionContainsExcHandlerRegion(IContainer container, IRegion region) {
		if (container == region) {
			return true;
		}
		if (container instanceof IRegion) {
			IRegion r = (IRegion) container;

			// process sub blocks
			for (IContainer b : r.getSubBlocks()) {
				// process try block
				CatchAttr cb = b.get(AType.CATCH_BLOCK);
				if (cb != null && b instanceof IRegion) {
					TryCatchBlock tb = cb.getTryBlock();
					for (ExceptionHandler eh : tb.getHandlers()) {
						if (isRegionContainsRegion(eh.getHandlerRegion(), region)) {
							return true;
						}
					}
				}
				if (isRegionContainsRegion(b, region)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Check if {@code region} contains in {@code container}.
	 * <br>
	 * For simple region (not from exception handlers) search in parents
	 * otherwise run recursive search because exception handlers can have several parents
	 */
	public static boolean isRegionContainsRegion(IContainer container, IRegion region) {
		if (container == region) {
			return true;
		}
		if (region == null) {
			return false;
		}
		IRegion parent = region.getParent();
		while (container != parent) {
			if (parent == null) {
				if (region.contains(AType.EXC_HANDLER)) {
					return isRegionContainsExcHandlerRegion(container, region);
				}
				return false;
			}
			region = parent;
			parent = region.getParent();
		}
		return true;
	}

	public static IContainer getBlockContainer(IContainer container, BlockNode block) {
		if (container instanceof IBlock) {
			return container == block ? container : null;
		} else if (container instanceof IRegion) {
			IRegion region = (IRegion) container;
			for (IContainer c : region.getSubBlocks()) {
				IContainer res = getBlockContainer(c, block);
				if (res != null) {
					return res instanceof IBlock ? region : res;
				}
			}
			return null;
		} else {
			throw new JadxRuntimeException(unknownContainerType(container));
		}
	}

	public static boolean isDominatedBy(BlockNode dom, IContainer cont) {
		if (dom == cont) {
			return true;
		}
		if (cont instanceof BlockNode) {
			BlockNode block = (BlockNode) cont;
			return block.isDominator(dom);
		} else if (cont instanceof IBlock) {
			return false;
		} else if (cont instanceof IRegion) {
			IRegion region = (IRegion) cont;
			for (IContainer c : region.getSubBlocks()) {
				if (!isDominatedBy(dom, c)) {
					return false;
				}
			}
			return true;
		} else {
			throw new JadxRuntimeException(unknownContainerType(cont));
		}
	}

	public static boolean hasPathThroughBlock(BlockNode block, IContainer cont) {
		if (block == cont) {
			return true;
		}
		if (cont instanceof BlockNode) {
			return BlockUtils.isPathExists(block, (BlockNode) cont);
		} else if (cont instanceof IBlock) {
			return false;
		} else if (cont instanceof IRegion) {
			IRegion region = (IRegion) cont;
			for (IContainer c : region.getSubBlocks()) {
				if (!hasPathThroughBlock(block, c)) {
					return false;
				}
			}
			return true;
		} else {
			throw new JadxRuntimeException(unknownContainerType(cont));
		}
	}

	protected static String unknownContainerType(IContainer container) {
		if (container == null) {
			return "Null container variable";
		}
		return "Unknown container type: " + container.getClass();
	}
}
