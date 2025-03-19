package jadx.core.dex.visitors.regions.maker;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.EdgeInsnAttr;
import jadx.core.dex.attributes.nodes.LoopInfo;
import jadx.core.dex.attributes.nodes.LoopLabelAttr;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.Edge;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.Region;
import jadx.core.dex.regions.conditions.IfInfo;
import jadx.core.dex.regions.loops.LoopRegion;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.ListUtils;
import jadx.core.utils.RegionUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static jadx.core.utils.BlockUtils.getNextBlock;
import static jadx.core.utils.BlockUtils.isPathExists;

final class LoopRegionMaker {
	private final MethodNode mth;
	private final RegionMaker regionMaker;
	private final IfRegionMaker ifMaker;

	LoopRegionMaker(MethodNode mth, RegionMaker regionMaker, IfRegionMaker ifMaker) {
		this.mth = mth;
		this.regionMaker = regionMaker;
		this.ifMaker = ifMaker;
	}

	BlockNode process(IRegion curRegion, LoopInfo loop, RegionStack stack) {
		BlockNode loopStart = loop.getStart();
		Set<BlockNode> exitBlocksSet = loop.getExitNodes();

		// set exit blocks scan order priority
		// this can help if loop has several exits (after using 'break' or 'return' in loop)
		List<BlockNode> exitBlocks = new ArrayList<>(exitBlocksSet.size());
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

		IfInfo condInfo = ifMaker.buildIfInfo(loopRegion);
		if (!loop.getLoopBlocks().contains(condInfo.getThenBlock())) {
			// invert loop condition if 'then' points to exit
			condInfo = IfInfo.invert(condInfo);
		}
		loopRegion.updateCondition(condInfo);
		// prevent if's merge with loop condition
		condInfo.getMergedBlocks().forEach(b -> b.add(AFlag.ADDED_TO_REGION));
		exitBlocks.removeAll(condInfo.getMergedBlocks().toList());

		if (!exitBlocks.isEmpty()) {
			BlockNode loopExit = condInfo.getElseBlock();
			if (loopExit != null) {
				// add 'break' instruction before path cross between main loop exit and sub-exit
				for (Edge exitEdge : loop.getExitEdges()) {
					if (exitBlocks.contains(exitEdge.getSource())) {
						insertLoopBreak(stack, loop, loopExit, exitEdge);
					}
				}
			}
		}

		BlockNode out;
		if (loopRegion.isConditionAtEnd()) {
			BlockNode thenBlock = condInfo.getThenBlock();
			out = thenBlock == loop.getEnd() || thenBlock == loopStart ? condInfo.getElseBlock() : thenBlock;
			out = BlockUtils.followEmptyPath(out);
			loopStart.remove(AType.LOOP);
			loop.getEnd().add(AFlag.ADDED_TO_REGION);
			stack.addExit(loop.getEnd());
			regionMaker.clearBlockProcessedState(loopStart);
			Region body = regionMaker.makeRegion(loopStart);
			loopRegion.setBody(body);
			loopStart.addAttr(AType.LOOP, loop);
			loop.getEnd().remove(AFlag.ADDED_TO_REGION);
		} else {
			out = condInfo.getElseBlock();
			if (outerRegion != null
					&& out != null
					&& out.contains(AFlag.LOOP_START)
					&& !out.getAll(AType.LOOP).contains(loop)
					&& RegionUtils.isRegionContainsBlock(outerRegion, out)) {
				// exit to already processed outer loop
				out = null;
			}
			stack.addExit(out);
			BlockNode loopBody = condInfo.getThenBlock();
			Region body;
			if (Objects.equals(loopBody, loopStart)) {
				// empty loop body
				body = new Region(loopRegion);
			} else {
				body = regionMaker.makeRegion(loopBody);
			}
			// add blocks from loop start to first condition block
			BlockNode conditionBlock = condInfo.getFirstIfBlock();
			if (loopStart != conditionBlock) {
				Set<BlockNode> blocks = BlockUtils.getAllPathsBlocks(loopStart, conditionBlock);
				blocks.remove(conditionBlock);
				for (BlockNode block : blocks) {
					if (block.getInstructions().isEmpty()
							&& !block.contains(AFlag.ADDED_TO_REGION)
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
			if (block.contains(AType.EXC_HANDLER)) {
				continue;
			}
			InsnNode lastInsn = BlockUtils.getLastInsn(block);
			if (lastInsn == null || lastInsn.getType() != InsnType.IF) {
				continue;
			}
			List<LoopInfo> loops = block.getAll(AType.LOOP);
			if (!loops.isEmpty() && loops.get(0) != loop) {
				// skip nested loop condition
				continue;
			}
			boolean exitAtLoopEnd = isExitAtLoopEnd(block, loop);
			LoopRegion loopRegion = new LoopRegion(curRegion, loop, block, exitAtLoopEnd);
			boolean found;
			if (block == loop.getStart() || exitAtLoopEnd
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
			if (found && !checkLoopExits(loop, block)) {
				found = false;
			}
			if (found) {
				return loopRegion;
			}
		}
		// no exit found => endless loop
		return null;
	}

	private static boolean isExitAtLoopEnd(BlockNode exit, LoopInfo loop) {
		BlockNode loopEnd = loop.getEnd();
		if (exit == loopEnd) {
			return true;
		}
		BlockNode loopStart = loop.getStart();
		if (loopStart.getInstructions().isEmpty() && ListUtils.isSingleElement(loopStart.getSuccessors(), exit)) {
			return false;
		}
		return loopEnd.getInstructions().isEmpty() && ListUtils.isSingleElement(loopEnd.getPredecessors(), exit);
	}

	private boolean checkLoopExits(LoopInfo loop, BlockNode mainExitBlock) {
		List<Edge> exitEdges = loop.getExitEdges();
		if (exitEdges.size() < 2) {
			return true;
		}
		Optional<Edge> mainEdgeOpt = exitEdges.stream().filter(edge -> edge.getSource() == mainExitBlock).findFirst();
		if (mainEdgeOpt.isEmpty()) {
			throw new JadxRuntimeException("Not found exit edge by exit block: " + mainExitBlock);
		}
		Edge mainExitEdge = mainEdgeOpt.get();
		BlockNode mainOutBlock = mainExitEdge.getTarget();
		for (Edge exitEdge : exitEdges) {
			if (exitEdge != mainExitEdge) {
				// all exit paths must be same or don't cross (will be inside loop)
				BlockNode exitBlock = exitEdge.getTarget();
				if (!BlockUtils.isEqualPaths(mainOutBlock, exitBlock)) {
					BlockNode crossBlock = BlockUtils.getPathCross(mth, mainOutBlock, exitBlock);
					if (crossBlock != null) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private BlockNode makeEndlessLoop(IRegion curRegion, RegionStack stack, LoopInfo loop, BlockNode loopStart) {
		LoopRegion loopRegion = new LoopRegion(curRegion, loop, null, false);
		curRegion.getSubBlocks().add(loopRegion);

		loopStart.remove(AType.LOOP);
		regionMaker.clearBlockProcessedState(loopStart);
		stack.push(loopRegion);

		BlockNode out = null;
		// insert 'break' for exits
		List<Edge> exitEdges = loop.getExitEdges();
		if (exitEdges.size() == 1) {
			Edge exitEdge = exitEdges.get(0);
			BlockNode exit = exitEdge.getTarget();
			if (insertLoopBreak(stack, loop, exit, exitEdge)) {
				BlockNode nextBlock = getNextBlock(exit);
				if (nextBlock != null) {
					stack.addExit(nextBlock);
					out = nextBlock;
				}
			}
		} else {
			for (Edge exitEdge : exitEdges) {
				BlockNode exit = exitEdge.getTarget();
				List<BlockNode> blocks = BlockUtils.bitSetToBlocks(mth, exit.getDomFrontier());
				for (BlockNode block : blocks) {
					if (BlockUtils.isPathExists(exit, block)) {
						stack.addExit(block);
						insertLoopBreak(stack, loop, block, exitEdge);
						out = block;
					} else {
						insertLoopBreak(stack, loop, exit, exitEdge);
					}
				}
			}
		}

		Region body = regionMaker.makeRegion(loopStart);
		BlockNode loopEnd = loop.getEnd();
		if (!RegionUtils.isRegionContainsBlock(body, loopEnd)
				&& !loopEnd.contains(AType.EXC_HANDLER)
				&& !inExceptionHandlerBlocks(loopEnd)) {
			body.getSubBlocks().add(loopEnd);
		}
		loopRegion.setBody(body);

		if (out == null) {
			BlockNode next = getNextBlock(loopEnd);
			out = RegionUtils.isRegionContainsBlock(body, next) ? null : next;
		}
		stack.pop();
		loopStart.addAttr(AType.LOOP, loop);
		return out;
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
		if (BlockUtils.containsExitInsn(exit)) {
			return false;
		}
		List<BlockNode> simplePath = BlockUtils.buildSimplePath(exit);
		if (!simplePath.isEmpty()) {
			BlockNode lastBlock = simplePath.get(simplePath.size() - 1);
			if (lastBlock.isMthExitBlock()
					|| lastBlock.isReturnBlock()
					|| mth.isPreExitBlock(lastBlock)) {
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

	private boolean insertLoopBreak(RegionStack stack, LoopInfo loop, BlockNode loopExit, Edge exitEdge) {
		BlockNode exit = exitEdge.getTarget();
		Edge insertEdge = null;
		boolean confirm = false;
		// process special cases:
		// 1. jump to outer loop
		BlockNode exitEnd = BlockUtils.followEmptyPath(exit);
		List<LoopInfo> loops = exitEnd.getAll(AType.LOOP);
		for (LoopInfo loopAtEnd : loops) {
			if (loopAtEnd != loop && loop.hasParent(loopAtEnd)) {
				insertEdge = exitEdge;
				confirm = true;
				break;
			}
		}

		if (!confirm) {
			BlockNode insertBlock = null;
			while (exit != null) {
				if (insertBlock != null && isPathExists(loopExit, exit)) {
					// found cross
					if (canInsertBreak(insertBlock)) {
						insertEdge = new Edge(insertBlock, insertBlock.getSuccessors().get(0));
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
		breakInsn.addAttr(AType.LOOP, loop);
		EdgeInsnAttr.addEdgeInsn(insertEdge, breakInsn);
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
		if (codePred.contains(AFlag.ADDED_TO_REGION)) {
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
}
