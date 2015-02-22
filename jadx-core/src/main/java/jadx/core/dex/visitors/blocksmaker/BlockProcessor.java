package jadx.core.dex.visitors.blocksmaker;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.LoopInfo;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.Edge;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.trycatch.CatchAttr;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static jadx.core.dex.visitors.blocksmaker.BlockSplitter.connect;
import static jadx.core.dex.visitors.blocksmaker.BlockSplitter.removeConnection;
import static jadx.core.utils.EmptyBitSet.EMPTY;

public class BlockProcessor extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(BlockProcessor.class);

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}
		processBlocksTree(mth);
	}

	public static void rerun(MethodNode mth) {
		removeBlocks(mth);
		clearBlocksState(mth);
		processBlocksTree(mth);
	}

	private static void processBlocksTree(MethodNode mth) {
		computeDominators(mth);
		markReturnBlocks(mth);

		int i = 0;
		while (modifyBlocksTree(mth)) {
			// revert calculations
			clearBlocksState(mth);
			// recalculate dominators tree
			computeDominators(mth);
			markReturnBlocks(mth);

			if (i++ > 100) {
				throw new AssertionError("Can't fix method cfg: " + mth);
			}
		}
		computeDominanceFrontier(mth);
		registerLoops(mth);
		processNestedLoops(mth);
	}

	private static void computeDominators(MethodNode mth) {
		List<BlockNode> basicBlocks = mth.getBasicBlocks();
		int nBlocks = basicBlocks.size();
		for (int i = 0; i < nBlocks; i++) {
			BlockNode block = basicBlocks.get(i);
			block.setId(i);
			block.setDoms(new BitSet(nBlocks));
			block.getDoms().set(0, nBlocks);
		}

		BlockNode entryBlock = mth.getEnterBlock();
		entryBlock.getDoms().clear();
		entryBlock.getDoms().set(entryBlock.getId());

		BitSet dset = new BitSet(nBlocks);
		boolean changed;
		do {
			changed = false;
			for (BlockNode block : basicBlocks) {
				if (block == entryBlock) {
					continue;
				}
				BitSet d = block.getDoms();
				if (!changed) {
					dset.clear();
					dset.or(d);
				}
				for (BlockNode pred : block.getPredecessors()) {
					d.and(pred.getDoms());
				}
				d.set(block.getId());
				if (!changed && !d.equals(dset)) {
					changed = true;
				}
			}
		} while (changed);

		markLoops(mth);

		// clear self dominance
		for (BlockNode block : basicBlocks) {
			block.getDoms().clear(block.getId());
		}

		// calculate immediate dominators
		for (BlockNode block : basicBlocks) {
			if (block == entryBlock) {
				continue;
			}
			BlockNode idom;
			List<BlockNode> preds = block.getPredecessors();
			if (preds.size() == 1) {
				idom = preds.get(0);
			} else {
				BitSet bs = new BitSet(block.getDoms().length());
				bs.or(block.getDoms());
				for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
					BlockNode dom = basicBlocks.get(i);
					bs.andNot(dom.getDoms());
				}
				if (bs.cardinality() != 1) {
					throw new JadxRuntimeException("Can't find immediate dominator for block " + block
							+ " in " + bs + " preds:" + preds);
				}
				idom = basicBlocks.get(bs.nextSetBit(0));
			}
			block.setIDom(idom);
			idom.addDominatesOn(block);
		}
	}

	private static void computeDominanceFrontier(MethodNode mth) {
		for (BlockNode exit : mth.getExitBlocks()) {
			exit.setDomFrontier(EMPTY);
		}
		for (BlockNode block : mth.getBasicBlocks()) {
			computeBlockDF(mth, block);
		}
	}

	private static void computeBlockDF(MethodNode mth, BlockNode block) {
		if (block.getDomFrontier() != null) {
			return;
		}
		for (BlockNode c : block.getDominatesOn()) {
			computeBlockDF(mth, c);
		}
		List<BlockNode> blocks = mth.getBasicBlocks();
		BitSet domFrontier = null;
		for (BlockNode s : block.getSuccessors()) {
			if (s.getIDom() != block) {
				if (domFrontier == null) {
					domFrontier = new BitSet(blocks.size());
				}
				domFrontier.set(s.getId());
			}
		}
		for (BlockNode c : block.getDominatesOn()) {
			BitSet frontier = c.getDomFrontier();
			for (int p = frontier.nextSetBit(0); p >= 0; p = frontier.nextSetBit(p + 1)) {
				if (blocks.get(p).getIDom() != block) {
					if (domFrontier == null) {
						domFrontier = new BitSet(blocks.size());
					}
					domFrontier.set(p);
				}
			}
		}
		if (domFrontier == null || domFrontier.cardinality() == 0) {
			domFrontier = EMPTY;
		}
		block.setDomFrontier(domFrontier);
	}

	private static void markReturnBlocks(MethodNode mth) {
		mth.getExitBlocks().clear();
		for (BlockNode block : mth.getBasicBlocks()) {
			if (BlockUtils.checkLastInsnType(block, InsnType.RETURN)) {
				block.add(AFlag.RETURN);
				mth.getExitBlocks().add(block);
			}
		}
	}

	private static void markLoops(MethodNode mth) {
		for (BlockNode block : mth.getBasicBlocks()) {
			for (BlockNode succ : block.getSuccessors()) {
				// Every successor that dominates its predecessor is a header of a loop,
				// block -> succ is a back edge.
				if (block.getDoms().get(succ.getId())) {
					succ.add(AFlag.LOOP_START);
					block.add(AFlag.LOOP_END);

					LoopInfo loop = new LoopInfo(succ, block);
					succ.addAttr(AType.LOOP, loop);
					block.addAttr(AType.LOOP, loop);
				}
			}
		}
	}

	private static void registerLoops(MethodNode mth) {
		for (BlockNode block : mth.getBasicBlocks()) {
			if (block.contains(AFlag.LOOP_START)) {
				for (LoopInfo loop : block.getAll(AType.LOOP)) {
					mth.registerLoop(loop);
				}
			}
		}
	}

	private static void processNestedLoops(MethodNode mth) {
		if (mth.getLoopsCount() == 0) {
			return;
		}
		for (LoopInfo outLoop : mth.getLoops()) {
			for (LoopInfo innerLoop : mth.getLoops()) {
				if (outLoop == innerLoop) {
					continue;
				}
				if (outLoop.getLoopBlocks().containsAll(innerLoop.getLoopBlocks())) {
					LoopInfo parentLoop = innerLoop.getParentLoop();
					if (parentLoop != null) {
						if (parentLoop.getLoopBlocks().containsAll(outLoop.getLoopBlocks())) {
							outLoop.setParentLoop(parentLoop);
							innerLoop.setParentLoop(outLoop);
						} else {
							parentLoop.setParentLoop(outLoop);
						}
					} else {
						innerLoop.setParentLoop(outLoop);
					}
				}
			}
		}
	}

	private static boolean modifyBlocksTree(MethodNode mth) {
		for (BlockNode block : mth.getBasicBlocks()) {
			if (block.getPredecessors().isEmpty() && block != mth.getEnterBlock()) {
				throw new JadxRuntimeException("Unreachable block: " + block);
			}

			// check loops
			List<LoopInfo> loops = block.getAll(AType.LOOP);
			if (loops.size() > 1) {
				boolean oneHeader = true;
				for (LoopInfo loop : loops) {
					if (loop.getStart() != block) {
						oneHeader = false;
						break;
					}
				}
				if (oneHeader) {
					// several back edges connected to one loop header => make additional block
					BlockNode newLoopHeader = BlockSplitter.startNewBlock(mth, block.getStartOffset());
					newLoopHeader.add(AFlag.SYNTHETIC);
					connect(newLoopHeader, block);
					for (LoopInfo la : loops) {
						BlockNode node = la.getEnd();
						removeConnection(node, block);
						connect(node, newLoopHeader);
					}
					return true;
				}
			}
			if (loops.size() == 1) {
				LoopInfo loop = loops.get(0);
				// insert additional blocks for possible 'break' insertion
				List<Edge> edges = loop.getExitEdges();
				if (!edges.isEmpty()) {
					boolean change = false;
					for (Edge edge : edges) {
						BlockNode target = edge.getTarget();
						if (!target.contains(AFlag.SYNTHETIC)) {
							BlockSplitter.insertBlockBetween(mth, edge.getSource(), target);
							change = true;
						}
					}
					if (change) {
						return true;
					}
				}
				// insert additional blocks for possible 'continue' insertion
				BlockNode loopEnd = loop.getEnd();
				if (loopEnd.getPredecessors().size() > 1) {
					boolean change = false;
					List<BlockNode> nodes = new ArrayList<BlockNode>(loopEnd.getPredecessors());
					for (BlockNode pred : nodes) {
						if (!pred.contains(AFlag.SYNTHETIC)) {
							BlockSplitter.insertBlockBetween(mth, pred, loopEnd);
							change = true;
						}
					}
					if (change) {
						return true;
					}
				}
			}
		}
		return splitReturn(mth);
	}

	/**
	 * Splice return block if several predecessors presents
	 */
	private static boolean splitReturn(MethodNode mth) {
		if (mth.getExitBlocks().size() != 1) {
			return false;
		}
		BlockNode exitBlock = mth.getExitBlocks().get(0);
		if (exitBlock.getInstructions().size() != 1
				|| exitBlock.contains(AFlag.SYNTHETIC)) {
			return false;
		}
		List<BlockNode> preds = exitBlock.getPredecessors();
		if (preds.size() < 2) {
			return false;
		}
		preds = BlockUtils.filterPredecessors(exitBlock);
		if (preds.size() < 2) {
			return false;
		}
		InsnNode returnInsn = exitBlock.getInstructions().get(0);
		if (returnInsn.getArgsCount() != 0 && !isReturnArgAssignInPred(preds, returnInsn)) {
			return false;
		}
		boolean first = true;
		for (BlockNode pred : preds) {
			BlockNode newRetBlock = BlockSplitter.startNewBlock(mth, exitBlock.getStartOffset());
			newRetBlock.add(AFlag.SYNTHETIC);
			InsnNode newRetInsn;
			if (first) {
				newRetInsn = returnInsn;
				newRetBlock.add(AFlag.ORIG_RETURN);
				first = false;
			} else {
				newRetInsn = duplicateReturnInsn(returnInsn);
			}
			newRetBlock.getInstructions().add(newRetInsn);
			removeConnection(pred, exitBlock);
			connect(pred, newRetBlock);
		}
		cleanExitNodes(mth);
		return true;
	}

	private static boolean isReturnArgAssignInPred(List<BlockNode> preds, InsnNode returnInsn) {
		RegisterArg arg = (RegisterArg) returnInsn.getArg(0);
		int regNum = arg.getRegNum();
		for (BlockNode pred : preds) {
			for (InsnNode insnNode : pred.getInstructions()) {
				RegisterArg result = insnNode.getResult();
				if (result != null && result.getRegNum() == regNum) {
					return true;
				}
			}
		}
		return false;
	}

	private static void cleanExitNodes(MethodNode mth) {
		for (Iterator<BlockNode> iterator = mth.getExitBlocks().iterator(); iterator.hasNext(); ) {
			BlockNode exitBlock = iterator.next();
			if (exitBlock.getPredecessors().isEmpty()) {
				mth.getBasicBlocks().remove(exitBlock);
				iterator.remove();
			}
		}
	}

	private static InsnNode duplicateReturnInsn(InsnNode returnInsn) {
		InsnNode insn = new InsnNode(returnInsn.getType(), returnInsn.getArgsCount());
		if (returnInsn.getArgsCount() == 1) {
			RegisterArg arg = (RegisterArg) returnInsn.getArg(0);
			insn.addArg(InsnArg.reg(arg.getRegNum(), arg.getType()));
		}
		insn.copyAttributesFrom(returnInsn);
		insn.setOffset(returnInsn.getOffset());
		insn.setSourceLine(returnInsn.getSourceLine());
		return insn;
	}

	private static void removeBlocks(MethodNode mth) {
		Iterator<BlockNode> it = mth.getBasicBlocks().iterator();
		while (it.hasNext()) {
			BlockNode block = it.next();
			if (block.contains(AFlag.REMOVE)) {
				if (!block.getPredecessors().isEmpty()
						|| !block.getSuccessors().isEmpty()) {
					LOG.error("Block {} not deleted, method: {}", block, mth);
				} else {
					CatchAttr catchAttr = block.get(AType.CATCH_BLOCK);
					if (catchAttr != null) {
						catchAttr.getTryBlock().removeBlock(mth, block);
					}
					it.remove();
				}
			}
		}
	}

	private static void clearBlocksState(MethodNode mth) {
		for (BlockNode block : mth.getBasicBlocks()) {
			block.remove(AType.LOOP);
			block.remove(AFlag.LOOP_START);
			block.remove(AFlag.LOOP_END);

			block.setDoms(null);
			block.setIDom(null);
			block.setDomFrontier(null);
			block.getDominatesOn().clear();
		}
	}
}
