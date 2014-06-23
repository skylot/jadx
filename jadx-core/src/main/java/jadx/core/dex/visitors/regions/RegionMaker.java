package jadx.core.dex.visitors.regions;

import jadx.core.Consts;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.LoopInfo;
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
import jadx.core.dex.trycatch.ExcHandlerAttr;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.dex.trycatch.TryCatchBlock;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.InstructionRemover;
import jadx.core.utils.RegionUtils;
import jadx.core.utils.exceptions.JadxOverflowException;

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

	// 'dumb' guard to prevent endless loop in regions processing
	private static final int REGIONS_LIMIT = 1000 * 1000;

	private final MethodNode mth;
	private BitSet processedBlocks;
	private int regionsCount;

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
		regionsCount++;
		if (regionsCount > REGIONS_LIMIT) {
			throw new JadxOverflowException("Regions count limit reached");
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

		List<LoopInfo> loops = block.getAll(AType.LOOP);
		int loopCount = loops.size();
		if (loopCount != 0 && block.contains(AFlag.LOOP_START)) {
			if (loopCount == 1) {
				next = processLoop(r, loops.get(0), stack);
				processed = true;
			} else {
				for (LoopInfo loop : loops) {
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

	private BlockNode processLoop(IRegion curRegion, LoopInfo loop, RegionStack stack) {
		BlockNode loopStart = loop.getStart();
		Set<BlockNode> exitBlocksSet = loop.getExitNodes();

		// set exit blocks scan order by priority
		// this can help if loop have several exits (after using 'break' or 'return' in loop)
		List<BlockNode> exitBlocks = new ArrayList<BlockNode>(exitBlocksSet.size());
		BlockNode nextStart = BlockUtils.getNextBlock(loopStart);
		if (nextStart != null && exitBlocksSet.remove(nextStart)) {
			exitBlocks.add(nextStart);
		}
		if (exitBlocksSet.remove(loopStart)) {
			exitBlocks.add(loopStart);
		}
		if (exitBlocksSet.remove(loop.getEnd())) {
			exitBlocks.add(loop.getEnd());
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
			if (exit.contains(AType.EXC_HANDLER)
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
			if (condBlock != loopStart && condBlock != loop.getEnd()) {
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
			IfInfo mergedIf = mergeNestedIfNodes(condBlock, ifnode, merged);
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
			BlockNode loopExit = BlockUtils.selectOtherSafe(loopBody, condBlock.getCleanSuccessors());
			if (loopExit != null) {
				// add 'break' instruction before path cross between main loop exit and subexit
				for (Edge exitEdge : loop.getExitEdges()) {
					if (!exitBlocks.contains(exitEdge.getSource())) {
						continue;
					}
					insertBreak(stack, loopExit, exitEdge);
				}
			}
		}

		BlockNode out;
		if (loopRegion.isConditionAtEnd()) {
			BlockNode bElse = ifnode.getElseBlock();
			out = (bThen == loopStart ? bElse : bThen);

			loopStart.remove(AType.LOOP);

			stack.addExit(loop.getEnd());
			loopRegion.setBody(makeRegion(loopStart, stack));
			loopStart.addAttr(AType.LOOP, loop);
		} else {
			if (bThen != loopBody) {
				loopRegion.setCondition(IfCondition.invert(loopRegion.getCondition()));
			}
			out = selectOther(loopBody, condBlock.getSuccessors());
			if (out.contains(AFlag.LOOP_START)
					&& !out.getAll(AType.LOOP).contains(loop)
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

	private BlockNode makeEndlessLoop(IRegion curRegion, RegionStack stack, LoopInfo loop, BlockNode loopStart) {
		LoopRegion loopRegion;
		loopRegion = new LoopRegion(curRegion, null, false);
		curRegion.getSubBlocks().add(loopRegion);

		loopStart.remove(AType.LOOP);
		stack.push(loopRegion);
		Region body = makeRegion(loopStart, stack);
		if (!RegionUtils.isRegionContainsBlock(body, loop.getEnd())) {
			body.getSubBlocks().add(loop.getEnd());
		}
		loopRegion.setBody(body);
		stack.pop();
		loopStart.addAttr(AType.LOOP, loop);

		BlockNode next = BlockUtils.getNextBlock(loop.getEnd());
		return RegionUtils.isRegionContainsBlock(body, next) ? null : next;
	}

	private void insertBreak(RegionStack stack, BlockNode loopExit, Edge exitEdge) {
		BlockNode prev = null;
		BlockNode exit = exitEdge.getTarget();
		while (exit != null) {
			if (prev != null && isPathExists(loopExit, exit)) {
				// found cross
				if (!exit.contains(AFlag.RETURN)) {
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
			InstructionRemover.unbindInsn(mth, exitInsn);
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
		if (block.contains(AFlag.SKIP)) {
			// block already included in other 'if' region
			return ifnode.getThenBlock();
		}
		final BlockNode thenBlock;
		final BlockNode elseBlock;
		BlockNode out = null;

		IfRegion ifRegion = new IfRegion(currentRegion, block);
		currentRegion.getSubBlocks().add(ifRegion);

		IfInfo mergedIf = mergeNestedIfNodes(block, ifnode, null);
		if (mergedIf != null) {
			ifRegion.setCondition(mergedIf.getCondition());
			thenBlock = mergedIf.getThenBlock();
			elseBlock = mergedIf.getElseBlock();
			out = BlockUtils.getPathCross(mth, thenBlock, elseBlock);
		} else {
			// invert condition (compiler often do it)
			ifnode.invertCondition();
			final BlockNode bThen = ifnode.getThenBlock();
			final BlockNode bElse = ifnode.getElseBlock();

			// select 'then', 'else' and 'exit' blocks
			if (bElse.getPredecessors().size() != 1
					&& BlockUtils.isPathExists(bThen, bElse)) {
				thenBlock = bThen;
				elseBlock = null;
				out = bElse;
			} else if (bThen.getPredecessors().size() != 1
					&& BlockUtils.isPathExists(bElse, bThen)) {
				ifnode.invertCondition();
				thenBlock = ifnode.getThenBlock();
				elseBlock = null;
				out = ifnode.getElseBlock();
			} else if (block.getDominatesOn().size() == 2) {
				thenBlock = bThen;
				elseBlock = bElse;
				out = BlockUtils.getPathCross(mth, bThen, bElse);
			} else if (bElse.getPredecessors().size() != 1) {
				thenBlock = bThen;
				elseBlock = null;
				out = bElse;
			} else {
				thenBlock = bThen;
				elseBlock = bElse;
				for (BlockNode d : block.getDominatesOn()) {
					if (d != bThen && d != bElse) {
						out = d;
						break;
					}
				}
			}
			if (BlockUtils.isBackEdge(block, out)) {
				out = null;
			}
		}

		stack.push(ifRegion);
		stack.addExit(out);

		ifRegion.setThenRegion(makeRegion(thenBlock, stack));
		if (elseBlock == null || stack.containsExit(elseBlock)) {
			ifRegion.setElseRegion(null);
		} else {
			ifRegion.setElseRegion(makeRegion(elseBlock, stack));
		}

		stack.pop();
		return out;
	}

	private IfInfo mergeNestedIfNodes(BlockNode block, IfNode ifnode, List<BlockNode> merged) {
		IfInfo info = new IfInfo();
		info.setIfnode(block);
		info.setCondition(IfCondition.fromIfBlock(block));
		info.setThenBlock(ifnode.getThenBlock());
		info.setElseBlock(ifnode.getElseBlock());
		return mergeNestedIfNodes(info, merged);
	}

	private IfInfo mergeNestedIfNodes(IfInfo info, List<BlockNode> merged) {
		BlockNode bThen = info.getThenBlock();
		BlockNode bElse = info.getElseBlock();
		if (bThen == bElse) {
			return null;
		}

		BlockNode ifBlock = info.getIfnode();
		BlockNode nestedIfBlock = getNextIfBlock(ifBlock);
		if (nestedIfBlock == null) {
			return null;
		}

		IfNode nestedIfInsn = (IfNode) nestedIfBlock.getInstructions().get(0);
		IfCondition nestedCondition = IfCondition.fromIfNode(nestedIfInsn);
		BlockNode nbThen = nestedIfInsn.getThenBlock();
		BlockNode nbElse = nestedIfInsn.getElseBlock();

		IfCondition condition = info.getCondition();
		boolean inverted = false;
		if (isPathExists(bElse, nestedIfBlock)) {
			// else branch
			if (!isEqualPaths(bThen, nbThen)) {
				if (!isEqualPaths(bThen, nbElse)) {
					// not connected conditions
					return null;
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
					return null;
				}
				nestedIfInsn.invertCondition();
				inverted = true;
			}
			condition = IfCondition.merge(Mode.AND, condition, nestedCondition);
		}
		if (merged != null) {
			merged.add(nestedIfBlock);
		}
		nestedIfBlock.add(AFlag.SKIP);
		BlockNode blockToNestedIfBlock = BlockUtils.getNextBlockToPath(ifBlock, nestedIfBlock);
		skipSimplePath(BlockUtils.selectOther(blockToNestedIfBlock, ifBlock.getCleanSuccessors()));

		IfInfo result = new IfInfo();
		result.setIfnode(nestedIfBlock);
		result.setCondition(condition);
		result.setThenBlock(inverted ? nbElse : nbThen);
		result.setElseBlock(inverted ? nbThen : nbElse);

		// search next nested if block
		IfInfo next = mergeNestedIfNodes(result, merged);
		if (next != null) {
			return next;
		}
		return result;
	}

	private BlockNode getNextIfBlock(BlockNode block) {
		for (BlockNode succ : block.getSuccessors()) {
			BlockNode nestedIfBlock = getIfNode(succ);
			if (nestedIfBlock != null && nestedIfBlock != block) {
				return nestedIfBlock;
			}
		}
		return null;
	}

	private static BlockNode getIfNode(BlockNode block) {
		if (block != null && !block.contains(AType.LOOP)) {
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
						List<RegisterArg> useList = res.getSVar().getUseList();
						if (useList.size() != 1) {
							pass = false;
							break;
						} else {
							InsnArg arg = useList.get(0);
							InsnNode usePlace = arg.getParentInsn();
							if (!BlockUtils.blockContains(block, usePlace)
									&& !BlockUtils.blockContains(next, usePlace)) {
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
		domsOn.xor(succ); // filter 'out' block

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
			List<BlockNode> blocks = mth.getBasicBlocks();
			for (int i = domsOn.nextSetBit(0); i >= 0; i = domsOn.nextSetBit(i + 1)) {
				BlockNode b = blocks.get(i);
				for (BlockNode s : b.getCleanSuccessors()) {
					domsOn.clear(s.getId());
				}
			}
			outCount = domsOn.cardinality();
		}

		BlockNode out = null;
		if (outCount == 1) {
			out = mth.getBasicBlocks().get(domsOn.nextSetBit(0));
		} else if (outCount == 0) {
			// one or several case blocks are empty,
			// run expensive algorithm for find 'out' block
			for (BlockNode maybeOut : block.getSuccessors()) {
				boolean allReached = true;
				for (BlockNode s : block.getSuccessors()) {
					if (!BlockUtils.isPathExists(s, maybeOut)) {
						allReached = false;
						break;
					}
				}
				if (allReached) {
					out = maybeOut;
					break;
				}
			}
		}

		stack.push(sw);
		if (out != null) {
			stack.addExit(out);
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

	public void processTryCatchBlocks(MethodNode mth) {
		Set<TryCatchBlock> tcs = new HashSet<TryCatchBlock>();
		for (ExceptionHandler handler : mth.getExceptionHandlers()) {
			tcs.add(handler.getTryBlock());
		}
		for (TryCatchBlock tc : tcs) {
			List<BlockNode> blocks = new ArrayList<BlockNode>(tc.getHandlersCount());
			Set<BlockNode> splitters = new HashSet<BlockNode>();
			for (ExceptionHandler handler : tc.getHandlers()) {
				BlockNode handlerBlock = handler.getHandlerBlock();
				if (handlerBlock != null) {
					blocks.add(handlerBlock);
					splitters.addAll(handlerBlock.getPredecessors());
				} else {
					LOG.debug(ErrorsCounter.formatErrorMsg(mth, "No exception handler block: " + handler));
				}
			}
			Set<BlockNode> exits = new HashSet<BlockNode>();
			for (BlockNode splitter : splitters) {
				for (BlockNode handler : blocks) {
					List<BlockNode> s = splitter.getCleanSuccessors();
					if (s.isEmpty()) {
						LOG.debug(ErrorsCounter.formatErrorMsg(mth, "No successors for splitter: " + splitter));
						continue;
					}
					BlockNode cross = BlockUtils.getPathCross(mth, s.get(0), handler);
					if (cross != null) {
						exits.add(cross);
					}
				}
			}
			for (ExceptionHandler handler : tc.getHandlers()) {
				processExcHandler(handler, exits);
			}
		}
	}

	private void processExcHandler(ExceptionHandler handler, Set<BlockNode> exits) {
		BlockNode start = handler.getHandlerBlock();
		if (start == null) {
			return;
		}

		// TODO extract finally part which exists in all handlers from same try block
		// TODO add blocks common for several handlers to some region

		RegionStack stack = new RegionStack(mth);
		stack.addExits(exits);
		handler.setHandlerRegion(makeRegion(start, stack));

		ExcHandlerAttr excHandlerAttr = start.get(AType.EXC_HANDLER);
		handler.getHandlerRegion().addAttr(excHandlerAttr);
	}

	private void skipSimplePath(BlockNode block) {
		while (block != null
				&& block.getCleanSuccessors().size() < 2
				&& block.getPredecessors().size() == 1) {
			block.add(AFlag.SKIP);
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
