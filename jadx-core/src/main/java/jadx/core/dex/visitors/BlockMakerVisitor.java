package jadx.core.dex.visitors;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.JumpInfo;
import jadx.core.dex.attributes.nodes.LoopInfo;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.Edge;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.trycatch.CatchAttr;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.dex.trycatch.SplitterBlockAttr;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static jadx.core.utils.EmptyBitSet.EMPTY;

public class BlockMakerVisitor extends AbstractVisitor {

	// leave these instructions alone in block node
	private static final Set<InsnType> SEPARATE_INSNS = EnumSet.of(
			InsnType.RETURN,
			InsnType.IF,
			InsnType.SWITCH,
			InsnType.MONITOR_ENTER,
			InsnType.MONITOR_EXIT
	);

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}
		mth.initBasicBlocks();
		splitBasicBlocks(mth);
		processBlocksTree(mth);
		BlockProcessingHelper.visit(mth);
		mth.finishBasicBlocks();
	}

	private static void splitBasicBlocks(MethodNode mth) {
		InsnNode prevInsn = null;
		Map<Integer, BlockNode> blocksMap = new HashMap<Integer, BlockNode>();
		BlockNode curBlock = startNewBlock(mth, 0);
		mth.setEnterBlock(curBlock);

		// split into blocks
		for (InsnNode insn : mth.getInstructions()) {
			if (insn == null) {
				continue;
			}
			boolean startNew = false;
			if (prevInsn != null) {
				InsnType type = prevInsn.getType();
				if (type == InsnType.GOTO
						|| type == InsnType.THROW
						|| SEPARATE_INSNS.contains(type)) {

					if (type == InsnType.RETURN || type == InsnType.THROW) {
						mth.addExitBlock(curBlock);
					}
					BlockNode block = startNewBlock(mth, insn.getOffset());
					if (type == InsnType.MONITOR_ENTER || type == InsnType.MONITOR_EXIT) {
						connect(curBlock, block);
					}
					curBlock = block;
					startNew = true;
				} else {
					startNew = isSplitByJump(prevInsn, insn)
							|| SEPARATE_INSNS.contains(insn.getType())
							|| isDoWhile(blocksMap, curBlock, insn);
					if (startNew) {
						BlockNode block = startNewBlock(mth, insn.getOffset());
						connect(curBlock, block);
						curBlock = block;
					}
				}
			}
			// for try/catch make empty block for connect handlers
			if (insn.contains(AFlag.TRY_ENTER)) {
				BlockNode block;
				if (insn.getOffset() != 0 && !startNew) {
					block = startNewBlock(mth, insn.getOffset());
					connect(curBlock, block);
					curBlock = block;
				}
				blocksMap.put(insn.getOffset(), curBlock);

				// add this insn in new block
				block = startNewBlock(mth, -1);
				curBlock.add(AFlag.SYNTHETIC);
				SplitterBlockAttr splitter = new SplitterBlockAttr(curBlock);
				block.addAttr(splitter);
				curBlock.addAttr(splitter);
				connect(curBlock, block);
				curBlock = block;
			} else {
				blocksMap.put(insn.getOffset(), curBlock);
			}
			curBlock.getInstructions().add(insn);
			prevInsn = insn;
		}
		// setup missing connections
		setupConnections(mth, blocksMap);
	}

	private static void setupConnections(MethodNode mth, Map<Integer, BlockNode> blocksMap) {
		for (BlockNode block : mth.getBasicBlocks()) {
			for (InsnNode insn : block.getInstructions()) {
				List<JumpInfo> jumps = insn.getAll(AType.JUMP);
				for (JumpInfo jump : jumps) {
					BlockNode srcBlock = getBlock(jump.getSrc(), blocksMap);
					BlockNode thisBlock = getBlock(jump.getDest(), blocksMap);
					connect(srcBlock, thisBlock);
				}

				// connect exception handlers
				CatchAttr catches = insn.get(AType.CATCH_BLOCK);
				// get synthetic block for handlers
				SplitterBlockAttr spl = block.get(AType.SPLITTER_BLOCK);
				if (catches != null && spl != null) {
					BlockNode splitterBlock = spl.getBlock();
					boolean tryEnd = insn.contains(AFlag.TRY_LEAVE);
					for (ExceptionHandler h : catches.getTryBlock().getHandlers()) {
						BlockNode handlerBlock = getBlock(h.getHandleOffset(), blocksMap);
						// skip self loop in handler
						if (splitterBlock != handlerBlock) {
							connect(splitterBlock, handlerBlock);
						}
						if (tryEnd) {
							connect(block, handlerBlock);
						}
					}
				}
			}
		}
	}

	private static boolean isSplitByJump(InsnNode prevInsn, InsnNode currentInsn) {
		List<JumpInfo> pJumps = prevInsn.getAll(AType.JUMP);
		for (JumpInfo jump : pJumps) {
			if (jump.getSrc() == prevInsn.getOffset()) {
				return true;
			}
		}
		List<JumpInfo> cJumps = currentInsn.getAll(AType.JUMP);
		for (JumpInfo jump : cJumps) {
			if (jump.getDest() == currentInsn.getOffset()) {
				return true;
			}
		}
		return false;
	}

	private static boolean isDoWhile(Map<Integer, BlockNode> blocksMap, BlockNode curBlock, InsnNode insn) {
		// split 'do-while' block (last instruction: 'if', target this block)
		if (insn.getType() == InsnType.IF) {
			IfNode ifs = (IfNode) (insn);
			BlockNode targetBlock = blocksMap.get(ifs.getTarget());
			if (targetBlock == curBlock) {
				return true;
			}
		}
		return false;
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

	private static BlockNode getBlock(int offset, Map<Integer, BlockNode> blocksMap) {
		BlockNode block = blocksMap.get(offset);
		assert block != null;
		return block;
	}

	private static void connect(BlockNode from, BlockNode to) {
		if (!from.getSuccessors().contains(to)) {
			from.getSuccessors().add(to);
		}
		if (!to.getPredecessors().contains(from)) {
			to.getPredecessors().add(from);
		}
	}

	private static void removeConnection(BlockNode from, BlockNode to) {
		from.getSuccessors().remove(to);
		to.getPredecessors().remove(from);
	}

	private static BlockNode startNewBlock(MethodNode mth, int offset) {
		BlockNode block = new BlockNode(mth.getBasicBlocks().size(), offset);
		mth.getBasicBlocks().add(block);
		return block;
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
		for (BlockNode c : block.getDominatesOn()) {
			computeBlockDF(mth, c);
		}
		BitSet domFrontier = null;
		for (BlockNode s : block.getSuccessors()) {
			if (s.getIDom() != block) {
				if (domFrontier == null) {
					domFrontier = new BitSet();
				}
				domFrontier.set(s.getId());
			}
		}
		for (BlockNode c : block.getDominatesOn()) {
			BitSet frontier = c.getDomFrontier();
			for (int p = frontier.nextSetBit(0); p >= 0; p = frontier.nextSetBit(p + 1)) {
				if (mth.getBasicBlocks().get(p).getIDom() != block) {
					if (domFrontier == null) {
						domFrontier = new BitSet();
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
					BlockNode newLoopHeader = startNewBlock(mth, block.getStartOffset());
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
			// insert additional blocks if loop has several exits
			if (loops.size() == 1) {
				LoopInfo loop = loops.get(0);
				List<Edge> edges = loop.getExitEdges();
				if (!edges.isEmpty()) {
					boolean change = false;
					for (Edge edge : edges) {
						BlockNode target = edge.getTarget();
						if (!target.contains(AFlag.SYNTHETIC)) {
							insertBlockBetween(mth, edge.getSource(), target);
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

	private static BlockNode insertBlockBetween(MethodNode mth, BlockNode source, BlockNode target) {
		BlockNode newBlock = startNewBlock(mth, target.getStartOffset());
		newBlock.add(AFlag.SYNTHETIC);
		removeConnection(source, target);
		connect(source, newBlock);
		connect(newBlock, target);
		return newBlock;
	}

	/**
	 * Splice return block if several predecessors presents
	 */
	private static boolean splitReturn(MethodNode mth) {
		if (mth.getExitBlocks().size() != 1) {
			return false;
		}
		BlockNode exitBlock = mth.getExitBlocks().get(0);
		if (exitBlock.getPredecessors().size() > 1
				&& exitBlock.getInstructions().size() == 1
				&& !exitBlock.contains(AFlag.SYNTHETIC)) {
			InsnNode returnInsn = exitBlock.getInstructions().get(0);
			List<BlockNode> preds = new ArrayList<BlockNode>(exitBlock.getPredecessors());
			if (returnInsn.getArgsCount() != 0 && !isReturnArgAssignInPred(preds, returnInsn)) {
				return false;
			}
			boolean first = true;
			for (BlockNode pred : preds) {
				BlockNode newRetBlock = startNewBlock(mth, exitBlock.getStartOffset());
				newRetBlock.add(AFlag.SYNTHETIC);
				InsnNode newRetInsn;
				if (first) {
					newRetInsn = returnInsn;
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
		return false;
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
