package jadx.core.dex.visitors.regions;

import jadx.core.Consts;
import jadx.core.dex.attributes.AttributeFlag;
import jadx.core.dex.attributes.AttributeType;
import jadx.core.dex.attributes.AttributesList;
import jadx.core.dex.attributes.IAttribute;
import jadx.core.dex.attributes.LoopAttr;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.SwitchNode;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.Edge;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.IfCondition;
import jadx.core.dex.regions.IfInfo;
import jadx.core.dex.regions.IfRegion;
import jadx.core.dex.regions.LoopRegion;
import jadx.core.dex.regions.Region;
import jadx.core.dex.regions.SwitchRegion;
import jadx.core.dex.regions.SynchronizedRegion;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.InstructionRemover;
import jadx.core.utils.RegionUtils;

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

import static jadx.core.dex.regions.IfCondition.Mode;
import static jadx.core.utils.BlockUtils.getBlockByOffset;
import static jadx.core.utils.BlockUtils.isPathExists;
import static jadx.core.utils.BlockUtils.selectOther;

public class RegionMaker {
	private static final Logger LOG = LoggerFactory.getLogger(RegionMaker.class);

	private final MethodNode mth;
	private BitSet processedBlocks;

	public RegionMaker(MethodNode mth) {
		this.mth = mth;
		if (Consts.DEBUG) {
			this.processedBlocks = new BitSet(mth.getBasicBlocks().size());
		}
	}

	public Region makeRegion(BlockNode startBlock, RegionStack stack) {
		if (Consts.DEBUG) {
			int id = startBlock.getId();
			if (processedBlocks.get(id)) {
				LOG.debug(" Block already processed: " + startBlock + ", mth: " + mth);
			} else {
				processedBlocks.set(id);
			}
		}

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

		if (next != null && !stack.containsExit(block) && !stack.containsExit(next)) {
			return next;
		} else {
			return null;
		}
	}

