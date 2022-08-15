package jadx.core.dex.visitors.blocks;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.EmptyBitSet;
import jadx.core.utils.exceptions.JadxRuntimeException;

/**
 * Build dominator tree based on the algorithm described in paper:
 * Cooper, Keith D.; Harvey, Timothy J; Kennedy, Ken (2001).
 * "A Simple, Fast Dominance Algorithm"
 * http://www.hipersoft.rice.edu/grads/publications/dom14.pdf
 */
@SuppressWarnings("JavadocLinkAsPlainText")
public class DominatorTree {

	public static void compute(MethodNode mth) {
		List<BlockNode> sorted = sortBlocks(mth);
		BlockNode[] doms = build(sorted);
		apply(sorted, doms);
	}

	private static List<BlockNode> sortBlocks(MethodNode mth) {
		int blocksCount = mth.getBasicBlocks().size();
		List<BlockNode> sorted = new ArrayList<>(blocksCount);
		BlockUtils.dfsVisit(mth, sorted::add);
		if (sorted.size() != blocksCount) {
			throw new JadxRuntimeException("Found unreachable blocks");
		}
		mth.setBasicBlocks(sorted);
		return sorted;
	}

	@NotNull
	private static BlockNode[] build(List<BlockNode> sorted) {
		int blocksCount = sorted.size();
		BlockNode[] doms = new BlockNode[blocksCount];
		doms[0] = sorted.get(0);
		boolean changed = true;
		while (changed) {
			changed = false;
			for (int blockId = 1; blockId < blocksCount; blockId++) {
				BlockNode b = sorted.get(blockId);
				List<BlockNode> preds = b.getPredecessors();
				int pickedPred = -1;
				BlockNode newIDom = null;
				for (BlockNode pred : preds) {
					int id = pred.getId();
					if (doms[id] != null) {
						newIDom = pred;
						pickedPred = id;
						break;
					}
				}
				if (newIDom == null) {
					throw new JadxRuntimeException("No predecessors for block: " + b);
				}
				for (BlockNode predBlock : preds) {
					int predId = predBlock.getId();
					if (predId == pickedPred) {
						continue;
					}
					if (doms[predId] != null) {
						newIDom = intersect(sorted, doms, predBlock, newIDom);
					}
				}
				if (doms[blockId] != newIDom) {
					doms[blockId] = newIDom;
					changed = true;
				}
			}
		}
		return doms;
	}

	private static BlockNode intersect(List<BlockNode> sorted, BlockNode[] doms, BlockNode b1, BlockNode b2) {
		int f1 = b1.getId();
		int f2 = b2.getId();
		while (f1 != f2) {
			while (f1 > f2) {
				f1 = doms[f1].getId();
			}
			while (f2 > f1) {
				f2 = doms[f2].getId();
			}
		}
		return sorted.get(f1);
	}

	private static void apply(List<BlockNode> sorted, BlockNode[] doms) {
		BlockNode enterBlock = sorted.get(0);
		enterBlock.setDoms(EmptyBitSet.EMPTY);
		enterBlock.setIDom(null);
		int blocksCount = sorted.size();
		for (int i = 1; i < blocksCount; i++) {
			BlockNode block = sorted.get(i);
			BlockNode idom = doms[i];
			block.setIDom(idom);
			idom.addDominatesOn(block);
			BitSet domBS = collectDoms(doms, idom);
			domBS.clear(i);
			block.setDoms(domBS);
		}
	}

	private static BitSet collectDoms(BlockNode[] doms, BlockNode idom) {
		BitSet domBS = new BitSet(doms.length);
		BlockNode nextIDom = idom;
		while (true) {
			int id = nextIDom.getId();
			if (domBS.get(id)) {
				break;
			}
			domBS.set(id);
			BitSet curDoms = nextIDom.getDoms();
			if (curDoms != null) {
				// use already collected set
				domBS.or(curDoms);
				break;
			}
			nextIDom = doms[id];
		}
		return domBS;
	}

	public static void computeDominanceFrontier(MethodNode mth) {
		List<BlockNode> blocks = mth.getBasicBlocks();
		for (BlockNode block : blocks) {
			block.setDomFrontier(null);
		}
		int blocksCount = blocks.size();
		for (BlockNode block : blocks) {
			List<BlockNode> preds = block.getPredecessors();
			if (preds.size() >= 2) {
				BlockNode idom = block.getIDom();
				for (BlockNode pred : preds) {
					BlockNode runner = pred;
					while (runner != idom) {
						addToDF(runner, block, blocksCount);
						runner = runner.getIDom();
					}
				}
			}
		}
		for (BlockNode block : blocks) {
			BitSet df = block.getDomFrontier();
			if (df == null || df.isEmpty()) {
				block.setDomFrontier(EmptyBitSet.EMPTY);
			}
		}
	}

	private static void addToDF(BlockNode block, BlockNode dfBlock, int blocksCount) {
		BitSet df = block.getDomFrontier();
		if (df == null) {
			df = new BitSet(blocksCount);
			block.setDomFrontier(df);
		}
		df.set(dfBlock.getId());
	}
}
