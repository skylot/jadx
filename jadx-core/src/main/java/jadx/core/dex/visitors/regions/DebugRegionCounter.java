package jadx.core.dex.visitors.regions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.AbstractVisitor;

public class DebugRegionCounter extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) {
		RegionCounterVisitor visitor = new RegionCounterVisitor();
		DepthRegionTraversal.traverse(mth, visitor);
		List<BlockDepthEntry> sortedBlocks = visitor.getSortedEntries();
		for (BlockDepthEntry x : sortedBlocks) {
			System.out.println(x.depth + " : " + x.block.toString() + " // " + x.block.getInstructions().toString());
		}

		System.out.println("nregions :: " + visitor.getNRegions());
	}

	private static class RegionCounterVisitor extends AbstractRegionVisitor {
		private int depth = 0;
		private int nregions = 0;
		private List<BlockDepthEntry> blockDepths = new ArrayList<>();

		@Override
		public boolean enterRegion(MethodNode mth, IRegion region) {
			depth += 1;
			nregions += 1;
			return true;
		}

		@Override
		public void processBlock(MethodNode mth, IBlock container) {
			if (container instanceof BlockNode) {
				BlockNode b = (BlockNode) container;
				blockDepths.add(new BlockDepthEntry(depth, b));
			}
		}

		@Override
		public void leaveRegion(MethodNode mth, IRegion region) {
			depth -= 1;
		}

		public List<BlockDepthEntry> getSortedEntries() {
			blockDepths.sort(Comparator.comparingInt(x -> x.depth));
			return blockDepths;
		}

		public int getNRegions() {
			return nregions;
		}

	}

	private static class BlockDepthEntry {
		public int depth;
		public BlockNode block;

		public BlockDepthEntry(int depth, BlockNode block) {
			this.depth = depth;
			this.block = block;
		}
	}

}
