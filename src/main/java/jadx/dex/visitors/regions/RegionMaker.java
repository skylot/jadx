package jadx.dex.visitors.regions;

import jadx.dex.attributes.AttributeFlag;
import jadx.dex.attributes.AttributeType;
import jadx.dex.attributes.AttributesList;
import jadx.dex.attributes.ForceReturnAttr;
import jadx.dex.attributes.IAttribute;
import jadx.dex.attributes.LoopAttr;
import jadx.dex.instructions.IfNode;
import jadx.dex.instructions.InsnType;
import jadx.dex.instructions.SwitchNode;
import jadx.dex.instructions.args.RegisterArg;
import jadx.dex.nodes.BlockNode;
import jadx.dex.nodes.IRegion;
import jadx.dex.nodes.InsnNode;
import jadx.dex.nodes.MethodNode;
import jadx.dex.regions.IfRegion;
import jadx.dex.regions.LoopRegion;
import jadx.dex.regions.Region;
import jadx.dex.regions.SwitchRegion;
import jadx.dex.regions.SynchronizedRegion;
import jadx.dex.trycatch.ExceptionHandler;
import jadx.dex.visitors.InstructionRemover;
import jadx.utils.BlockUtils;
import jadx.utils.ErrorsCounter;
import jadx.utils.RegionUtils;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegionMaker {
	private static final Logger LOG = LoggerFactory.getLogger(RegionMaker.class);

	private final MethodNode mth;

	public RegionMaker(MethodNode mth) {
		this.mth = mth;
	}

	public Region makeRegion(BlockNode startBlock, RegionStack stack) {
		Region r = new Region(stack.peekRegion());
		BlockNode next = startBlock;
		while (next != null) {
			next = traverse(r, next, stack);
		}
		return r;
	}

	/**
	 * Recursively traverse all blocks from 'block' until block from 'exits'
	 */
	private BlockNode traverse(IRegion r, BlockNode block, RegionStack stack) {
		BlockNode next = null;
		boolean processed = false;

		AttributesList attrs = block.getAttributes();
		int loopCount = attrs.getCount(AttributeType.LOOP);
		if (loopCount != 0 && attrs.contains(AttributeFlag.LOOP_START)) {
			if (loopCount == 1) {
				LoopAttr loop = (LoopAttr) attrs.get(AttributeType.LOOP);
				next = processLoop(r, loop, stack);
				processed = true;
			} else {
				List<IAttribute> loops = attrs.getAll(AttributeType.LOOP);
				for (IAttribute a : loops) {
					LoopAttr loop = (LoopAttr) a;
					if (loop.getStart() == block) {
						next = processLoop(r, loop, stack);
						processed = true;
						break;
					}
				}
			}
		}

		if (!processed) {
			int size = block.getInstructions().size();
			if (size == 1) {
				InsnNode insn = block.getInstructions().get(0);
				switch (insn.getType()) {
					case IF:
						next = processIf(r, block, (IfNode) insn, stack);
						processed = true;
						break;

					case SWITCH:
						next = processSwitch(r, block, (SwitchNode) insn, stack);
						processed = true;
						break;

					case MONITOR_ENTER:
						next = processMonitorEnter(r, block, insn, stack);
						processed = true;
						break;

					default:
						break;
				}
			}
		}

		if (!processed) {
			r.getSubBlocks().add(block);
			next = BlockUtils.getNextBlock(block);
		}

		if (!stack.containsExit(block) && next != null && !stack.containsExit(next))
			return next;
		else
			return null;
	}

	private BlockNode processLoop(IRegion curRegion, LoopAttr loop, RegionStack stack) {
		BlockNode loopStart = loop.getStart();
		IfNode ifnode = null;
		LoopRegion loopRegion = null;
		Set<BlockNode> exitBlocksSet = loop.getExitNodes();
		// set exit blocks scan order by priority
		// this can help if we have several exit from loop (after using 'break' or 'return' in loop)
		List<BlockNode> exitBlocks = new ArrayList<BlockNode>(exitBlocksSet.size());
		if (exitBlocksSet.contains(loop.getEnd())) {
			exitBlocks.add(loop.getEnd());
			exitBlocksSet.remove(loop.getEnd());
		}
		if (exitBlocksSet.contains(loopStart)) {
			exitBlocks.add(loopStart);
			exitBlocksSet.remove(loopStart);
		}
		exitBlocks.addAll(exitBlocksSet);
		exitBlocksSet = null;

		BlockNode condBlock = null; // exit block with loop condition

		for (BlockNode exit : exitBlocks) {
			if (exit.getAttributes().contains(AttributeType.EXC_HANDLER)
					|| exit.getInstructions().size() != 1)
				continue;

			InsnNode insn = exit.getInstructions().get(0);
			if (insn.getType() == InsnType.IF) {
				ifnode = (IfNode) insn;
				condBlock = exit;
				loopRegion = new LoopRegion(curRegion, condBlock, condBlock == loop.getEnd());
				if (!loopRegion.isConditionAtEnd() && condBlock != loop.getStart()
						&& condBlock.getPredecessors().contains(loopStart)) {
					loopRegion.setPreCondition(loopStart);
					// if we can't merge pre-condition this is not correct header
					if (!loopRegion.checkPreCondition()) {
						ifnode = null;
						loopRegion = null;
						// try another exit
						continue;
					}
				}
				break;
			}
		}

		// endless loop
		if (loopRegion == null) {
			loopRegion = new LoopRegion(curRegion, null, false);
			curRegion.getSubBlocks().add(loopRegion);

			loopStart.getAttributes().remove(AttributeType.LOOP);
			stack.push(loopRegion);
			Region body = makeRegion(loopStart, stack);
			if (!RegionUtils.isRegionContainsBlock(body, loop.getEnd()))
				body.getSubBlocks().add(loop.getEnd());
			loopRegion.setBody(body);
			stack.pop();
			loopStart.getAttributes().add(loop);

			return BlockUtils.getNextBlock(loop.getEnd());
		}

		stack.push(loopRegion);
		curRegion.getSubBlocks().add(loopRegion);

		exitBlocks.remove(condBlock);
		if (exitBlocks.size() > 0) {
			// set BREAK or FORCE_RETURN attributes
			// before path cross between main loop exit and subexit
			BlockNode loopExit = BlockUtils.getNextBlock(condBlock);
			for (BlockNode exit : exitBlocks) {
				BlockNode next = BlockUtils.getNextBlock(exit);
				while (next != null) {
					if (BlockUtils.isPathExists(loopExit, next)) {
						// found cross
						if (next.getCleanSuccessors().size() == 1) {
							BlockNode r = BlockUtils.getNextBlock(next);
							if (r != null && r.getAttributes().contains(AttributeFlag.RETURN)) {
								next.getAttributes().add(new ForceReturnAttr(r.getInstructions().get(0)));
							} else {
								next.getAttributes().add(AttributeFlag.BREAK);
							}
						}
						stack.addExit(next);
						break;
					}
					next = BlockUtils.getNextBlock(next);
				}
			}
		}

		BlockNode bThen = BlockUtils.getBlockByOffset(ifnode.getTarget(), condBlock.getSuccessors());
		BlockNode out;
		if (loopRegion.isConditionAtEnd()) {
			BlockNode bElse = BlockUtils.selectOther(bThen, condBlock.getSuccessors());
			out = (bThen == loopStart ? bElse : bThen);

			loopStart.getAttributes().remove(AttributeType.LOOP);
			stack.addExit(loop.getEnd());
			loopRegion.setBody(makeRegion(loopStart, stack));
			loopStart.getAttributes().add(loop);
		} else {
			Set<BlockNode> loopBlocks = loop.getLoopBlocks();
			BlockNode loopBody = null;
			for (BlockNode s : condBlock.getSuccessors()) {
				if (loopBlocks.contains(s)) {
					loopBody = s;
					break;
				}
			}
			if (bThen != loopBody)
				ifnode.invertOp(bThen.getStartOffset());

			out = BlockUtils.selectOther(loopBody, condBlock.getSuccessors());
			AttributesList outAttrs = out.getAttributes();
			if (outAttrs.contains(AttributeFlag.LOOP_START)
					&& outAttrs.get(AttributeType.LOOP) != loop) {
				// exit to outer loop which already processed
				out = null;
			}
			stack.addExit(out);
			loopRegion.setBody(makeRegion(loopBody, stack));
		}
		stack.pop();
		return out;
	}

	private static final Set<BlockNode> cacheSet = new HashSet<BlockNode>();

	private BlockNode processMonitorEnter(IRegion curRegion, BlockNode block, InsnNode insn, RegionStack stack) {
		RegisterArg arg = (RegisterArg) insn.getArg(0);
		SynchronizedRegion synchRegion = new SynchronizedRegion(curRegion, arg);
		synchRegion.getSubBlocks().add(block);
		curRegion.getSubBlocks().add(synchRegion);

		Set<BlockNode> exits = new HashSet<BlockNode>();
		cacheSet.clear();
		traverseMonitorExits(arg, block, exits, cacheSet);

		block = BlockUtils.getNextBlock(block);
		BlockNode exit;
		if (exits.size() == 1) {
			exit = BlockUtils.getNextBlock(exits.iterator().next());
		} else {
			cacheSet.clear();
			exit = traverseMonitorExitsCross(block, exits, cacheSet);
			// LOG.debug("synchronized exits: " + exits + ", cross: " + exit);
		}

		stack.push(synchRegion);
		stack.addExit(exit);
		synchRegion.getSubBlocks().add(makeRegion(block, stack));
		stack.pop();
		return exit;
	}

	/**
	 * Traverse from monitor-enter thru successors and collect blocks contains monitor-exit
	 */
	private void traverseMonitorExits(RegisterArg arg, BlockNode block, Set<BlockNode> exits, Set<BlockNode> visited) {
		visited.add(block);
		for (InsnNode insn : block.getInstructions()) {
			if (insn.getType() == InsnType.MONITOR_EXIT
					&& insn.getArg(0).equals(arg)) {
				exits.add(block);
				InstructionRemover.remove(block, insn);
				return;
			}
		}
		for (BlockNode node : block.getCleanSuccessors()) {
			if (!visited.contains(node))
				traverseMonitorExits(arg, node, exits, visited);
		}
	}

	/**
	 * Traverse from monitor-enter thru successors and search for exit paths cross
	 */
	private BlockNode traverseMonitorExitsCross(BlockNode block, Set<BlockNode> exits, Set<BlockNode> visited) {
		visited.add(block);
		for (BlockNode node : block.getCleanSuccessors()) {
			boolean cross = true;
			for (BlockNode exitBlock : exits) {
				boolean p = BlockUtils.isPathExists(exitBlock, node);
				if (!p) {
					cross = false;
					break;
				}
			}
			if (cross)
				return node;

			if (!visited.contains(node)) {
				BlockNode res = traverseMonitorExitsCross(node, exits, visited);
				if (res != null)
					return res;
			}
		}
		return null;
	}

	private BlockNode processIf(IRegion currentRegion, BlockNode block, IfNode ifnode, RegionStack stack) {
		BlockNode bElse = BlockUtils.getBlockByOffset(ifnode.getTarget(), block.getSuccessors());
		BlockNode bThen;
		if (block.getSuccessors().size() == 1) {
			// TODO eliminate useless 'if' instruction
			bThen = bElse;
		} else {
			bThen = BlockUtils.selectOther(bElse, block.getSuccessors());
		}

		ifnode.invertOp(bThen.getStartOffset());

		BlockNode out = null;
		BlockNode thenBlock;
		BlockNode elseBlock = null;

		thenBlock = bThen;
		// select else and exit blocks
		if (block.getDominatesOn().size() == 2) {
			if (bElse.getPredecessors().size() == 1)
				elseBlock = bElse;
			else
				out = bElse;
		} else {
			if (bElse.getPredecessors().size() != 1) {
				elseBlock = null;
				out = bElse;
			} else {
				elseBlock = bElse;
				for (BlockNode d : block.getDominatesOn()) {
					if (d != bThen && d != bElse) {
						out = d;
						break;
					}
				}
			}
		}

		if (stack.containsExit(elseBlock))
			elseBlock = null;

		IfRegion ifRegion = new IfRegion(currentRegion, block);
		currentRegion.getSubBlocks().add(ifRegion);

		stack.push(ifRegion);
		stack.addExit(out);

		ifRegion.setThenRegion(makeRegion(thenBlock, stack));
		ifRegion.setElseRegion(elseBlock == null ? null : makeRegion(elseBlock, stack));

		stack.pop();
		return out;
	}

	private BlockNode processSwitch(IRegion currentRegion, BlockNode block, SwitchNode insn, RegionStack stack) {
		SwitchRegion sw = new SwitchRegion(currentRegion, block);
		currentRegion.getSubBlocks().add(sw);

		int len = insn.getTargets().length;
		// sort by target
		Map<Integer, List<Integer>> casesMap = new LinkedHashMap<Integer, List<Integer>>(len);
		for (int i = 0; i < len; i++) {
			int key = insn.getKeys()[i];
			int targ = insn.getTargets()[i];
			List<Integer> keys = casesMap.get(targ);
			if (keys == null) {
				keys = new ArrayList<Integer>(1);
				casesMap.put(targ, keys);
			}
			keys.add(key);
		}

		Map<BlockNode, List<Integer>> blocksMap = new LinkedHashMap<BlockNode, List<Integer>>(len);

		for (Entry<Integer, List<Integer>> entry : casesMap.entrySet()) {
			BlockNode c = BlockUtils.getBlockByOffset(entry.getKey(), block.getSuccessors());
			assert c != null;
			blocksMap.put(c, entry.getValue());
		}

		BitSet succ = BlockUtils.blocksToBitSet(mth, block.getSuccessors());
		BitSet domsOn = BlockUtils.blocksToBitSet(mth, block.getDominatesOn());
		domsOn.andNot(succ); // filter 'out' block

		BlockNode defCase = BlockUtils.getBlockByOffset(insn.getDefaultCaseOffset(), block.getSuccessors());
		if (defCase != null) {
			blocksMap.remove(defCase);
		}

		int outCount = domsOn.cardinality();
		if (outCount > 1) {
			// remove exception handlers
			BlockUtils.cleanBitSet(mth, domsOn);
			outCount = domsOn.cardinality();
		}
		if (outCount > 1) {
			// filter successors of other blocks
			for (int i = domsOn.nextSetBit(0); i >= 0; i = domsOn.nextSetBit(i + 1)) {
				BlockNode b = mth.getBasicBlocks().get(i);
				for (BlockNode s : b.getCleanSuccessors())
					if (domsOn.get(s.getId()))
						domsOn.clear(s.getId());
			}
			outCount = domsOn.cardinality();
		}

		BlockNode out = null;
		if (outCount == 1) {
			out = mth.getBasicBlocks().get(domsOn.nextSetBit(0));
		} else if (outCount == 0) {
			// default and out blocks are same
			out = defCase;
		}

		stack.push(sw);
		if (out != null) {
			stack.addExit(out);
		} else {
			for (BlockNode e : BlockUtils.bitsetToBlocks(mth, domsOn))
				stack.addExit(e);
		}

		if (!stack.containsExit(defCase)) {
			sw.setDefaultCase(makeRegion(defCase, stack));
		}
		for (Entry<BlockNode, List<Integer>> entry : blocksMap.entrySet()) {
			BlockNode c = entry.getKey();
			sw.addCase(entry.getValue(), makeRegion(c, stack));
		}

		stack.pop();
		return out;
	}

	public void processExcHandler(ExceptionHandler handler, RegionStack stack) {
		BlockNode start = handler.getHandleBlock();
		if (start == null) {
			LOG.debug(ErrorsCounter.formatErrorMsg(mth, "No exception handler block: " + handler));
			return;
		}

		BlockNode out = BlockUtils.traverseWhileDominates(start, start);
		if (out != null)
			stack.addExit(out);
		// TODO extract finally part which exists in all handlers from same try block
		// TODO add blocks common for several handlers to some region
		handler.setHandlerRegion(makeRegion(start, stack));

		IAttribute excHandlerAttr = start.getAttributes().get(AttributeType.EXC_HANDLER);
		handler.getHandlerRegion().getAttributes().add(excHandlerAttr);
	}

}