	private BlockNode processLoop(IRegion curRegion, LoopAttr loop, RegionStack stack) {
		BlockNode loopStart = loop.getStart();
		Set<BlockNode> exitBlocksSet = loop.getExitNodes();

		// set exit blocks scan order by priority
		// this can help if loop have several exits (after using 'break' or 'return' in loop)
		List<BlockNode> exitBlocks = new ArrayList<BlockNode>(exitBlocksSet.size());
		BlockNode nextStart = BlockUtils.getNextBlock(loopStart);
		if (nextStart != null && exitBlocksSet.remove(nextStart)) {
			exitBlocks.add(nextStart);
		}
		if (exitBlocksSet.remove(loop.getEnd())) {
			exitBlocks.add(loop.getEnd());
		}
		if (exitBlocksSet.remove(loopStart)) {
			exitBlocks.add(loopStart);
		}
		exitBlocks.addAll(exitBlocksSet);
		exitBlocksSet = null;

		IfNode ifnode = null;
		LoopRegion loopRegion = null;

		// exit block with loop condition
		BlockNode condBlock = null;

		// first block in loop
		BlockNode bThen = null;

		for (BlockNode exit : exitBlocks) {
			if (exit.getAttributes().contains(AttributeType.EXC_HANDLER)
					|| exit.getInstructions().size() != 1) {
				continue;
			}
			InsnNode insn = exit.getInstructions().get(0);
			if (insn.getType() != InsnType.IF) {
				continue;
			}
			ifnode = (IfNode) insn;
			condBlock = exit;

			loopRegion = new LoopRegion(curRegion, condBlock, condBlock == loop.getEnd());
			boolean found = true;
			if (condBlock != loopStart) {
				if (condBlock.getPredecessors().contains(loopStart)) {
					loopRegion.setPreCondition(loopStart);
					// if we can't merge pre-condition this is not correct header
					found = loopRegion.checkPreCondition();
				} else {
					found = false;
				}
			}
			if (!found) {
				ifnode = null;
				loopRegion = null;
				condBlock = null;
				// try another exit
				continue;
			}

			List<BlockNode> merged = new ArrayList<BlockNode>(2);
			IfInfo mergedIf = mergeNestedIfNodes(condBlock,
					ifnode.getThenBlock(), ifnode.getElseBlock(), merged);
			if (mergedIf != null) {
				condBlock = mergedIf.getIfnode();
				if (!loop.getLoopBlocks().contains(mergedIf.getThenBlock())) {
					// invert loop condition if it points to exit
					loopRegion.setCondition(IfCondition.invert(mergedIf.getCondition()));
					bThen = mergedIf.getElseBlock();
				} else {
					loopRegion.setCondition(mergedIf.getCondition());
					bThen = mergedIf.getThenBlock();
				}
				exitBlocks.removeAll(merged);
			}
			break;
		}

		// endless loop
		if (loopRegion == null) {
			return makeEndlessLoop(curRegion, stack, loop, loopStart);
		}

		if (bThen == null) {
			bThen = ifnode.getThenBlock();
		}
		BlockNode loopBody = null;
		for (BlockNode s : condBlock.getSuccessors()) {
			if (loop.getLoopBlocks().contains(s)) {
				loopBody = s;
				break;
			}
		}

		curRegion.getSubBlocks().add(loopRegion);
		stack.push(loopRegion);

		exitBlocks.remove(condBlock);
		if (exitBlocks.size() > 0) {
			// add 'break' instruction before path cross between main loop exit and subexit
			BlockNode loopExit = BlockUtils.selectOther(loopBody, condBlock.getCleanSuccessors());
			for (Edge exitEdge : loop.getExitEdges()) {
				if (!exitBlocks.contains(exitEdge.getSource())) {
					continue;
				}
				insertBreak(stack, loopExit, exitEdge);
			}
		}

		BlockNode out;
		if (loopRegion.isConditionAtEnd()) {
			BlockNode bElse = ifnode.getElseBlock();
			out = (bThen == loopStart ? bElse : bThen);

			loopStart.getAttributes().remove(AttributeType.LOOP);

			stack.addExit(loop.getEnd());
			loopRegion.setBody(makeRegion(loopStart, stack));
			loopStart.getAttributes().add(loop);
		} else {
			if (bThen != loopBody) {
				loopRegion.setCondition(IfCondition.invert(loopRegion.getCondition()));
			}
			out = selectOther(loopBody, condBlock.getSuccessors());
			AttributesList outAttrs = out.getAttributes();
			if (outAttrs.contains(AttributeFlag.LOOP_START)
					&& outAttrs.get(AttributeType.LOOP) != loop
					&& stack.peekRegion() instanceof LoopRegion) {
				LoopRegion outerLoop = (LoopRegion) stack.peekRegion();
				boolean notYetProcessed = outerLoop.getBody() == null;
				if (notYetProcessed || RegionUtils.isRegionContainsBlock(outerLoop, out)) {
					// exit to outer loop which already processed
					out = null;
				}
			}
			stack.addExit(out);
			loopRegion.setBody(makeRegion(loopBody, stack));
		}
		stack.pop();
		return out;
	}

	private BlockNode makeEndlessLoop(IRegion curRegion, RegionStack stack, LoopAttr loop, BlockNode loopStart) {
		LoopRegion loopRegion;
		loopRegion = new LoopRegion(curRegion, null, false);
		curRegion.getSubBlocks().add(loopRegion);

		loopStart.getAttributes().remove(AttributeType.LOOP);
		stack.push(loopRegion);
		Region body = makeRegion(loopStart, stack);
		if (!RegionUtils.isRegionContainsBlock(body, loop.getEnd())) {
			body.getSubBlocks().add(loop.getEnd());
		}
		loopRegion.setBody(body);
		stack.pop();
		loopStart.getAttributes().add(loop);

		BlockNode next = BlockUtils.getNextBlock(loop.getEnd());
		return RegionUtils.isRegionContainsBlock(body, next) ? null : next;
	}

	private void insertBreak(RegionStack stack, BlockNode loopExit, Edge exitEdge) {
		BlockNode prev = null;
		BlockNode exit = exitEdge.getTarget();
		while (exit != null) {
			if (prev != null && isPathExists(loopExit, exit)) {
				// found cross
				if (!exit.getAttributes().contains(AttributeFlag.RETURN)) {
					prev.getInstructions().add(new InsnNode(InsnType.BREAK, 0));
					stack.addExit(exit);
				}
				return;
			}
			prev = exit;
			exit = BlockUtils.getNextBlock(exit);
		}
	}

