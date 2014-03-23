package jadx.core.dex.visitors.regions;

import jadx.core.dex.attributes.AttributeFlag;
import jadx.core.dex.attributes.AttributeType;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.LoopRegion;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.exceptions.JadxException;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckRegions extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(CheckRegions.class);

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode()
				|| mth.getBasicBlocks().isEmpty()
				|| mth.getAttributes().contains(AttributeType.JADX_ERROR)) {
			return;
		}

		// check if all blocks included in regions
		final Set<BlockNode> blocksInRegions = new HashSet<BlockNode>();
		IRegionVisitor collectBlocks = new AbstractRegionVisitor() {
			@Override
			public void processBlock(MethodNode mth, IBlock container) {
				if (container instanceof BlockNode) {
					blocksInRegions.add((BlockNode) container);
				}
			}
		};
		DepthRegionTraversal.traverseAll(mth, collectBlocks);

		if (mth.getBasicBlocks().size() != blocksInRegions.size()) {
			for (BlockNode block : mth.getBasicBlocks()) {
				if (!blocksInRegions.contains(block)
						&& !block.getInstructions().isEmpty()
						&& !block.getAttributes().contains(AttributeFlag.SKIP)) {
					mth.getAttributes().add(AttributeFlag.INCONSISTENT_CODE);
					LOG.debug(" Missing block: {} in {}", block, mth);
				}
			}
		}

		// check loop conditions
		IRegionVisitor checkLoops = new AbstractRegionVisitor() {
			@Override
			public void enterRegion(MethodNode mth, IRegion region) {
				if (region instanceof LoopRegion) {
					LoopRegion loop = (LoopRegion) region;
					BlockNode loopHeader = loop.getHeader();
					if (loopHeader != null && loopHeader.getInstructions().size() != 1) {
						ErrorsCounter.methodError(mth, "Incorrect condition in loop: " + loopHeader);
						mth.getAttributes().add(AttributeFlag.INCONSISTENT_CODE);
					}
				}
			}
		};
		DepthRegionTraversal.traverseAll(mth, checkLoops);
	}
}
