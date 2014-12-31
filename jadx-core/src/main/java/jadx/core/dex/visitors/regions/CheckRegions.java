package jadx.core.dex.visitors.regions;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.loops.LoopRegion;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.exceptions.JadxException;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckRegions extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(CheckRegions.class);

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode()
				|| mth.getBasicBlocks().isEmpty()
				|| mth.contains(AType.JADX_ERROR)) {
			return;
		}

		// printRegion(mth);

		// check if all blocks included in regions
		final Set<BlockNode> blocksInRegions = new HashSet<BlockNode>();
		DepthRegionTraversal.traverse(mth, new AbstractRegionVisitor() {
			@Override
			public void processBlock(MethodNode mth, IBlock container) {
				if (!(container instanceof BlockNode)) {
					return;
				}
				BlockNode block = (BlockNode) container;
				if (blocksInRegions.add(block)) {
					return;
				}
				if (!block.contains(AFlag.RETURN)
						&& !block.contains(AFlag.SKIP)
						&& !block.contains(AFlag.SYNTHETIC)
						&& !block.getInstructions().isEmpty()) {
					// TODO
					// mth.add(AFlag.INCONSISTENT_CODE);
					LOG.debug(" Duplicated block: {} in {}", block, mth);
					// printRegionsWithBlock(mth, block);
				}
			}
		});
		if (mth.getBasicBlocks().size() != blocksInRegions.size()) {
			for (BlockNode block : mth.getBasicBlocks()) {
				if (!blocksInRegions.contains(block)
						&& !block.getInstructions().isEmpty()
						&& !block.contains(AFlag.SKIP)) {
					mth.add(AFlag.INCONSISTENT_CODE);
					LOG.debug(" Missing block: {} in {}", block, mth);
				}
			}
		}

		// check loop conditions
		DepthRegionTraversal.traverse(mth, new AbstractRegionVisitor() {
			@Override
			public void enterRegion(MethodNode mth, IRegion region) {
				if (region instanceof LoopRegion) {
					BlockNode loopHeader = ((LoopRegion) region).getHeader();
					if (loopHeader != null && loopHeader.getInstructions().size() != 1) {
						ErrorsCounter.methodError(mth, "Incorrect condition in loop: " + loopHeader);
					}
				}
			}
		});
	}

	private static void printRegionsWithBlock(MethodNode mth, final BlockNode block) {
		final Set<IRegion> regions = new LinkedHashSet<IRegion>();
		DepthRegionTraversal.traverse(mth, new TracedRegionVisitor() {
			@Override
			public void processBlockTraced(MethodNode mth, IBlock container, IRegion currentRegion) {
				if (block.equals(container)) {
					regions.add(currentRegion);
				}
			}
		});
		LOG.debug(" Found block: {} in regions: {}", block, regions);
	}

	private void printRegion(MethodNode mth) {
		LOG.debug("|" + mth.toString());
		printRegion(mth, mth.getRegion(), "| ");
	}

	private void printRegion(MethodNode mth, IRegion region, String indent) {
		LOG.debug(indent + region);
		for (IContainer container : region.getSubBlocks()) {
			if (container instanceof IRegion) {
				printRegion(mth, (IRegion) container, indent + "  ");
			} else {
				LOG.debug(indent + "  " + container);
			}
		}
	}
}
