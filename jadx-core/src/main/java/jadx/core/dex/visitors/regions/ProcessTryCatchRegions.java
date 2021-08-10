package jadx.core.dex.visitors.regions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jadx.core.dex.attributes.AFlag;
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
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.dex.trycatch.TryCatchBlockAttr;
import jadx.core.utils.RegionUtils;

/**
 * Extract blocks to separate try/catch region
 */
public class ProcessTryCatchRegions extends AbstractRegionVisitor {

	public static void process(MethodNode mth) {
		if (mth.isNoCode() || mth.isNoExceptionHandlers()) {
			return;
		}
		List<TryCatchBlockAttr> tryBlocks = collectTryCatchBlocks(mth);
		if (tryBlocks.isEmpty()) {
			return;
		}
		DepthRegionTraversal.traverseIncludingExcHandlers(mth, (regionMth, region) -> {
			boolean changed = checkAndWrap(regionMth, tryBlocks, region);
			return changed && !tryBlocks.isEmpty();
		});
	}

	private static List<TryCatchBlockAttr> collectTryCatchBlocks(MethodNode mth) {
		List<TryCatchBlockAttr> list = mth.getAll(AType.TRY_BLOCKS_LIST);
		if (list.isEmpty()) {
			return Collections.emptyList();
		}
		List<TryCatchBlockAttr> tryBlocks = new ArrayList<>(list);
		tryBlocks.sort((a, b) -> a == b ? 0 : a.getOuterTryBlock() == b ? 1 : -1); // move parent try block to top
		return tryBlocks;
	}

	private static boolean checkAndWrap(MethodNode mth, List<TryCatchBlockAttr> tryBlocks, IRegion region) {
		// search top splitter block in this region (don't need to go deeper)
		for (TryCatchBlockAttr tb : tryBlocks) {
			BlockNode topSplitter = tb.getTopSplitter();
			if (region.getSubBlocks().contains(topSplitter)) {
				if (!wrapBlocks(region, tb, topSplitter)) {
					mth.addWarn("Can't wrap try/catch for region: " + region);
				}
				tryBlocks.remove(tb);
				return true;
			}
		}
		return false;
	}

	/**
	 * Extract all block dominated by 'dominator' to separate region and mark as try/catch block
	 */
	private static boolean wrapBlocks(IRegion replaceRegion, TryCatchBlockAttr tb, BlockNode dominator) {
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
		tryCatchRegion.setTryCatchBlock(tb);

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

	private static boolean isHandlerPath(TryCatchBlockAttr tb, IContainer cont) {
		for (ExceptionHandler h : tb.getHandlers()) {
			BlockNode handlerBlock = h.getHandlerBlock();
			if (handlerBlock != null
					&& !handlerBlock.contains(AFlag.REMOVE)
					&& RegionUtils.hasPathThroughBlock(handlerBlock, cont)) {
				return true;
			}
		}
		return false;
	}
}
