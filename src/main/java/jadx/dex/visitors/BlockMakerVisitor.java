package jadx.dex.visitors;

import jadx.dex.attributes.AttributeFlag;
import jadx.dex.attributes.AttributeType;
import jadx.dex.attributes.AttributesList;
import jadx.dex.attributes.IAttribute;
import jadx.dex.attributes.JumpAttribute;
import jadx.dex.attributes.LoopAttr;
import jadx.dex.instructions.IfNode;
import jadx.dex.instructions.InsnType;
import jadx.dex.instructions.args.InsnArg;
import jadx.dex.instructions.args.RegisterArg;
import jadx.dex.nodes.BlockNode;
import jadx.dex.nodes.InsnNode;
import jadx.dex.nodes.MethodNode;
import jadx.dex.trycatch.CatchAttr;
import jadx.dex.trycatch.ExceptionHandler;
import jadx.dex.trycatch.SplitterBlockAttr;
import jadx.utils.exceptions.JadxRuntimeException;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BlockMakerVisitor extends AbstractVisitor {

	// leave these instructions alone in block node
	private static final Set<InsnType> separateInsns = EnumSet.of(
			InsnType.RETURN,
			InsnType.IF,
			InsnType.SWITCH,
			InsnType.MONITOR_ENTER,
			InsnType.MONITOR_EXIT);

	private static int nextBlockId;

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode())
			return;

		mth.initBasicBlocks();
		makeBasicBlocks(mth);
		BlockProcessingHelper.visit(mth);
		mth.finishBasicBlocks();
	}

	private static void makeBasicBlocks(MethodNode mth) {
		nextBlockId = 0;

		InsnNode prevInsn = null;
		Map<Integer, BlockNode> blocksMap = new HashMap<Integer, BlockNode>();
		BlockNode curBlock = startNewBlock(mth, 0);
		mth.setEnterBlock(curBlock);

		// split into blocks
		for (InsnNode insn : mth.getInstructions()) {
			boolean startNew = false;
			if (prevInsn != null) {
				InsnType type = prevInsn.getType();
				if (type == InsnType.RETURN
						|| type == InsnType.GOTO
						|| type == InsnType.THROW
						|| separateInsns.contains(type)) {

					if (type == InsnType.RETURN || type == InsnType.THROW)
						mth.addExitBlock(curBlock);

					BlockNode block = startNewBlock(mth, insn.getOffset());
					if (type == InsnType.MONITOR_ENTER || type == InsnType.MONITOR_EXIT)
						connect(curBlock, block);
					curBlock = block;
					startNew = true;
				} else {
					type = insn.getType();
					startNew = separateInsns.contains(type);

					List<IAttribute> pjumps = prevInsn.getAttributes().getAll(AttributeType.JUMP);
					if (pjumps.size() > 0) {
						for (IAttribute j : pjumps) {
							JumpAttribute jump = (JumpAttribute) j;
							if (jump.getSrc() == prevInsn.getOffset())
								startNew = true;
						}
					}

					List<IAttribute> cjumps = insn.getAttributes().getAll(AttributeType.JUMP);
					if (cjumps.size() > 0) {
						for (IAttribute j : cjumps) {
							JumpAttribute jump = (JumpAttribute) j;
							if (jump.getDest() == insn.getOffset())
								startNew = true;
						}
					}

					// split 'do-while' block (last instruction: 'if', target this block)
					if (type == InsnType.IF) {
						IfNode ifs = (IfNode) (insn);
						BlockNode targBlock = blocksMap.get(ifs.getTarget());
						if (targBlock == curBlock)
							startNew = true;
					}

					if (startNew) {
						BlockNode block = startNewBlock(mth, insn.getOffset());
						connect(curBlock, block);
						curBlock = block;
					}
				}
			}

			// for try/catch make empty block for connect handlers
			if (insn.getAttributes().contains(AttributeFlag.TRY_ENTER)) {
				BlockNode block;
				if (insn.getOffset() != 0 && !startNew) {
					block = startNewBlock(mth, insn.getOffset());
					connect(curBlock, block);
					curBlock = block;
				}
				blocksMap.put(insn.getOffset(), curBlock);

				// add this insn in new block
				block = startNewBlock(mth, -1);
				curBlock.getAttributes().add(AttributeFlag.SYNTHETIC);
				block.getAttributes().add(new SplitterBlockAttr(curBlock));
				connect(curBlock, block);
				curBlock = block;
			} else {
				blocksMap.put(insn.getOffset(), curBlock);
			}

			curBlock.getInstructions().add(insn);
			prevInsn = insn;
		}

		// setup missing connections
		for (BlockNode block : mth.getBasicBlocks()) {
			for (InsnNode insn : block.getInstructions()) {
				List<IAttribute> jumps = insn.getAttributes().getAll(AttributeType.JUMP);
				for (IAttribute attr : jumps) {
					JumpAttribute jump = (JumpAttribute) attr;
					BlockNode srcBlock = getBlock(mth, jump.getSrc(), blocksMap);
					BlockNode thisblock = getBlock(mth, jump.getDest(), blocksMap);
					connect(srcBlock, thisblock);
				}

				// connect exception handlers
				CatchAttr catches = (CatchAttr) insn.getAttributes().get(AttributeType.CATCH_BLOCK);
				if (catches != null) {
					// get synthetic block for handlers
					IAttribute spl = block.getAttributes().get(AttributeType.SPLITTER_BLOCK);
					if (spl != null) {
						BlockNode connBlock = ((SplitterBlockAttr) spl).getBlock();
						for (ExceptionHandler h : catches.getTryBlock().getHandlers()) {
							BlockNode destBlock = getBlock(mth, h.getHandleOffset(), blocksMap);
							// skip self loop in handler
							if (connBlock != destBlock)
								// && !connBlock.getPredecessors().contains(destBlock))
								connect(connBlock, destBlock);
						}
					}
				}
			}
		}
		computeDominators(mth);
		markReturnBlocks(mth);

		int i = 0;
		while (modifyBlocksTree(mth)) {
			// revert calculations
			cleanDomTree(mth);
			// recalculate dominators tree
			computeDominators(mth);
			markReturnBlocks(mth);

			i++;
			if (i > 100)
				throw new AssertionError("Can't fix method cfg: " + mth);
		}
	}

	private static BlockNode getBlock(MethodNode mth, int offset, Map<Integer, BlockNode> blocksMap) {
		BlockNode block = blocksMap.get(offset);
		assert block != null;
		return block;
	}

	private static void connect(BlockNode from, BlockNode to) {
		if (!from.getSuccessors().contains(to))
			from.getSuccessors().add(to);
		if (!to.getPredecessors().contains(from))
			to.getPredecessors().add(from);
	}

	private static void removeConnection(BlockNode from, BlockNode to) {
		from.getSuccessors().remove(to);
		to.getPredecessors().remove(from);
	}

	private static BlockNode startNewBlock(MethodNode mth, int offset) {
		BlockNode block = new BlockNode(mth, ++nextBlockId, offset);
		mth.getBasicBlocks().add(block);
		return block;
	}

	private static void computeDominators(MethodNode mth) {
		int nBlocks = mth.getBasicBlocks().size();
		for (int i = 0; i < nBlocks; i++) {
			BlockNode block = mth.getBasicBlocks().get(i);
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
			for (BlockNode block : mth.getBasicBlocks()) {
				if (block == entryBlock)
					continue;

				BitSet d = block.getDoms();

				dset.clear();
				dset.or(d);
				for (BlockNode pred : block.getPredecessors()) {
					d.and(pred.getDoms());
				}
				d.set(block.getId());

				if (!d.equals(dset))
					changed = true;
			}
		} while (changed);

		markLoops(mth);

		// clear self dominance
		for (BlockNode block : mth.getBasicBlocks()) {
			block.getDoms().clear(block.getId());
		}

		// calculate immediate dominators
		for (BlockNode block : mth.getBasicBlocks()) {
			if (block == entryBlock)
				continue;

			if (block.getPredecessors().size() == 1) {
				block.setIDom(block.getPredecessors().get(0));
			} else {
				BitSet bs = new BitSet(block.getDoms().length());
				bs.or(block.getDoms());

				for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
					BlockNode dom = mth.getBasicBlocks().get(i);
					bs.andNot(dom.getDoms());
				}

				int c = bs.cardinality();
				if (c == 1) {
					int id = bs.nextSetBit(0);
					BlockNode idom = mth.getBasicBlocks().get(id);
					block.setIDom(idom);
					idom.getDominatesOn().add(block);
				} else {
					throw new JadxRuntimeException("Can't find immediate dominator for block " + block
							+ " in " + bs + " prec:" + block.getPredecessors());
				}
			}
		}
	}

	private static void markLoops(MethodNode mth) {
		for (BlockNode block : mth.getBasicBlocks()) {
			for (BlockNode succ : block.getSuccessors()) {
				// Every successor that dominates its predecessor is a header of a loop,
				// block -> succ is a back edge.
				if (block.getDoms().get(succ.getId())) {
					succ.getAttributes().add(AttributeFlag.LOOP_START);
					block.getAttributes().add(AttributeFlag.LOOP_END);

					LoopAttr loop = new LoopAttr(succ, block);
					succ.getAttributes().add(loop);
					block.getAttributes().add(loop);
				}
			}
		}
	}

	private static void markReturnBlocks(MethodNode mth) {
		for (BlockNode block : mth.getBasicBlocks()) {
			if (block.getInstructions().size() == 1) {
				if (block.getInstructions().get(0).getType() == InsnType.RETURN)
					block.getAttributes().add(AttributeFlag.RETURN);
			}
		}
	}

	private static boolean modifyBlocksTree(MethodNode mth) {
		for (BlockNode block : mth.getBasicBlocks()) {
			if (block.getPredecessors().isEmpty() && block != mth.getEnterBlock()) {
				throw new JadxRuntimeException("Unreachable block: " + block);
			}

			// check loops
			List<IAttribute> loops = block.getAttributes().getAll(AttributeType.LOOP);
			if (loops.size() > 1) {
				boolean oneHeader = true;
				for (IAttribute a : loops) {
					LoopAttr loop = (LoopAttr) a;
					if (loop.getStart() != block) {
						oneHeader = false;
						break;
					}
				}
				if (oneHeader) {
					// several back edges connected to one loop header => make additional block
					BlockNode newLoopHeader = startNewBlock(mth, block.getStartOffset());
					connect(newLoopHeader, block);
					for (IAttribute a : loops) {
						LoopAttr la = (LoopAttr) a;
						BlockNode node = la.getEnd();
						removeConnection(node, block);
						connect(node, newLoopHeader);
					}
					return true;
				}
			}

			// splice return block if several precessors presents
			if (block.getAttributes().contains(AttributeFlag.RETURN)
					&& block.getPredecessors().size() > 1
					&& !block.getInstructions().get(0).getAttributes().contains(AttributeType.CATCH_BLOCK)) {
				List<BlockNode> preds = new ArrayList<BlockNode>(block.getPredecessors());

				BlockNode origRetBlock = block;
				origRetBlock.getPredecessors().clear();
				origRetBlock.getPredecessors().add(preds.get(0));
				preds.remove(0);

				InsnNode origReturnInsn = origRetBlock.getInstructions().get(0);
				RegisterArg retArg = null;
				if (origReturnInsn.getArgsCount() != 0)
					retArg = (RegisterArg) origReturnInsn.getArg(0);

				for (BlockNode pred : preds) {
					pred.getSuccessors().remove(origRetBlock);
					// make copy of return block and connect to predecessor
					BlockNode newRetBlock = startNewBlock(mth, origRetBlock.getStartOffset());

					InsnNode ret = new InsnNode(InsnType.RETURN, 1);
					if (retArg != null)
						ret.addArg(InsnArg.reg(retArg.getRegNum(), retArg.getType()));
					ret.getAttributes().addAll(origReturnInsn.getAttributes());

					newRetBlock.getInstructions().add(ret);
					newRetBlock.getAttributes().add(AttributeFlag.RETURN);

					connect(pred, newRetBlock);
					mth.addExitBlock(newRetBlock);
				}
				return true;
			}

			// TODO detect ternary operator
		}
		return false;
	}

	private static void cleanDomTree(MethodNode mth) {
		for (BlockNode block : mth.getBasicBlocks()) {
			AttributesList attrs = block.getAttributes();
			attrs.remove(AttributeType.LOOP);
			attrs.remove(AttributeFlag.LOOP_START);
			attrs.remove(AttributeFlag.LOOP_END);

			block.setDoms(null);
			block.setIDom(null);
			block.getDominatesOn().clear();
		}
	}

}
