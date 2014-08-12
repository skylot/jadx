package jadx.core.dex.visitors.regions;

import jadx.core.Consts;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.LoopInfo;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.SwitchNode;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.Edge;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
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

import static jadx.core.dex.visitors.regions.IfMakerHelper.makeIfInfo;
import static jadx.core.dex.visitors.regions.IfMakerHelper.mergeNestedIfNodes;
import static jadx.core.utils.BlockUtils.getBlockByOffset;
import static jadx.core.utils.BlockUtils.getNextBlock;
import static jadx.core.utils.BlockUtils.isPathExists;

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

		if (!processed && block.getInstructions().size() == 1) {
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
		if (!processed) {
			r.getSubBlocks().add(block);
			next = getNextBlock(block);
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

		// set exit blocks scan order priority
		// this can help if loop have several exits (after using 'break' or 'return' in loop)
		List<BlockNode> exitBlocks = new ArrayList<BlockNode>(exitBlocksSet.size());
		BlockNode nextStart = getNextBlock(loopStart);
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

		LoopRegion loopRegion = makeLoopRegion(curRegion, loop, exitBlocks);
		if (loopRegion == null) {
			return makeEndlessLoop(curRegion, stack, loop, loopStart);
		}
		curRegion.getSubBlocks().add(loopRegion);
		IRegion outerRegion = stack.peekRegion();
		stack.push(loopRegion);

		IfInfo info = makeIfInfo(loopRegion.getHeader());
		IfInfo condInfo = mergeNestedIfNodes(info);
		if (condInfo == null) {
			condInfo = info;
		}
		if (!loop.getLoopBlocks().contains(condInfo.getThenBlock())) {
			// invert loop condition if 'then' points to exit
			condInfo = IfInfo.invert(condInfo);
		}
		loopRegion.setCondition(condInfo.getCondition());
		exitBlocks.removeAll(condInfo.getMergedBlocks());

		if (exitBlocks.size() > 0) {
			BlockNode loopExit = condInfo.getElseBlock();
			if (loopExit != null) {
				// add 'break' instruction before path cross between main loop exit and subexit
				for (Edge exitEdge : loop.getExitEdges()) {
					if (!exitBlocks.contains(exitEdge.getSource())) {
						continue;
					}
					tryInsertBreak(stack, loopExit, exitEdge);
				}
			}
		}

		BlockNode out;
		if (loopRegion.isConditionAtEnd()) {
			BlockNode thenBlock = condInfo.getThenBlock();
			out = (thenBlock == loopStart ? condInfo.getElseBlock() : thenBlock);
			loopStart.remove(AType.LOOP);
			loop.getEnd().add(AFlag.SKIP);
			stack.addExit(loop.getEnd());
			loopRegion.setBody(makeRegion(loopStart, stack));
			loopStart.addAttr(AType.LOOP, loop);
			loop.getEnd().remove(AFlag.SKIP);
		} else {
			out = condInfo.getElseBlock();
			if (outerRegion != null
					&& out.contains(AFlag.LOOP_START)
					&& !out.getAll(AType.LOOP).contains(loop)
					&& RegionUtils.isRegionContainsBlock(outerRegion, out)) {
				// exit to already processed outer loop
				out = null;
			}
			stack.addExit(out);
			BlockNode loopBody = condInfo.getThenBlock();
			Region body = makeRegion(loopBody, stack);
			// add blocks from loop start to first condition block
			BlockNode conditionBlock = condInfo.getIfBlock();
			if (loopStart != conditionBlock) {
				Set<BlockNode> blocks = BlockUtils.getAllPathsBlocks(loopStart, conditionBlock);
				blocks.remove(conditionBlock);
				for (BlockNode block : blocks) {
					if (block.getInstructions().isEmpty()
							&& !block.contains(AFlag.SKIP)
							&& !RegionUtils.isRegionContainsBlock(body, block)) {
						body.add(block);
					}
				}
			}
			loopRegion.setBody(body);
		}
		stack.pop();
		return out;
	}

	/**
	 * Select loop exit and construct LoopRegion
	 */
	private LoopRegion makeLoopRegion(IRegion curRegion, LoopInfo loop, List<BlockNode> exitBlocks) {
		for (BlockNode block : exitBlocks) {
			if (block.contains(AType.EXC_HANDLER)
					|| block.getInstructions().size() != 1
					|| block.getInstructions().get(0).getType() != InsnType.IF) {
				continue;
			}
			LoopRegion loopRegion = new LoopRegion(curRegion, block, block == loop.getEnd());
			boolean found;
			if (block == loop.getStart() || block == loop.getEnd()
					|| BlockUtils.isEmptySimplePath(loop.getStart(), block)) {
				found = true;
			} else if (block.getPredecessors().contains(loop.getStart())) {
				loopRegion.setPreCondition(loop.getStart());
				// if we can't merge pre-condition this is not correct header
				found = loopRegion.checkPreCondition();
			} else {
				found = false;
			}
			if (found) {
				return loopRegion;
			}
		}
		// no exit found => endless loop
		return null;
	}

	private BlockNode makeEndlessLoop(IRegion curRegion, RegionStack stack, LoopInfo loop, BlockNode loopStart) {
		LoopRegion loopRegion = new LoopRegion(curRegion, null, false);
		curRegion.getSubBlocks().add(loopRegion);

		loopStart.remove(AType.LOOP);
		stack.push(loopRegion);

		BlockNode loopExit = null;
		// insert 'break' for exits
		List<Edge> exitEdges = loop.getExitEdges();
		if (exitEdges.size() == 1) {
			for (Edge exitEdge : exitEdges) {
				BlockNode exit = exitEdge.getTarget();
				if (canInsertBreak(exit)) {
					exit.getInstructions().add(new InsnNode(InsnType.BREAK, 0));
					BlockNode nextBlock = getNextBlock(exit);
					if (nextBlock != null) {
						stack.addExit(nextBlock);
						loopExit = nextBlock;
					}
				}
			}
		}

		Region body = makeRegion(loopStart, stack);
		BlockNode loopEnd = loop.getEnd();
		if (!RegionUtils.isRegionContainsBlock(body, loopEnd)
				&& !loopEnd.contains(AType.EXC_HANDLER)) {
			body.getSubBlocks().add(loopEnd);
		}
		loopRegion.setBody(body);

		if (loopExit == null) {
			BlockNode next = getNextBlock(loopEnd);
			loopExit = RegionUtils.isRegionContainsBlock(body, next) ? null : next;
		}
		stack.pop();
		loopStart.addAttr(AType.LOOP, loop);
		return loopExit;
	}

	private boolean canInsertBreak(BlockNode exit) {
		if (exit.contains(AFlag.RETURN)) {
			return false;
		}
		List<BlockNode> simplePath = BlockUtils.buildSimplePath(exit);
		if (!simplePath.isEmpty()
				&& simplePath.get(simplePath.size() - 1).contains(AFlag.RETURN)) {
			return false;
		}
		// check if there no outer switch (TODO: very expensive check)
		Set<BlockNode> paths = BlockUtils.getAllPathsBlocks(mth.getEnterBlock(), exit);
		for (BlockNode block : paths) {
			if (BlockUtils.checkLastInsnType(block, InsnType.SWITCH)) {
				return false;
			}
		}
		return true;
	}

	private void tryInsertBreak(RegionStack stack, BlockNode loopExit, Edge exitEdge) {
		BlockNode prev = null;
		BlockNode exit = exitEdge.getTarget();
		while (exit != null) {
			if (prev != null && isPathExists(loopExit, exit)) {
				// found cross
				if (canInsertBreak(exit)) {
					prev.getInstructions().add(new InsnNode(InsnType.BREAK, 0));
					stack.addExit(exit);
				}
				return;
			}
			prev = exit;
			exit = getNextBlock(exit);
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

		BlockNode body = getNextBlock(block);
		if (body == null) {
			mth.add(AFlag.INCONSISTENT_CODE);
			LOG.warn("Unexpected end of synchronized block");
			return null;
		}
		BlockNode exit;
		if (exits.size() == 1) {
			exit = getNextBlock(exits.iterator().next());
		} else {
			cacheSet.clear();
			exit = traverseMonitorExitsCross(body, exits, cacheSet);
		}

		stack.push(synchRegion);
		stack.addExit(exit);
		synchRegion.getSubBlocks().add(makeRegion(body, stack));
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

		IfInfo currentIf = makeIfInfo(block);
		IfInfo mergedIf = mergeNestedIfNodes(currentIf);
		if (mergedIf != null) {
			currentIf = mergedIf;
		} else {
			// invert simple condition (compiler often do it)
			currentIf = IfInfo.invert(currentIf);
		}
		currentIf = IfMakerHelper.restructureIf(mth, block, currentIf);
		if (currentIf == null) {
			// invalid merged if, check simple one again
			currentIf = makeIfInfo(block);
			currentIf = IfMakerHelper.restructureIf(mth, block, currentIf);
			if (currentIf == null) {
				// all attempts failed
				return null;
			}
		}

		IfRegion ifRegion = new IfRegion(currentRegion, block);
		ifRegion.setCondition(currentIf.getCondition());
		currentRegion.getSubBlocks().add(ifRegion);

		stack.push(ifRegion);
		stack.addExit(currentIf.getOutBlock());

		ifRegion.setThenRegion(makeRegion(currentIf.getThenBlock(), stack));
		BlockNode elseBlock = currentIf.getElseBlock();
		if (elseBlock == null || stack.containsExit(elseBlock)) {
			ifRegion.setElseRegion(null);
		} else {
			ifRegion.setElseRegion(makeRegion(elseBlock, stack));
		}

		stack.pop();
		return currentIf.getOutBlock();
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
					if (!isPathExists(s, maybeOut)) {
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
					List<BlockNode> s = splitter.getSuccessors();
					if (s.isEmpty()) {
						LOG.debug(ErrorsCounter.formatErrorMsg(mth, "No successors for splitter: " + splitter));
						continue;
					}
					BlockNode ss = s.get(0);
					BlockNode cross = BlockUtils.getPathCross(mth, ss, handler);
					if (cross != null && cross != ss && cross != handler) {
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

		BlockNode exit = BlockUtils.traverseWhileDominates(start, start);
		if (exit != null && RegionUtils.isRegionContainsBlock(mth.getRegion(), exit)) {
			stack.addExit(exit);
		}

		handler.setHandlerRegion(makeRegion(start, stack));

		ExcHandlerAttr excHandlerAttr = start.get(AType.EXC_HANDLER);
		handler.getHandlerRegion().addAttr(excHandlerAttr);
	}

	static void skipSimplePath(BlockNode block) {
		while (block != null
				&& block.getCleanSuccessors().size() < 2
				&& block.getPredecessors().size() == 1) {
			block.add(AFlag.SKIP);
			block = getNextBlock(block);
		}
	}

	static boolean isEqualPaths(BlockNode b1, BlockNode b2) {
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
		BlockNode n1 = getNextBlock(b1);
		BlockNode n2 = getNextBlock(b2);
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
