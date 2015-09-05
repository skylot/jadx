package jadx.core.dex.visitors.regions;

import jadx.core.Consts;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.EdgeInsnAttr;
import jadx.core.dex.attributes.nodes.LoopInfo;
import jadx.core.dex.attributes.nodes.LoopLabelAttr;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.SwitchNode;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.Edge;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.Region;
import jadx.core.dex.regions.SwitchRegion;
import jadx.core.dex.regions.SynchronizedRegion;
import jadx.core.dex.regions.conditions.IfInfo;
import jadx.core.dex.regions.conditions.IfRegion;
import jadx.core.dex.regions.loops.LoopRegion;
import jadx.core.dex.trycatch.ExcHandlerAttr;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.dex.trycatch.SplitterBlockAttr;
import jadx.core.dex.trycatch.TryCatchBlock;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.InstructionRemover;
import jadx.core.utils.RegionUtils;
import jadx.core.utils.exceptions.JadxOverflowException;
import jadx.core.utils.exceptions.JadxRuntimeException;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static jadx.core.dex.visitors.regions.IfMakerHelper.confirmMerge;
import static jadx.core.dex.visitors.regions.IfMakerHelper.makeIfInfo;
import static jadx.core.dex.visitors.regions.IfMakerHelper.mergeNestedIfNodes;
import static jadx.core.dex.visitors.regions.IfMakerHelper.searchNestedIf;
import static jadx.core.utils.BlockUtils.getBlockByOffset;
import static jadx.core.utils.BlockUtils.getNextBlock;
import static jadx.core.utils.BlockUtils.isPathExists;
import static jadx.core.utils.BlockUtils.skipSyntheticSuccessor;

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
				LOG.debug(" Block already processed: {}, mth: {}", startBlock, mth);
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
		}
		return null;
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
			BlockNode exit = makeEndlessLoop(curRegion, stack, loop, loopStart);
			insertContinue(loop);
			return exit;
		}
		curRegion.getSubBlocks().add(loopRegion);
		IRegion outerRegion = stack.peekRegion();
		stack.push(loopRegion);

		IfInfo condInfo = makeIfInfo(loopRegion.getHeader());
		condInfo = searchNestedIf(condInfo);
		confirmMerge(condInfo);
		if (!loop.getLoopBlocks().contains(condInfo.getThenBlock())) {
			// invert loop condition if 'then' points to exit
			condInfo = IfInfo.invert(condInfo);
		}
		loopRegion.setCondition(condInfo.getCondition());
		exitBlocks.removeAll(condInfo.getMergedBlocks());

		if (!exitBlocks.isEmpty()) {
			BlockNode loopExit = condInfo.getElseBlock();
			if (loopExit != null) {
				// add 'break' instruction before path cross between main loop exit and sub-exit
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
			BlockNode thenBlock = condInfo.getThenBlock();
			out = thenBlock == loopStart ? condInfo.getElseBlock() : thenBlock;
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
		insertContinue(loop);
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
			List<LoopInfo> loops = block.getAll(AType.LOOP);
			if (!loops.isEmpty() && loops.get(0) != loop) {
				// skip nested loop condition
				continue;
			}
			LoopRegion loopRegion = new LoopRegion(curRegion, loop, block, block == loop.getEnd());
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
				List<LoopInfo> list = mth.getAllLoopsForBlock(block);
				if (list.size() >= 2) {
					// bad condition if successors going out of all loops
					boolean allOuter = true;
					for (BlockNode outerBlock : block.getCleanSuccessors()) {
						List<LoopInfo> outLoopList = mth.getAllLoopsForBlock(outerBlock);
						outLoopList.remove(loop);
						if (!outLoopList.isEmpty()) {
							// goes to outer loop
							allOuter = false;
							break;
						}
					}
					if (allOuter) {
						found = false;
					}
				}
			}
			if (found) {
				return loopRegion;
			}
		}
		// no exit found => endless loop
		return null;
	}

	private BlockNode makeEndlessLoop(IRegion curRegion, RegionStack stack, LoopInfo loop, BlockNode loopStart) {
		LoopRegion loopRegion = new LoopRegion(curRegion, loop, null, false);
		curRegion.getSubBlocks().add(loopRegion);

		loopStart.remove(AType.LOOP);
		stack.push(loopRegion);

		BlockNode loopExit = null;
		// insert 'break' for exits
		List<Edge> exitEdges = loop.getExitEdges();
		for (Edge exitEdge : exitEdges) {
			BlockNode exit = exitEdge.getTarget();
			if (insertBreak(stack, exit, exitEdge)) {
				BlockNode nextBlock = getNextBlock(exit);
				if (nextBlock != null) {
					stack.addExit(nextBlock);
					loopExit = nextBlock;
				}
			}
		}

		Region body = makeRegion(loopStart, stack);
		BlockNode loopEnd = loop.getEnd();
		if (!RegionUtils.isRegionContainsBlock(body, loopEnd)
				&& !loopEnd.contains(AType.EXC_HANDLER)
				&& !inExceptionHandlerBlocks(loopEnd)) {
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

	private boolean inExceptionHandlerBlocks(BlockNode loopEnd) {
		if (mth.getExceptionHandlersCount() == 0) {
			return false;
		}
		for (ExceptionHandler eh : mth.getExceptionHandlers()) {
			if (eh.getBlocks().contains(loopEnd)) {
				return true;
			}
		}
		return false;
	}

	private boolean canInsertBreak(BlockNode exit) {
		if (exit.contains(AFlag.RETURN)
				|| BlockUtils.checkLastInsnType(exit, InsnType.BREAK)) {
			return false;
		}
		List<BlockNode> simplePath = BlockUtils.buildSimplePath(exit);
		if (!simplePath.isEmpty()) {
			BlockNode lastBlock = simplePath.get(simplePath.size() - 1);
			if (lastBlock.contains(AFlag.RETURN)
					|| lastBlock.getSuccessors().isEmpty()) {
				return false;
			}
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

	private boolean insertBreak(RegionStack stack, BlockNode loopExit, Edge exitEdge) {
		BlockNode exit = exitEdge.getTarget();
		BlockNode insertBlock = null;
		boolean confirm = false;
		// process special cases
		if (loopExit == exit) {
			// try/catch at loop end
			BlockNode source = exitEdge.getSource();
			if (source.contains(AType.CATCH_BLOCK)
					&& source.getSuccessors().size() == 2) {
				BlockNode other = BlockUtils.selectOther(loopExit, source.getSuccessors());
				if (other != null) {
					other = BlockUtils.skipSyntheticSuccessor(other);
					if (other.contains(AType.EXC_HANDLER)) {
						insertBlock = source;
						confirm = true;
					}
				}
			}
		}
		if (!confirm) {
			while (exit != null) {
				if (insertBlock != null && isPathExists(loopExit, exit)) {
					// found cross
					if (canInsertBreak(insertBlock)) {
						confirm = true;
						break;
					}
					return false;
				}
				insertBlock = exit;
				List<BlockNode> cs = exit.getCleanSuccessors();
				exit = cs.size() == 1 ? cs.get(0) : null;
			}
		}
		if (!confirm) {
			return false;
		}
		InsnNode breakInsn = new InsnNode(InsnType.BREAK, 0);
		EdgeInsnAttr.addEdgeInsn(insertBlock, insertBlock.getSuccessors().get(0), breakInsn);
		stack.addExit(exit);
		// add label to 'break' if needed
		addBreakLabel(exitEdge, exit, breakInsn);
		return true;
	}

	private void addBreakLabel(Edge exitEdge, BlockNode exit, InsnNode breakInsn) {
		BlockNode outBlock = BlockUtils.getNextBlock(exitEdge.getTarget());
		if (outBlock == null) {
			return;
		}
		List<LoopInfo> exitLoop = mth.getAllLoopsForBlock(outBlock);
		if (!exitLoop.isEmpty()) {
			return;
		}
		List<LoopInfo> inLoops = mth.getAllLoopsForBlock(exitEdge.getSource());
		if (inLoops.size() < 2) {
			return;
		}
		// search for parent loop
		LoopInfo parentLoop = null;
		for (LoopInfo loop : inLoops) {
			if (loop.getParentLoop() == null) {
				parentLoop = loop;
				break;
			}
		}
		if (parentLoop == null) {
			return;
		}
		if (parentLoop.getEnd() != exit && !parentLoop.getExitNodes().contains(exit)) {
			LoopLabelAttr labelAttr = new LoopLabelAttr(parentLoop);
			breakInsn.addAttr(labelAttr);
			parentLoop.getStart().addAttr(labelAttr);
		}
	}

	private static void insertContinue(LoopInfo loop) {
		BlockNode loopEnd = loop.getEnd();
		List<BlockNode> predecessors = loopEnd.getPredecessors();
		if (predecessors.size() <= 1) {
			return;
		}
		Set<BlockNode> loopExitNodes = loop.getExitNodes();
		for (BlockNode pred : predecessors) {
			if (canInsertContinue(pred, predecessors, loopEnd, loopExitNodes)) {
				InsnNode cont = new InsnNode(InsnType.CONTINUE, 0);
				pred.getInstructions().add(cont);
			}
		}
	}

	private static boolean canInsertContinue(BlockNode pred, List<BlockNode> predecessors, BlockNode loopEnd,
			Set<BlockNode> loopExitNodes) {
		if (!pred.contains(AFlag.SYNTHETIC)
				|| BlockUtils.checkLastInsnType(pred, InsnType.CONTINUE)) {
			return false;
		}
		List<BlockNode> preds = pred.getPredecessors();
		if (preds.isEmpty()) {
			return false;
		}
		BlockNode codePred = preds.get(0);
		if (codePred.contains(AFlag.SKIP)) {
			return false;
		}
		if (loopEnd.isDominator(codePred)
				|| loopExitNodes.contains(codePred)) {
			return false;
		}
		if (isDominatedOnBlocks(codePred, predecessors)) {
			return false;
		}
		boolean gotoExit = false;
		for (BlockNode exit : loopExitNodes) {
			if (BlockUtils.isPathExists(codePred, exit)) {
				gotoExit = true;
				break;
			}
		}
		return gotoExit;
	}

	private static boolean isDominatedOnBlocks(BlockNode dom, List<BlockNode> blocks) {
		for (BlockNode node : blocks) {
			if (!node.isDominator(dom)) {
				return false;
			}
		}
		return true;
	}

	private BlockNode processMonitorEnter(IRegion curRegion, BlockNode block, InsnNode insn, RegionStack stack) {
		SynchronizedRegion synchRegion = new SynchronizedRegion(curRegion, insn);
		synchRegion.getSubBlocks().add(block);
		curRegion.getSubBlocks().add(synchRegion);

		Set<BlockNode> exits = new HashSet<BlockNode>();
		Set<BlockNode> cacheSet = new HashSet<BlockNode>();
		traverseMonitorExits(synchRegion, insn.getArg(0), block, exits, cacheSet);

		for (InsnNode exitInsn : synchRegion.getExitInsns()) {
			BlockNode insnBlock = BlockUtils.getBlockByInsn(mth, exitInsn);
			if (insnBlock != null) {
				insnBlock.add(AFlag.SKIP);
			}
			exitInsn.add(AFlag.SKIP);
			InstructionRemover.unbindInsn(mth, exitInsn);
		}

		BlockNode body = getNextBlock(block);
		if (body == null) {
			ErrorsCounter.methodError(mth, "Unexpected end of synchronized block");
			return null;
		}
		BlockNode exit = null;
		if (exits.size() == 1) {
			exit = getNextBlock(exits.iterator().next());
		} else if (exits.size() > 1) {
			cacheSet.clear();
			exit = traverseMonitorExitsCross(body, exits, cacheSet);
		}

		stack.push(synchRegion);
		if (exit != null) {
			stack.addExit(exit);
		} else {
			for (BlockNode exitBlock : exits) {
				// don't add exit blocks which leads to method end blocks ('return', 'throw', etc)
				List<BlockNode> list = BlockUtils.buildSimplePath(exitBlock);
				if (list.isEmpty() || !list.get(list.size() - 1).getSuccessors().isEmpty()) {
					stack.addExit(exitBlock);
				}
			}
		}
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
		for (BlockNode node : block.getSuccessors()) {
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
		IfInfo modifiedIf = IfMakerHelper.restructureIf(mth, block, currentIf);
		if (modifiedIf != null) {
			currentIf = modifiedIf;
		} else {
			if (currentIf.getMergedBlocks().size() <= 1) {
				return null;
			}
			currentIf = makeIfInfo(block);
			currentIf = IfMakerHelper.restructureIf(mth, block, currentIf);
			if (currentIf == null) {
				// all attempts failed
				return null;
			}
		}
		confirmMerge(currentIf);

		IfRegion ifRegion = new IfRegion(currentRegion, block);
		ifRegion.setCondition(currentIf.getCondition());
		currentRegion.getSubBlocks().add(ifRegion);

		BlockNode outBlock = currentIf.getOutBlock();
		stack.push(ifRegion);
		stack.addExit(outBlock);

		ifRegion.setThenRegion(makeRegion(currentIf.getThenBlock(), stack));
		BlockNode elseBlock = currentIf.getElseBlock();
		if (elseBlock == null || stack.containsExit(elseBlock)) {
			ifRegion.setElseRegion(null);
		} else {
			ifRegion.setElseRegion(makeRegion(elseBlock, stack));
		}

		// insert edge insns in new 'else' branch
		// TODO: make more common algorithm
		if (ifRegion.getElseRegion() == null && outBlock != null) {
			List<EdgeInsnAttr> edgeInsnAttrs = outBlock.getAll(AType.EDGE_INSN);
			if (!edgeInsnAttrs.isEmpty()) {
				Region elseRegion = new Region(ifRegion);
				for (EdgeInsnAttr edgeInsnAttr : edgeInsnAttrs) {
					if (edgeInsnAttr.getEnd().equals(outBlock)) {
						addEdgeInsn(currentIf, elseRegion, edgeInsnAttr);
					}
				}
				ifRegion.setElseRegion(elseRegion);
			}
		}

		stack.pop();
		return outBlock;
	}

	private void addEdgeInsn(IfInfo ifInfo, Region region, EdgeInsnAttr edgeInsnAttr) {
		BlockNode start = edgeInsnAttr.getStart();
		if (start.contains(AFlag.SKIP)) {
			return;
		}
		boolean fromThisIf = false;
		for (BlockNode ifBlock : ifInfo.getMergedBlocks()) {
			if (ifBlock.getSuccessors().contains(start)) {
				fromThisIf = true;
				break;
			}
		}
		if (!fromThisIf) {
			return;
		}
		region.add(start);
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
		for (Map.Entry<Integer, List<Object>> entry : casesMap.entrySet()) {
			BlockNode c = getBlockByOffset(entry.getKey(), block.getSuccessors());
			if (c == null) {
				throw new JadxRuntimeException("Switch block not found by offset: " + entry.getKey());
			}
			blocksMap.put(c, entry.getValue());
		}
		BlockNode defCase = getBlockByOffset(insn.getDefaultCaseOffset(), block.getSuccessors());
		if (defCase != null) {
			blocksMap.remove(defCase);
		}
		LoopInfo loop = mth.getLoopForBlock(block);

		Map<BlockNode, BlockNode> fallThroughCases = new LinkedHashMap<BlockNode, BlockNode>();

		List<BlockNode> basicBlocks = mth.getBasicBlocks();
		BitSet outs = new BitSet(basicBlocks.size());
		outs.or(block.getDomFrontier());
		for (BlockNode s : block.getCleanSuccessors()) {
			BitSet df = s.getDomFrontier();
			// fall through case block
			if (df.cardinality() > 1) {
				if (df.cardinality() > 2) {
					LOG.debug("Unexpected case pattern, block: {}, mth: {}", s, mth);
				} else {
					BlockNode first = basicBlocks.get(df.nextSetBit(0));
					BlockNode second = basicBlocks.get(df.nextSetBit(first.getId() + 1));
					if (second.getDomFrontier().get(first.getId())) {
						fallThroughCases.put(s, second);
						df = new BitSet(df.size());
						df.set(first.getId());
					} else if (first.getDomFrontier().get(second.getId())) {
						fallThroughCases.put(s, first);
						df = new BitSet(df.size());
						df.set(second.getId());
					}
				}
			}
			outs.or(df);
		}
		outs.clear(block.getId());
		if (loop != null) {
			outs.clear(loop.getStart().getId());
		}

		stack.push(sw);
		stack.addExits(BlockUtils.bitSetToBlocks(mth, outs));

		// check cases order if fall through case exists
		if (!fallThroughCases.isEmpty()) {
			if (isBadCasesOrder(blocksMap, fallThroughCases)) {
				LOG.debug("Fixing incorrect switch cases order, method: {}", mth);
				blocksMap = reOrderSwitchCases(blocksMap, fallThroughCases);
				if (isBadCasesOrder(blocksMap, fallThroughCases)) {
					LOG.error("Can't fix incorrect switch cases order, method: {}", mth);
					mth.add(AFlag.INCONSISTENT_CODE);
				}
			}
		}

		// filter 'out' block
		if (outs.cardinality() > 1) {
			// remove exception handlers
			BlockUtils.cleanBitSet(mth, outs);
		}
		if (outs.cardinality() > 1) {
			// filter loop start and successors of other blocks
			for (int i = outs.nextSetBit(0); i >= 0; i = outs.nextSetBit(i + 1)) {
				BlockNode b = basicBlocks.get(i);
				outs.andNot(b.getDomFrontier());
				if (b.contains(AFlag.LOOP_START)) {
					outs.clear(b.getId());
				} else {
					for (BlockNode s : b.getCleanSuccessors()) {
						outs.clear(s.getId());
					}
				}
			}
		}

		if (loop != null && outs.cardinality() > 1) {
			outs.clear(loop.getEnd().getId());
		}
		if (outs.cardinality() == 0) {
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
					outs.set(maybeOut.getId());
					break;
				}
			}
		}
		BlockNode out = null;
		if (outs.cardinality() == 1) {
			out = basicBlocks.get(outs.nextSetBit(0));
			stack.addExit(out);
		} else if (loop == null && outs.cardinality() > 1) {
			LOG.warn("Can't detect out node for switch block: {} in {}", block, mth);
		}
		if (loop != null) {
			// check if 'continue' must be inserted
			BlockNode end = loop.getEnd();
			if (out != end && out != null) {
				insertContinueInSwitch(block, out, end);
			}
		}

		if (!stack.containsExit(defCase)) {
			sw.setDefaultCase(makeRegion(defCase, stack));
		}
		for (Entry<BlockNode, List<Object>> entry : blocksMap.entrySet()) {
			BlockNode caseBlock = entry.getKey();
			if (stack.containsExit(caseBlock)) {
				// empty case block
				sw.addCase(entry.getValue(), new Region(stack.peekRegion()));
			} else {
				BlockNode next = fallThroughCases.get(caseBlock);
				stack.addExit(next);
				Region caseRegion = makeRegion(caseBlock, stack);
				stack.removeExit(next);
				if (next != null) {
					next.add(AFlag.FALL_THROUGH);
					caseRegion.add(AFlag.FALL_THROUGH);
				}
				sw.addCase(entry.getValue(), caseRegion);
				// 'break' instruction will be inserted in RegionMakerVisitor.PostRegionVisitor
			}
		}

		stack.pop();
		return out;
	}

	private boolean isBadCasesOrder(final Map<BlockNode, List<Object>> blocksMap,
			final Map<BlockNode, BlockNode> fallThroughCases) {
		BlockNode nextCaseBlock = null;
		for (BlockNode caseBlock : blocksMap.keySet()) {
			if (nextCaseBlock != null && !caseBlock.equals(nextCaseBlock)) {
				return true;
			}
			nextCaseBlock = fallThroughCases.get(caseBlock);
		}
		return nextCaseBlock != null;
	}

	private Map<BlockNode, List<Object>> reOrderSwitchCases(Map<BlockNode, List<Object>> blocksMap,
			final Map<BlockNode, BlockNode> fallThroughCases) {
		List<BlockNode> list = new ArrayList<BlockNode>(blocksMap.size());
		list.addAll(blocksMap.keySet());
		Collections.sort(list, new Comparator<BlockNode>() {
			@Override
			public int compare(BlockNode a, BlockNode b) {
				BlockNode nextA = fallThroughCases.get(a);
				if (nextA != null) {
					if (b.equals(nextA)) {
						return -1;
					}
				} else if (a.equals(fallThroughCases.get(b))) {
					return 1;
				}
				return 0;
			}
		});

		Map<BlockNode, List<Object>> newBlocksMap = new LinkedHashMap<BlockNode, List<Object>>(blocksMap.size());
		for (BlockNode key : list) {
			newBlocksMap.put(key, blocksMap.get(key));
		}
		return newBlocksMap;
	}

	private static void insertContinueInSwitch(BlockNode block, BlockNode out, BlockNode end) {
		int endId = end.getId();
		for (BlockNode s : block.getCleanSuccessors()) {
			if (s.getDomFrontier().get(endId) && s != out) {
				// search predecessor of loop end on path from this successor
				List<BlockNode> list = BlockUtils.collectBlocksDominatedBy(s, s);
				for (BlockNode p : end.getPredecessors()) {
					if (list.contains(p)) {
						if (p.isSynthetic()) {
							p.getInstructions().add(new InsnNode(InsnType.CONTINUE, 0));
						}
						break;
					}
				}
			}
		}
	}

	public IRegion processTryCatchBlocks(MethodNode mth) {
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
		return processHandlersOutBlocks(mth, tcs);
	}

	/**
	 * Search handlers successor blocks not included in any region.
	 */
	protected IRegion processHandlersOutBlocks(MethodNode mth, Set<TryCatchBlock> tcs) {
		Set<IBlock> allRegionBlocks = new HashSet<IBlock>();
		RegionUtils.getAllRegionBlocks(mth.getRegion(), allRegionBlocks);

		Set<IBlock> succBlocks = new HashSet<IBlock>();
		for (TryCatchBlock tc : tcs) {
			for (ExceptionHandler handler : tc.getHandlers()) {
				IContainer region = handler.getHandlerRegion();
				if (region != null) {
					IBlock lastBlock = RegionUtils.getLastBlock(region);
					if (lastBlock instanceof BlockNode) {
						succBlocks.addAll(((BlockNode) lastBlock).getSuccessors());
					}
					RegionUtils.getAllRegionBlocks(region, allRegionBlocks);
				}
			}
		}
		succBlocks.removeAll(allRegionBlocks);
		if (succBlocks.isEmpty()) {
			return null;
		}
		Region excOutRegion = new Region(mth.getRegion());
		for (IBlock block : succBlocks) {
			if (block instanceof BlockNode) {
				excOutRegion.add(makeRegion((BlockNode) block, new RegionStack(mth)));
			}
		}
		return excOutRegion;
	}

	private void processExcHandler(ExceptionHandler handler, Set<BlockNode> exits) {
		BlockNode start = handler.getHandlerBlock();
		if (start == null) {
			return;
		}
		RegionStack stack = new RegionStack(mth);
		BlockNode dom;
		if (handler.isFinally()) {
			SplitterBlockAttr splitterAttr = start.get(AType.SPLITTER_BLOCK);
			if (splitterAttr == null) {
				return;
			}
			dom = splitterAttr.getBlock();
		} else {
			dom = start;
			stack.addExits(exits);
		}
		BitSet domFrontier = dom.getDomFrontier();
		List<BlockNode> handlerExits = BlockUtils.bitSetToBlocks(mth, domFrontier);
		boolean inLoop = mth.getLoopForBlock(start) != null;
		for (BlockNode exit : handlerExits) {
			if ((!inLoop || BlockUtils.isPathExists(start, exit))
					&& RegionUtils.isRegionContainsBlock(mth.getRegion(), exit)) {
				stack.addExit(exit);
			}
		}
		handler.setHandlerRegion(makeRegion(start, stack));

		ExcHandlerAttr excHandlerAttr = start.get(AType.EXC_HANDLER);
		if (excHandlerAttr == null) {
			LOG.warn("Missing exception handler attribute for start block");
		} else {
			handler.getHandlerRegion().addAttr(excHandlerAttr);
		}
	}

	static boolean isEqualPaths(BlockNode b1, BlockNode b2) {
		if (b1 == b2) {
			return true;
		}
		if (b1 == null || b2 == null) {
			return false;
		}
		return isEqualReturnBlocks(b1, b2) || isSyntheticPath(b1, b2);
	}

	private static boolean isSyntheticPath(BlockNode b1, BlockNode b2) {
		BlockNode n1 = skipSyntheticSuccessor(b1);
		BlockNode n2 = skipSyntheticSuccessor(b2);
		return (n1 != b1 || n2 != b2) && isEqualPaths(n1, n2);
	}

	public static boolean isEqualReturnBlocks(BlockNode b1, BlockNode b2) {
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
		if (i1.getArgsCount() != i2.getArgsCount()) {
			return false;
		}
		return i1.getArgsCount() == 0 || i1.getArg(0).equals(i2.getArg(0));
	}
}
