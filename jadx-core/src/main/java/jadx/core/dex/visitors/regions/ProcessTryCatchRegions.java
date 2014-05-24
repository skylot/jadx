package jadx.core.dex.visitors.regions;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.AbstractRegion;
import jadx.core.dex.regions.Region;
import jadx.core.dex.trycatch.CatchAttr;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.dex.trycatch.TryCatchBlock;
import jadx.core.utils.BlockUtils;
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
	private static final boolean DEBUG = false;

	static {
		if (DEBUG) {
			LOG.debug("Debug enabled for " + ProcessTryCatchRegions.class);
		}
	}

	private final Map<BlockNode, TryCatchBlock> tryBlocksMap = new HashMap<BlockNode, TryCatchBlock>(2);

	public ProcessTryCatchRegions(MethodNode mth) {
		if (mth.isNoCode() || mth.isNoExceptionHandlers()) {
			return;
		}

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
			BitSet bs = null;
			// build bitset with dominators of blocks covered with this try/catch block
			for (BlockNode block : mth.getBasicBlocks()) {
				CatchAttr c = block.get(AType.CATCH_BLOCK);
				if (c != null && c.getTryBlock() == tb) {
					if (bs == null) {
						bs = (BitSet) block.getDoms().clone();
					} else {
						bs.and(block.getDoms());
					}
				}
			}
			assert bs != null;

			// intersect to get dominator of dominators
			List<BlockNode> domBlocks = BlockUtils.bitSetToBlocks(mth, bs);
			for (BlockNode block : domBlocks) {
				bs.andNot(block.getDoms());
			}
			domBlocks = BlockUtils.bitSetToBlocks(mth, bs);
			if (domBlocks.size() != 1) {
				throw new JadxRuntimeException(
						"Exception block dominator not found, method:" + mth + ". bs: " + bs);
			}

			BlockNode domBlock = domBlocks.get(0);

			TryCatchBlock prevTB = tryBlocksMap.put(domBlock, tb);
			if (prevTB != null) {
				LOG.info("!!! TODO: merge try blocks in " + mth);
			}
		}

		if (DEBUG && !tryBlocksMap.isEmpty()) {
			LOG.debug("ProcessTryCatchRegions: \n {} \n {}", mth, tryBlocksMap);
		}
	}

	@Override
	public void leaveRegion(MethodNode mth, IRegion region) {
		if (tryBlocksMap.isEmpty() || !(region instanceof Region)) {
			return;
		}
		// search dominator blocks in this region (don't need to go deeper)
		for (BlockNode dominator : tryBlocksMap.keySet()) {
			if (region.getSubBlocks().contains(dominator)) {
				wrapBlocks(region, dominator);
				tryBlocksMap.remove(dominator);
				// if region is modified rerun this method
				leaveRegion(mth, region);
				return;
			}
		}
	}

	/**
	 * Extract all block dominated by 'dominator' to separate region and mark as try/catch block
	 */
	private void wrapBlocks(IRegion region, BlockNode dominator) {
		Region newRegion = new Region(region);
		TryCatchBlock tb = tryBlocksMap.get(dominator);
		assert tb != null;

		for (IContainer cont : region.getSubBlocks()) {
			if (RegionUtils.isDominaterBy(dominator, cont)) {
				boolean pathFromExcHandler = false;
				for (ExceptionHandler h : tb.getHandlers()) {
					if (RegionUtils.hasPathThruBlock(h.getHandleBlock(), cont)) {
						pathFromExcHandler = true;
						break;
					}
				}
				if (!pathFromExcHandler) {
					newRegion.getSubBlocks().add(cont);
				} else {
					break;
				}
			}
		}
		if (!newRegion.getSubBlocks().isEmpty()) {
			if (DEBUG) {
				LOG.debug("ProcessTryCatchRegions mark: {}", newRegion);
			}
			// replace first node by region
			IContainer firstNode = newRegion.getSubBlocks().get(0);
			int i = region.getSubBlocks().indexOf(firstNode);
			region.getSubBlocks().set(i, newRegion);
			region.getSubBlocks().removeAll(newRegion.getSubBlocks());

			newRegion.addAttr(tb.getCatchAttr());

			// fix parents
			for (IContainer cont : newRegion.getSubBlocks()) {
				if (cont instanceof AbstractRegion) {
					AbstractRegion aReg = (AbstractRegion) cont;
					aReg.setParent(newRegion);
				}
			}
		}
	}

}
