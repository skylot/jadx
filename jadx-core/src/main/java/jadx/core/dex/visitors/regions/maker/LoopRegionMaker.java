package jadx.core.dex.visitors.regions.maker;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
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
import jadx.core.utils.blocks.BlockSet;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static jadx.core.utils.BlockUtils.followEmptyPath;
import static jadx.core.utils.BlockUtils.getNextBlock;
import static jadx.core.utils.BlockUtils.isPathExists;

/*
 * Definitions:
 * main loop body - the set of nodes that form a loop in the control flow graph e.g. they can all
 * reach the loop start and are all reachable from the loop start
 * loop exit edge - an edge with a source in the main loop body and a target outside the main loop
 * body
 * outblock - the first node after the entire loop has finished that should be regioned next
 * header block - an IF node that implements the loop condition
 * crossing - a block that is reachable from two different source nodes and represents where control
 * flow paths from the two blocks cross
 * exit block/node - an overloaded term. May mean the source or target of an exit edge, or a route
 * to exit the overall region e.g. the outblock
 */

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

			// Blocks associated with the loop condition
			List<BlockNode> loopConditionBlocks = loopRegion.getConditionBlocks();

			for (Edge exitEdge : loop.getExitEdges()) {
				// An exit edge from the loop condition blocks
				BlockNode exitSource = exitEdge.getSource();

				if (loopConditionBlocks.contains(exitSource)) {
					BlockNode outBlock = followEmptyPath(exitEdge.getTarget());

					for (BlockNode pred : outBlock.getPredecessors()) {

						// Restarting search through exit edges from the beginning ("top")
						for (Edge exitEdgeTop : loop.getExitEdges()) {

							if (!loopConditionBlocks.contains(exitEdgeTop.getSource())) {
								if (isPathExists(exitEdgeTop.getTarget(), pred) || exitEdgeTop.getTarget() == outBlock) {
									insertLoopBreak(stack, loop, outBlock, exitEdgeTop.getSource(), new Edge(pred, outBlock));
								}
							}
						}
					}

					// Exit edge found - no need to check further regardless of break outcome
					break;
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
			out = condInfo.getElseBlock(); // Following Jadx convention, this must be the next synthetic block, not actual (theoretical) out
											// block
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
			// Ignore blocks that lead to exception handlers
			if (block.contains(AType.EXC_HANDLER)) {
				continue;
			}
			// Ignore blocks that do not branch based on an if statement
			InsnNode lastInsn = BlockUtils.getLastInsn(block);
			if (lastInsn == null || lastInsn.getType() != InsnType.IF) {
				continue;
			}
			// Skip any nested if statements
			List<LoopInfo> loops = block.getAll(AType.LOOP);
			if (!loops.isEmpty() && loops.get(0) != loop) {
				// skip nested loop condition
				continue;
			}
			boolean exitAtLoopEnd = isExitAtLoopEnd(block, loop);

			LoopRegion loopRegion = new LoopRegion(curRegion, loop, block, exitAtLoopEnd);

			boolean found;
			if (block == loop.getStart() || exitAtLoopEnd || BlockUtils.isEmptySimplePath(loop.getStart(), block)) {
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

	/*
	 * Check that the exits suggested by treating mainExitBlock as the header block
	 * are consistent with a loop condition
	 */
	private boolean checkLoopExits(LoopInfo loop, BlockNode mainExitBlock) {
		List<Edge> exitEdges = loop.getExitEdges();
		if (exitEdges.size() < 2) {
			return true;
		}
		// If the header selected does not have an exit edge, raise an exception
		Optional<Edge> mainEdgeOpt = exitEdges.stream().filter(edge -> edge.getSource() == mainExitBlock).findFirst();
		if (mainEdgeOpt.isEmpty()) {
			throw new JadxRuntimeException("Not found exit edge by exit block: " + mainExitBlock);
		}

		Edge mainExitEdge = mainEdgeOpt.get();
		BlockNode mainOutBlock = mainExitEdge.getTarget();

		BlockNode firstWorkAfterMainExitBlock = BlockUtils.followEmptyPath(mainOutBlock);
		List<InsnNode> firstInstructions = firstWorkAfterMainExitBlock.getInstructions();

		// If there is a direct path to a return from the header, all exits are inside the loop
		if (firstInstructions.size() == 1 && firstInstructions.get(0).getType() == InsnType.RETURN) {
			return true;
		}

		// Otherwise the exit must lead to a valid out block
		return validOutBlock(firstWorkAfterMainExitBlock, loop);
	}

	/*
	 * An out block is valid if every exit path passes through it or doesn't cross any other exit path
	 * (permitting one block of duplication)
	 * @param outblock The proposed region exit block
	 * @param exitEdges All edges leaving a section at the start of the region e.g. edges leaving a loop
	 * body
	 */
	private Boolean validOutBlock(BlockNode outBlock, LoopInfo loop) {
		/*
		 * Not permitted:
		 * - An edge which cannot reach outblock, but does cross with another exit path
		 * --- This crossing could be on a path that never reaches outblock
		 * --- This crossing could be after outblock
		 * - An edge which can reach outblock, but has another crossing with an exit path
		 * --- This crossing could be before outblock
		 * --- This crossing could be after outblock
		 * --- This crossing could be on a branch that does not reach outblock
		 * Permitted:
		 * - If any of these inconsistent crossings occur at or near the method exit
		 * - If the node can reach the outblock but has no crossing there because it dominates the outnode
		 * - A number of other edge cases
		 */
		List<Edge> exitEdges = loop.getExitEdges();
		Queue<Edge> edgesToCheck = new LinkedList<>(exitEdges);

		while (!edgesToCheck.isEmpty()) {
			Edge exitEdge = edgesToCheck.remove();
			BlockNode exitBlock = exitEdge.getTarget();

			// Get the dominance frontier of exitEdge.getTarget() only along paths through exitEdge

			List<BlockNode> dominanceFrontier;
			if (!exitEdge.isSynthetic()) {
				dominanceFrontier = BlockUtils.bitSetToBlocks(mth, BlockUtils.getDomFrontierThroughEdge(exitEdge));
			} else {
				dominanceFrontier = BlockUtils.bitSetToBlocks(mth, exitEdge.getTarget().getDomFrontier());
			}

			if (outBlock.isDominator(exitBlock) || outBlock == exitBlock) {
				// Accept if the loop exit block is a dominator of the suggested out block
				continue;
			}

			for (BlockNode crossing : dominanceFrontier) {
				if (crossing == outBlock) {
					// Accept if the crossing is at the outblock
					continue;
				}
				if (BlockUtils.isExitBlock(mth, crossing)) {
					// Accept if the crossing is at the method end
					continue;
				}

				// Find the first block after the crossing with instructions
				BlockNode firstInstructionBlock = crossing;
				List<InsnNode> cInsns = crossing.getInstructions();
				if (cInsns.isEmpty()) {
					firstInstructionBlock = BlockUtils.followEmptyPath(crossing);
				}

				// Return false if the crossing doesn't satisfy any relevant edge case
				if (!(viaValidUncleanSuccessor(exitBlock, crossing, loop)
						|| noWorkBeforeEnd(firstInstructionBlock, outBlock)
						|| oneBlockOfWorkBeforeEnd(firstInstructionBlock, outBlock)
						|| isNestedIfCross(crossing, edgesToCheck)
						|| isOuterOutblock(crossing, loop))) {
					return false;
				}
			}
		}
		return true;
	}

	/*
	 * @param exitBlock the target of an exit edge
	 * @param crossing the crossing block between exitBlock and a possible outblock
	 * @param loop the loop
	 */
	private boolean viaValidUncleanSuccessor(BlockNode exitBlock, BlockNode crossing, LoopInfo loop) {
		// Return true if the path from exitBlock is to an exception handler or via a backwards loop edge
		// with continue

		if (isPathExists(exitBlock, crossing)) {
			// This case does not apply if there is a path via clean successors
			return false;
		}

		// If to a loop start check if the backwards edge has a branch without a valid continue
		if (crossing.contains(AFlag.LOOP_START)) {
			// Note: This loop start cannot be for the internal loop else exitEdge would not leave the loop

			// Find the outer loop containing the loop start
			LoopInfo parent = loop.getParentLoop();
			LoopInfo outerLoop = null;
			while (parent != null) {
				if (parent.getStart() == crossing) {
					outerLoop = parent;
					break;
				}
				parent = parent.getParentLoop();
			}

			if (outerLoop != null) {
				BlockNode loopEnd = outerLoop.getEnd();
				List<BlockNode> predecessors = loopEnd.getPredecessors();
				if (predecessors.size() > 1) {
					for (BlockNode predecessor : predecessors) {
						// Do not accept if a predecessor to the loop end reachable from the exit would not have a
						// continue inserted
						if (BlockUtils.isPathExists(exitBlock, predecessor)
								&& !canInsertContinue(predecessor, predecessors, loopEnd, outerLoop.getExitNodes())) {
							return false;
						}
					}
				} else {
					// Do not accept if no continues would be placed
					return false;
				}
			}

		}

		// Accept if all branches have a valid continue or if not to a loop start (to an exception handler)
		return true;
	}

	/*
	 * @param firstInstructionBlock the first block containing instructions off the main loop body
	 * @param outBlock a possible outblock of the loop
	 */
	private boolean noWorkBeforeEnd(BlockNode firstInstructionBlock, BlockNode outBlock) {
		// Return true if there is no work between the crossing and an exit block
		return (BlockUtils.isExitBlock(mth, firstInstructionBlock) || firstInstructionBlock == outBlock);
	}

	/*
	 * @param firstInstructionBlock the first block containing instructions off the main loop body
	 * @param outBlock a possible outblock of the loop
	 */
	private boolean oneBlockOfWorkBeforeEnd(BlockNode firstInstructionBlock, BlockNode outBlock) {
		// Return true if down every path there is no more than one block of work between the crossing and
		// an exit block
		List<BlockNode> cleanSuccessors = firstInstructionBlock.getCleanSuccessors();
		if (cleanSuccessors.isEmpty()) {
			return false;
		}
		for (BlockNode cleanSuccessor : cleanSuccessors) {
			BlockNode nextInstructionBlock = BlockUtils.followEmptyPath(cleanSuccessor);
			if (!BlockUtils.isExitBlock(mth, nextInstructionBlock) && nextInstructionBlock != outBlock) {
				return false;
			}
		}
		return true;
	}

	/*
	 * @param crossing the block that may be the joint block of a merged if
	 * @param edgesToCheck the list of edges that will be processed to add to
	 */
	private boolean isNestedIfCross(BlockNode crossing, Queue<Edge> edgesToCheck) {
		// Return true if the crossing is due to merged control flow after a nested if
		// Add the edges out of the crossing to be investigated

		// If the crossing is the branch of a merged if, all predecessors will be synthetic up to the if
		// statements, and the first if statement will dominate the crossing
		List<BlockNode> predecessors = crossing.getPredecessors();

		// Find a predecessor that dominates all other predecessors
		BlockNode possibleFirstIF = BlockUtils.followEmptyPath(predecessors.get(0), true);
		for (BlockNode predecessor : predecessors) {
			// Follow the predecessor up to the first node with instructions
			BlockNode possibleIF = followEmptyPath(predecessor, true);
			if (crossing.isDominator(possibleIF)) {
				possibleFirstIF = possibleIF;
			}
		}

		// This case does not apply if a merged if cannot be made
		IfInfo currentIf = IfRegionMaker.makeIfInfo(mth, possibleFirstIF);
		if (currentIf == null) {
			return false;
		}

		IfInfo mergedIf = IfRegionMaker.mergeNestedIfNodes(currentIf);
		if (mergedIf == null) {
			return false;
		}

		// Note: work will be repeated for large merged ifs. Results could be cached to improve performance
		// Accept if following every predecessor path from the crossing reaches a merged if node
		BlockSet mergedBlocks = mergedIf.getMergedBlocks();
		for (BlockNode predecessor : predecessors) {
			BlockNode possibleIF = followEmptyPath(predecessor, true);
			if (!mergedBlocks.contains(possibleIF)) {
				return false;
			}
		}

		// If this crossing is the result of merged ifs, check the next crossing after this one
		Edge placeHolderEdge = new Edge(crossing, crossing, true);
		if (!edgesToCheck.contains(placeHolderEdge)) {
			edgesToCheck.add(placeHolderEdge);
		}

		return true;
	}

	/*
	 * @param crossing the block that may be an outblock for a parent loop
	 * @param loop the inner loop currently being considered
	 */
	private boolean isOuterOutblock(BlockNode crossing, LoopInfo loop) {
		// Return true if the crossing is the outblock for an outer loop and is jumped to using a labelled
		// break

		List<EdgeInsnAttr> edgeInsns = crossing.getAll(AType.EDGE_INSN);
		for (EdgeInsnAttr edgeInsn : edgeInsns) {
			InsnNode insn = edgeInsn.getInsn();
			// If there is a break edge instruction
			if (insn.getType() == InsnType.BREAK) {
				List<LoopInfo> loopsBrokenFrom = insn.get(AType.LOOP).getList();
				for (LoopInfo loopBrokenFrom : loopsBrokenFrom) {
					// If it is for a parent of the current loop
					if (loop.hasParent(loopBrokenFrom)) {
						BlockNode target = edgeInsn.getEnd();
						// If it points at the crossing
						if (target == crossing) {
							// Accept if the crossing block is already the target of a break instruction from a parent loop
							return true;
						}
					}
				}
			}
		}
		return false;
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
			if (insertLoopBreak(stack, loop, exit, exitEdge.getSource(), exitEdge)) {
				BlockNode nextBlock = getNextBlock(exit);
				if (nextBlock != null) {
					stack.addExit(nextBlock);
					out = nextBlock;
				}
			}
		} else {
			loop0: for (Edge exitEdge : exitEdges) {
				BlockNode exit = exitEdge.getTarget();
				List<BlockNode> blocks = BlockUtils.bitSetToBlocks(mth, BlockUtils.getDomFrontierThroughEdge(exitEdge));

				// Only select the method exit if there is no other valid outblock
				BlockNode methodExit = mth.getExitBlock();
				if (blocks.contains(methodExit)) {
					blocks.remove(methodExit);
					blocks.add(methodExit);
				}
				for (BlockNode block : blocks) {
					if (BlockUtils.isPathExists(exit, block)) {
						if (validOutBlock(block, loop)) {
							out = block;
							break loop0;
						}
					} else if (block.contains(AFlag.LOOP_START)) {
						// Special case if there is no joining control flow before an outer loop back edge
						if (validOutBlock(exit, loop)) {
							out = exit;
							break loop0;
						}
					}
				}
			}

			// Add breaks
			stack.addExit(out);
			if (out != null && out != mth.getExitBlock()) {
				// Add a break on every incoming edge where the predecessor is reachable from the loop
				for (BlockNode predecessor : out.getPredecessors()) {
					for (Edge exitEdge : loop.getExitEdges()) {
						BlockNode target = exitEdge.getTarget();
						if (BlockUtils.isPathExists(exitEdge.getTarget(), predecessor) || target == out) {
							insertLoopBreak(stack, loop, out, exitEdge.getSource(), new Edge(predecessor, out));
						}
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

	/*
	 * Insert a break instruction where exitEdge meets loopExit
	 * @param stack the region stack
	 * @param loop the loop being broken out of
	 * @param loopExit the outblock for loop
	 * @param blockOnLoop an exit block on loop through which exitEdge is reachable
	 * @param exitEdge an edge on the path between blockOnLoop and loopExit indicative of the breaking
	 * path
	 */
	private boolean insertLoopBreak(RegionStack stack, LoopInfo loop, BlockNode loopExit, BlockNode blockOnLoop, Edge exitEdge) {
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
			// Start search from the next edge if the target is simple (e.g. first
			// node after loop exit)
			Boolean isSimple = BlockUtils.followEmptyPath(exit) != exit;
			BlockNode insertBlock = isSimple ? null : exitEdge.getSource();
			BlockSet visited = new BlockSet(mth);
			while (true) {
				if (exit == null || visited.contains(exit)) {
					break;
				}
				visited.add(exit);
				if (insertBlock != null && isPathExists(loopExit, exit)) {
					// found cross
					if (canInsertBreak(insertBlock)) {
						insertEdge = new Edge(insertBlock, exit);
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
		addBreakLabel(blockOnLoop, exit, breakInsn);
		return true;
	}

	/*
	 * Adds a label to a break instruction if reaching the exit from the loop involves leaving multiple
	 * loops
	 * @param blockOnLoop the exit block on the loop to which breakInsn is currently associated
	 * @param exit the out block of the loop to which breakInsn is currently associated
	 * @param breakInsn a break instruction
	 */
	private void addBreakLabel(BlockNode blockOnLoop, BlockNode exit, InsnNode breakInsn) {
		List<LoopInfo> exitLoop = mth.getAllLoopsForBlock(exit);
		if (!exitLoop.isEmpty()) {
			return;
		}
		List<LoopInfo> inLoops = mth.getAllLoopsForBlock(blockOnLoop);
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
		if (!pred.getAll(AType.EDGE_INSN).isEmpty()) {
			// if we've already inserted a break, don't also insert a continue in the same spot
			List<EdgeInsnAttr> insns = pred.getAll(AType.EDGE_INSN);
			for (EdgeInsnAttr insn : insns) {
				if (insn.getInsn().getType() == InsnType.BREAK) {
					return false;
				}
			}
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
