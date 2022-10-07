package jadx.core.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.AttrList;
import jadx.core.dex.attributes.nodes.LoopInfo;
import jadx.core.dex.attributes.nodes.LoopLabelAttr;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IBranchRegion;
import jadx.core.dex.nodes.IConditionRegion;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.Region;
import jadx.core.dex.regions.loops.LoopRegion;
import jadx.core.dex.trycatch.CatchAttr;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.dex.trycatch.TryCatchBlockAttr;
import jadx.core.dex.visitors.regions.AbstractRegionVisitor;
import jadx.core.dex.visitors.regions.DepthRegionTraversal;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class RegionUtils {

	private RegionUtils() {
	}

	public static boolean hasExitEdge(IContainer container) {
		if (container instanceof IBlock) {
			return BlockUtils.containsExitInsn((IBlock) container);
		}
		if (container instanceof IBranchRegion) {
			// all branches must have exit edge
			for (IContainer br : ((IBranchRegion) container).getBranches()) {
				if (br == null || !hasExitEdge(br)) {
					return false;
				}
			}
			return true;
		}
		if (container instanceof IRegion) {
			IRegion region = (IRegion) container;
			List<IContainer> blocks = region.getSubBlocks();
			return !blocks.isEmpty() && hasExitEdge(blocks.get(blocks.size() - 1));
		}
		throw new JadxRuntimeException(unknownContainerType(container));
	}

	@Nullable
	public static InsnNode getFirstInsn(IContainer container) {
		if (container instanceof IBlock) {
			IBlock block = (IBlock) container;
			List<InsnNode> insnList = block.getInstructions();
			if (insnList.isEmpty()) {
				return null;
			}
			return insnList.get(0);
		} else if (container instanceof IBranchRegion) {
			return null;
		} else if (container instanceof IRegion) {
			IRegion region = (IRegion) container;
			List<IContainer> blocks = region.getSubBlocks();
			if (blocks.isEmpty()) {
				return null;
			}
			return getFirstInsn(blocks.get(0));
		} else {
			throw new JadxRuntimeException(unknownContainerType(container));
		}
	}

	public static int getFirstSourceLine(IContainer container) {
		if (container instanceof IBlock) {
			return BlockUtils.getFirstSourceLine((IBlock) container);
		}
		if (container instanceof IConditionRegion) {
			return ((IConditionRegion) container).getConditionSourceLine();
		}
		if (container instanceof IBranchRegion) {
			IBranchRegion branchRegion = (IBranchRegion) container;
			return getFirstSourceLine(branchRegion.getBranches());
		}
		if (container instanceof IRegion) {
			IRegion region = (IRegion) container;
			return getFirstSourceLine(region.getSubBlocks());
		}
		return 0;
	}

	private static int getFirstSourceLine(List<IContainer> containers) {
		if (containers.isEmpty()) {
			return 0;
		}
		for (IContainer container : containers) {
			int line = getFirstSourceLine(container);
			if (line != 0) {
				return line;
			}
		}
		return 0;
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

	public static boolean isExitBlock(MethodNode mth, IContainer container) {
		if (container instanceof BlockNode) {
			return BlockUtils.isExitBlock(mth, (BlockNode) container);
		}
		return false;
	}

	/**
	 * Return true if last block in region has no successors or jump out insn (return or break)
	 */
	public static boolean hasExitBlock(IContainer container) {
		if (container == null) {
			return false;
		}
		return hasExitBlock(container, container);
	}

	private static boolean hasExitBlock(IContainer rootContainer, IContainer container) {
		if (container instanceof BlockNode) {
			BlockNode blockNode = (BlockNode) container;
			if (BlockUtils.isExitBlock(blockNode)) {
				return true;
			}
			return isInsnExitContainer(rootContainer, (IBlock) container);
		}
		if (container instanceof IBranchRegion) {
			IBranchRegion branchRegion = (IBranchRegion) container;
			return ListUtils.allMatch(branchRegion.getBranches(), RegionUtils::hasExitBlock);
		}
		if (container instanceof IBlock) {
			return isInsnExitContainer(rootContainer, (IBlock) container);
		}
		if (container instanceof IRegion) {
			List<IContainer> blocks = ((IRegion) container).getSubBlocks();
			return !blocks.isEmpty()
					&& hasExitBlock(rootContainer, blocks.get(blocks.size() - 1));
		}
		throw new JadxRuntimeException(unknownContainerType(container));
	}

	private static boolean isInsnExitContainer(IContainer rootContainer, IBlock block) {
		InsnNode lastInsn = BlockUtils.getLastInsn(block);
		if (lastInsn == null) {
			return false;
		}
		InsnType insnType = lastInsn.getType();
		if (insnType == InsnType.RETURN) {
			return true;
		}
		if (insnType == InsnType.THROW) {
			// check if after throw execution can continue in current container
			CatchAttr catchAttr = lastInsn.get(AType.EXC_CATCH);
			if (catchAttr != null) {
				for (ExceptionHandler handler : catchAttr.getHandlers()) {
					if (RegionUtils.isRegionContainsBlock(rootContainer, handler.getHandlerBlock())) {
						return false;
					}
				}
			}
			return true;
		}
		if (insnType == InsnType.BREAK) {
			AttrList<LoopInfo> loopInfoAttrList = lastInsn.get(AType.LOOP);
			if (loopInfoAttrList != null) {
				for (LoopInfo loopInfo : loopInfoAttrList.getList()) {
					if (!RegionUtils.isRegionContainsBlock(rootContainer, loopInfo.getStart())) {
						return true;
					}
				}
			}
			LoopLabelAttr loopLabelAttr = lastInsn.get(AType.LOOP_LABEL);
			if (loopLabelAttr != null
					&& !RegionUtils.isRegionContainsBlock(rootContainer, loopLabelAttr.getLoop().getStart())) {
				return true;
			}
		}
		return false;
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
			List<InsnNode> insnList = ((IBlock) container).getInstructions();
			int count = 0;
			for (InsnNode insn : insnList) {
				if (insn.contains(AFlag.DONT_GENERATE)) {
					continue;
				}
				count++;
			}
			return count;
		}
		if (container instanceof IRegion) {
			IRegion region = (IRegion) container;
			int count = 0;
			for (IContainer block : region.getSubBlocks()) {
				count += insnsCount(block);
			}
			return count;
		}
		throw new JadxRuntimeException(unknownContainerType(container));
	}

	public static boolean isEmpty(IContainer container) {
		return !notEmpty(container);
	}

	public static boolean notEmpty(@Nullable IContainer container) {
		if (container == null) {
			return false;
		}
		if (container instanceof IBlock) {
			List<InsnNode> insnList = ((IBlock) container).getInstructions();
			for (InsnNode insnNode : insnList) {
				if (!insnNode.contains(AFlag.DONT_GENERATE)) {
					return true;
				}
			}
			return false;
		}
		if (container instanceof LoopRegion) {
			return true;
		}
		if (container instanceof IRegion) {
			IRegion region = (IRegion) container;
			for (IContainer block : region.getSubBlocks()) {
				if (notEmpty(block)) {
					return true;
				}
			}
			return false;
		}
		throw new JadxRuntimeException(unknownContainerType(container));
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
		TryCatchBlockAttr tb = region.get(AType.TRY_BLOCK);
		if (tb != null) {
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
				TryCatchBlockAttr tb = b.get(AType.TRY_BLOCK);
				if (tb != null && b instanceof IRegion) {
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

	/**
	 * Check if two blocks in same region on same level
	 * TODO: Add 'region' annotation to all blocks to speed up checks
	 */
	public static boolean isBlocksInSameRegion(MethodNode mth, BlockNode firstBlock, BlockNode secondBlock) {
		Region region = mth.getRegion();
		if (region == null) {
			return false;
		}
		IContainer firstContainer = getBlockContainer(region, firstBlock);
		if (firstContainer instanceof IRegion) {
			if (firstContainer instanceof IBranchRegion) {
				return false;
			}
			List<IContainer> subBlocks = ((IRegion) firstContainer).getSubBlocks();
			return subBlocks.contains(secondBlock);
		}
		return false;
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
		}
		if (cont instanceof IBlock) {
			return false;
		}
		if (cont instanceof IRegion) {
			IRegion region = (IRegion) cont;
			for (IContainer c : region.getSubBlocks()) {
				if (hasPathThroughBlock(block, c)) {
					return true;
				}
			}
			return false;
		}
		throw new JadxRuntimeException(unknownContainerType(cont));
	}

	protected static String unknownContainerType(IContainer container) {
		if (container == null) {
			return "Null container variable";
		}
		return "Unknown container type: " + container.getClass();
	}

	public static void visitBlocks(MethodNode mth, IContainer container, Consumer<IBlock> visitor) {
		DepthRegionTraversal.traverse(mth, container, new AbstractRegionVisitor() {
			@Override
			public void processBlock(MethodNode mth, IBlock block) {
				visitor.accept(block);
			}
		});
	}
}