	private final Set<BlockNode> cacheSet = new HashSet<BlockNode>();

	private BlockNode processMonitorEnter(IRegion curRegion, BlockNode block, InsnNode insn, RegionStack stack) {
		SynchronizedRegion synchRegion = new SynchronizedRegion(curRegion, insn);
		synchRegion.getSubBlocks().add(block);
		curRegion.getSubBlocks().add(synchRegion);

		Set<BlockNode> exits = new HashSet<BlockNode>();
		cacheSet.clear();
		traverseMonitorExits(synchRegion, insn.getArg(0), block, exits, cacheSet);

		for (InsnNode exitInsn : synchRegion.getExitInsns()) {
			InstructionRemover.unbindInsn(exitInsn);
		}

		block = BlockUtils.getNextBlock(block);
		BlockNode exit;
		if (exits.size() == 1) {
			exit = BlockUtils.getNextBlock(exits.iterator().next());
		} else {
			cacheSet.clear();
			exit = traverseMonitorExitsCross(block, exits, cacheSet);
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
	private static void traverseMonitorExits(SynchronizedRegion region, InsnArg arg, BlockNode block,
	                                         Set<BlockNode> exits, Set<BlockNode> visited) {
		visited.add(block);
		for (InsnNode insn : block.getInstructions()) {
			if (insn.getType() == InsnType.MONITOR_EXIT
					&& insn.getArg(0).equals(arg)) {
				exits.add(block);
				region.getExitInsns().add(insn);
				return;
			}
		}
		for (BlockNode node : block.getCleanSuccessors()) {
			if (!visited.contains(node)) {
				traverseMonitorExits(region, arg, node, exits, visited);
			}
		}
	}

	/**
	 * Traverse from monitor-enter thru successors and search for exit paths cross
	 */
	private static BlockNode traverseMonitorExitsCross(BlockNode block, Set<BlockNode> exits, Set<BlockNode> visited) {
		visited.add(block);
		for (BlockNode node : block.getCleanSuccessors()) {
			boolean cross = true;
			for (BlockNode exitBlock : exits) {
				boolean p = isPathExists(exitBlock, node);
				if (!p) {
					cross = false;
					break;
				}
			}
			if (cross) {
				return node;
			}
			if (!visited.contains(node)) {
				BlockNode res = traverseMonitorExitsCross(node, exits, visited);
				if (res != null) {
					return res;
				}
			}
		}
		return null;
	}

	private BlockNode processIf(IRegion currentRegion, BlockNode block, IfNode ifnode, RegionStack stack) {
		BlockNode bThen = ifnode.getThenBlock();
		BlockNode bElse = ifnode.getElseBlock();

		if (block.getAttributes().contains(AttributeFlag.SKIP)) {
			// block already included in other if region
			return bThen;
		}

		BlockNode out = null;
		BlockNode thenBlock = null;
		BlockNode elseBlock = null;

		for (BlockNode d : block.getDominatesOn()) {
			if (d != bThen && d != bElse) {
				out = d;
				break;
			}
		}

		IfRegion ifRegion = new IfRegion(currentRegion, block);
		currentRegion.getSubBlocks().add(ifRegion);

		IfInfo mergedIf = mergeNestedIfNodes(block, bThen, bElse, null);
		if (mergedIf != null) {
			block = mergedIf.getIfnode();
			ifRegion.setCondition(mergedIf.getCondition());
			thenBlock = mergedIf.getThenBlock();
			elseBlock = mergedIf.getElseBlock();
			bThen = thenBlock;
			bElse = elseBlock;
		}

		if (thenBlock == null) {
			// invert condition (compiler often do it)
			ifnode.invertCondition();
			BlockNode tmp = bThen;
			bThen = bElse;
			bElse = tmp;

			thenBlock = bThen;
			// select else and exit blocks
			if (block.getDominatesOn().size() == 2) {
				elseBlock = bElse;
			} else {
				if (bElse.getPredecessors().size() != 1) {
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
			if (BlockUtils.isBackEdge(block, out)) {
				out = null;
			}
		}

		if (elseBlock != null) {
			if (stack.containsExit(elseBlock)) {
				elseBlock = null;
			} else if (elseBlock.getAttributes().contains(AttributeFlag.RETURN)) {
				out = elseBlock;
				elseBlock = null;
			}
		}

		stack.push(ifRegion);
		stack.addExit(out);

		ifRegion.setThenRegion(makeRegion(thenBlock, stack));
		ifRegion.setElseRegion(elseBlock == null ? null : makeRegion(elseBlock, stack));

		stack.pop();
		return out;
	}

	private IfInfo mergeNestedIfNodes(BlockNode block, BlockNode bThen, BlockNode bElse, List<BlockNode> merged) {
		if (bThen == bElse) {
			return null;
		}

		boolean found;
		IfCondition condition = IfCondition.fromIfBlock(block);
		IfInfo result = null;
		do {
			found = false;
			for (BlockNode succ : block.getSuccessors()) {
				BlockNode nestedIfBlock = getIfNode(succ);
				if (nestedIfBlock != null && nestedIfBlock != block) {
					IfNode nestedIfInsn = (IfNode) nestedIfBlock.getInstructions().get(0);
					BlockNode nbThen = nestedIfInsn.getThenBlock();
					BlockNode nbElse = nestedIfInsn.getElseBlock();

					boolean inverted = false;
					IfCondition nestedCondition = IfCondition.fromIfNode(nestedIfInsn);
					if (isPathExists(bElse, nestedIfBlock)) {
						// else branch
						if (!isEqualPaths(bThen, nbThen)) {
							if (!isEqualPaths(bThen, nbElse)) {
								// not connected conditions
								break;
							}
							nestedIfInsn.invertCondition();
							inverted = true;
						}
						condition = IfCondition.merge(Mode.OR, condition, nestedCondition);
					} else {
						// then branch
						if (!isEqualPaths(bElse, nbElse)) {
							if (!isEqualPaths(bElse, nbThen)) {
								// not connected conditions
								break;
							}
							nestedIfInsn.invertCondition();
							inverted = true;
						}
						condition = IfCondition.merge(Mode.AND, condition, nestedCondition);
					}
					result = new IfInfo();
					result.setCondition(condition);
					nestedIfBlock.getAttributes().add(AttributeFlag.SKIP);
					skipSimplePath(bThen);

					if (merged != null) {
						merged.add(nestedIfBlock);
					}

					// set new blocks
					result.setIfnode(nestedIfBlock);
					result.setThenBlock(inverted ? nbElse : nbThen);
					result.setElseBlock(inverted ? nbThen : nbElse);

					found = true;
					block = nestedIfBlock;
					bThen = result.getThenBlock();
					bElse = result.getElseBlock();
					break;
				}
			}
		} while (found);

		return result;
	}

	private static BlockNode getIfNode(BlockNode block) {
		if (block != null && !block.getAttributes().contains(AttributeType.LOOP)) {
			List<InsnNode> insns = block.getInstructions();
			if (insns.size() == 1 && insns.get(0).getType() == InsnType.IF) {
				return block;
			}
			// skip block
			List<BlockNode> successors = block.getSuccessors();
			if (successors.size() == 1) {
				BlockNode next = successors.get(0);
				boolean pass = true;
				if (block.getInstructions().size() != 0) {
					for (InsnNode insn : block.getInstructions()) {
						RegisterArg res = insn.getResult();
						if (res == null) {
							pass = false;
							break;
						}
						List<InsnArg> useList = res.getTypedVar().getUseList();
						if (useList.size() != 2) {
							pass = false;
							break;
						} else {
							InsnArg arg = useList.get(1);
							InsnNode usePlace = arg.getParentInsn();
							if (!BlockUtils.blockContains(block, usePlace) && !BlockUtils.blockContains(next, usePlace)) {
								pass = false;
								break;
							}
						}
					}
				}
				if (pass) {
					return getIfNode(next);
				}
			}
		}
		return null;
	}

	private BlockNode processSwitch(IRegion currentRegion, BlockNode block, SwitchNode insn, RegionStack stack) {
		SwitchRegion sw = new SwitchRegion(currentRegion, block);
		currentRegion.getSubBlocks().add(sw);

		int len = insn.getTargets().length;
		// sort by target
		Map<Integer, List<Object>> casesMap = new LinkedHashMap<Integer, List<Object>>(len);
		for (int i = 0; i < len; i++) {
			Object key = insn.getKeys()[i];
			int targ = insn.getTargets()[i];
			List<Object> keys = casesMap.get(targ);
			if (keys == null) {
				keys = new ArrayList<Object>(2);
				casesMap.put(targ, keys);
			}
			keys.add(key);
		}

		Map<BlockNode, List<Object>> blocksMap = new LinkedHashMap<BlockNode, List<Object>>(len);
		for (Entry<Integer, List<Object>> entry : casesMap.entrySet()) {
			BlockNode c = getBlockByOffset(entry.getKey(), block.getSuccessors());
			assert c != null;
			blocksMap.put(c, entry.getValue());
		}

		BitSet succ = BlockUtils.blocksToBitSet(mth, block.getSuccessors());
		BitSet domsOn = BlockUtils.blocksToBitSet(mth, block.getDominatesOn());
		domsOn.and(succ); // filter 'out' block

		BlockNode defCase = getBlockByOffset(insn.getDefaultCaseOffset(), block.getSuccessors());
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
				for (BlockNode s : b.getCleanSuccessors()) {
					if (domsOn.get(s.getId())) {
						domsOn.clear(s.getId());
					}
				}
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
			for (BlockNode e : BlockUtils.bitSetToBlocks(mth, domsOn)) {
				stack.addExit(e);
			}
		}

		if (!stack.containsExit(defCase)) {
			sw.setDefaultCase(makeRegion(defCase, stack));
		}
		for (Entry<BlockNode, List<Object>> entry : blocksMap.entrySet()) {
			BlockNode c = entry.getKey();
			if (stack.containsExit(c)) {
				// empty case block
				sw.addCase(entry.getValue(), new Region(stack.peekRegion()));
			} else {
				sw.addCase(entry.getValue(), makeRegion(c, stack));
			}
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
		if (out != null) {
			stack.addExit(out);
		}
		// TODO extract finally part which exists in all handlers from same try block
		// TODO add blocks common for several handlers to some region
		handler.setHandlerRegion(makeRegion(start, stack));

		IAttribute excHandlerAttr = start.getAttributes().get(AttributeType.EXC_HANDLER);
		handler.getHandlerRegion().getAttributes().add(excHandlerAttr);
	}

	private void skipSimplePath(BlockNode block) {
		while (block != null
				&& block.getCleanSuccessors().size() < 2
				&& block.getPredecessors().size() == 1) {
			block.getAttributes().add(AttributeFlag.SKIP);
			block = BlockUtils.getNextBlock(block);
		}
	}

	private static boolean isEqualPaths(BlockNode b1, BlockNode b2) {
		if (b1 == b2) {
			return true;
		}
		if (b1 == null || b2 == null) {
			return false;
		}
		if (isReturnBlocks(b1, b2)) {
			return true;
		}
		if (isSyntheticPath(b1, b2)) {
			return true;
		}
		return false;
	}

	private static boolean isSyntheticPath(BlockNode b1, BlockNode b2) {
		if (!b1.isSynthetic() || !b2.isSynthetic()) {
			return false;
		}
		BlockNode n1 = BlockUtils.getNextBlock(b1);
		BlockNode n2 = BlockUtils.getNextBlock(b2);
		return isEqualPaths(n1, n2);
	}

	private static boolean isReturnBlocks(BlockNode b1, BlockNode b2) {
		if (!b1.isReturnBlock() || !b2.isReturnBlock()) {
			return false;
		}
		List<InsnNode> b1Insns = b1.getInstructions();
		List<InsnNode> b2Insns = b2.getInstructions();
		if (b1Insns.size() != 1 || b2Insns.size() != 1) {
			return false;
		}
		InsnNode i1 = b1Insns.get(0);
		InsnNode i2 = b2Insns.get(0);
		if (i1.getArgsCount() == 0 || i2.getArgsCount() == 0) {
			return false;
		}
		return i1.getArg(0).equals(i2.getArg(0));
	}
}
