package jadx.core.dex.visitors.regions;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IBranchRegion;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.AbstractRegion;
import jadx.core.dex.regions.Region;
import jadx.core.dex.regions.TryCatchRegion;
import jadx.core.dex.regions.loops.LoopRegion;
import jadx.core.dex.trycatch.CatchAttr;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.dex.trycatch.SplitterBlockAttr;
import jadx.core.dex.trycatch.TryCatchBlock;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.RegionUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extract blocks to separate try/catch region
 */
public class ProcessTryCatchRegions extends AbstractRegionVisitor {

	private static final Logger LOG = LoggerFactory.getLogger(ProcessTryCatchRegions.class);

	public static void process(MethodNode mth) {
		if (mth.isNoCode() || mth.isNoExceptionHandlers()) {
			return;
		}

		final Map<BlockNode, TryCatchBlock> tryBlocksMap = new HashMap<BlockNode, TryCatchBlock>(2);
		searchTryCatchDominators(mth, tryBlocksMap);

		IRegionIterativeVisitor visitor = new IRegionIterativeVisitor() {
			@Override
			public boolean visitRegion(MethodNode mth, IRegion region) {
				boolean changed = checkAndWrap(mth, tryBlocksMap, region);
				return changed && !tryBlocksMap.isEmpty();
			}
		};
		DepthRegionTraversal.traverseIncludingExcHandlers(mth, visitor);
	}

	private static void searchTryCatchDominators(MethodNode mth, Map<BlockNode, TryCatchBlock> tryBlocksMap) {
		Set<TryCatchBlock> tryBlocks = new HashSet<TryCatchBlock>();
		// collect all try/catch blocks
		for (BlockNode block : mth.getBasicBlocks()) {
			CatchAttr c = block.get(AType.CATCH_BLOCK);
			if (c != null) {
				tryBlocks.add(c.getTryBlock());
			}
		}

		// for each try block search nearest dominator block
		for (TryCatchBlock tb : tryBlocks) {
			if (tb.getHandlersCount() == 0) {
				LOG.warn("No exception handlers in catch block, method: {}", mth);
				continue;
			}
			BitSet bs = new BitSet(mth.getBasicBlocks().size());
			for (ExceptionHandler excHandler : tb.getHandlers()) {
				SplitterBlockAttr splitter = excHandler.getHandlerBlock().get(AType.SPLITTER_BLOCK);
				if (splitter != null) {
					BlockNode block = splitter.getBlock();
					bs.set(block.getId());
				}
			}
			List<BlockNode> domBlocks = BlockUtils.bitSetToBlocks(mth, bs);
			BlockNode domBlock;
			if (domBlocks.size() != 1) {
				domBlock = BlockUtils.getTopBlock(domBlocks);
				if (domBlock == null) {
					throw new JadxRuntimeException(
							"Exception block dominator not found, method:" + mth + ". bs: " + domBlocks);
				}
			} else {
				domBlock = domBlocks.get(0);
			}
			TryCatchBlock prevTB = tryBlocksMap.put(domBlock, tb);
			if (prevTB != null) {
				ErrorsCounter.methodError(mth, "Failed to process nested try/catch");
			}
		}
	}

	private static boolean checkAndWrap(MethodNode mth, Map<BlockNode, TryCatchBlock> tryBlocksMap, IRegion region) {
		// search dominator blocks in this region (don't need to go deeper)
		for (Map.Entry<BlockNode, TryCatchBlock> entry : tryBlocksMap.entrySet()) {
			BlockNode dominator = entry.getKey();
			if (region.getSubBlocks().contains(dominator)) {
				TryCatchBlock tb = tryBlocksMap.get(dominator);
				if (!wrapBlocks(region, tb, dominator)) {
					ErrorsCounter.methodError(mth, "Can't wrap try/catch for " + region);
				}
				tryBlocksMap.remove(dominator);
				return true;
			}
		}
		return false;
	}

	/**
	 * Extract all block dominated by 'dominator' to separate region and mark as try/catch block
	 */
	private static boolean wrapBlocks(IRegion replaceRegion, TryCatchBlock tb, BlockNode dominator) {
		if (replaceRegion == null) {
			return false;
		}
		if (replaceRegion instanceof LoopRegion) {
			LoopRegion loop = (LoopRegion) replaceRegion;
			return wrapBlocks(loop.getBody(), tb, dominator);
		}
		if (replaceRegion instanceof IBranchRegion) {
			return wrapBlocks(replaceRegion.getParent(), tb, dominator);
		}

		Region tryRegion = new Region(replaceRegion);
		List<IContainer> subBlocks = replaceRegion.getSubBlocks();
		for (IContainer cont : subBlocks) {
			if (RegionUtils.hasPathThroughBlock(dominator, cont)) {
				if (isHandlerPath(tb, cont)) {
					break;
				}
				tryRegion.getSubBlocks().add(cont);
			}
		}
		if (tryRegion.getSubBlocks().isEmpty()) {
			return false;
		}

		TryCatchRegion tryCatchRegion = new TryCatchRegion(replaceRegion, tryRegion);
		tryRegion.setParent(tryCatchRegion);
		tryCatchRegion.setTryCatchBlock(tb.getCatchAttr().getTryBlock());

		// replace first node by region
		IContainer firstNode = tryRegion.getSubBlocks().get(0);
		if (!replaceRegion.replaceSubBlock(firstNode, tryCatchRegion)) {
			return false;
		}
		subBlocks.removeAll(tryRegion.getSubBlocks());

		// fix parents for tryRegion sub blocks
		for (IContainer cont : tryRegion.getSubBlocks()) {
			if (cont instanceof AbstractRegion) {
				AbstractRegion aReg = (AbstractRegion) cont;
				aReg.setParent(tryRegion);
			}
		}
		return true;
	}

	private static boolean isHandlerPath(TryCatchBlock tb, IContainer cont) {
		for (ExceptionHandler h : tb.getHandlers()) {
			if (RegionUtils.hasPathThroughBlock(h.getHandlerBlock(), cont)) {
				return true;
			}
		}
		return false;
	}
}
