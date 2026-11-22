package jadx.core.dex.visitors.regions.maker;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.nodes.LoopInfo;
import jadx.core.dex.attributes.nodes.RegionRefAttr;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.SwitchInsn;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnContainer;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.Region;
import jadx.core.dex.regions.SwitchRegion;
import jadx.core.dex.regions.SwitchRegion.CaseInfo;
import jadx.core.dex.visitors.regions.AbstractRegionVisitor;
import jadx.core.dex.visitors.regions.DepthRegionTraversal;
import jadx.core.dex.visitors.regions.SwitchBreakVisitor;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.ListUtils;
import jadx.core.utils.RegionUtils;
import jadx.core.utils.Utils;
import jadx.core.utils.blocks.BlockSet;

public final class SwitchRegionMaker {
	private final MethodNode mth;
	private final RegionMaker regionMaker;

	private static final Logger LOG = LoggerFactory.getLogger(SwitchRegionMaker.class);

	SwitchRegionMaker(MethodNode mth, RegionMaker regionMaker) {
		this.mth = mth;
		this.regionMaker = regionMaker;
	}

	BlockNode process(IRegion currentRegion, BlockNode block, SwitchInsn insn, RegionStack stack) {
		// map case blocks to keys
		int len = insn.getTargets().length;
		Map<BlockNode, List<Object>> blocksMap = new LinkedHashMap<>(len);
		BlockNode[] targetBlocksArr = insn.getTargetBlocks();
		for (int i = 0; i < len; i++) {
			List<Object> keys = blocksMap.computeIfAbsent(targetBlocksArr[i], k -> new ArrayList<>(2));
			keys.add(insn.getKey(i));
		}
		BlockNode defCase = insn.getDefTargetBlock();
		if (defCase != null) {
			List<Object> keys = blocksMap.computeIfAbsent(defCase, k -> new ArrayList<>(1));
			keys.add(SwitchRegion.DEFAULT_CASE_KEY);
		}

		SwitchRegion sw = new SwitchRegion(currentRegion, block);
		insn.addAttr(new RegionRefAttr(sw));
		currentRegion.getSubBlocks().add(sw);
		stack.push(sw);

		BlockNode out = calcSwitchOut(block, insn, stack);
		stack.addExit(out);

		addCases(sw, out, stack, blocksMap);
		removeEmptyCases(insn, sw, defCase, out);

		stack.pop();
		return out;
	}

	/**
	 * Insert 'break' for all cases in switch region
	 * Executed in {@link jadx.core.dex.visitors.regions.PostProcessRegions} after try/catch wrap to
	 * handle all blocks
	 */
	public static void insertBreaks(MethodNode mth, SwitchRegion sw) {
		for (SwitchRegion.CaseInfo caseInfo : sw.getCases()) {
			insertBreaksForCase(mth, sw, caseInfo.getContainer());
		}
	}

	private void addCases(SwitchRegion sw, @Nullable BlockNode out,
			RegionStack stack, Map<BlockNode, List<Object>> blocksMap) {
		Map<BlockNode, BlockNode> fallThroughCases = new LinkedHashMap<>();
		if (out != null) {
			// detect fallthrough cases
			BitSet caseBlocks = BlockUtils.blocksToBitSet(mth, blocksMap.keySet());
			caseBlocks.clear(out.getPos());
			for (BlockNode successor : sw.getHeader().getSuccessors()) {
				BitSet df = successor.getDomFrontier();
				if (df.intersects(caseBlocks)) {
					BlockNode fallThroughBlock = getOneIntersectionBlock(out, caseBlocks, df);
					if (fallThroughBlock != null && !fallThroughBlock.equals(successor)) {
						fallThroughCases.put(successor, fallThroughBlock);
					}
				}
			}
			// check fallthrough cases order
			if (!fallThroughCases.isEmpty() && isBadCasesOrder(blocksMap, fallThroughCases)) {
				blocksMap = reOrderSwitchCases(blocksMap, fallThroughCases);
				if (isBadCasesOrder(blocksMap, fallThroughCases)) {
					mth.addWarnComment("Can't fix incorrect switch cases order, some code will duplicate");
					filterBadFallThroughCases(blocksMap, fallThroughCases);
				}
			}
		}
		for (Map.Entry<BlockNode, List<Object>> entry : blocksMap.entrySet()) {
			List<Object> keysList = entry.getValue();
			BlockNode caseBlock = entry.getKey();
			Region caseRegion;
			if (stack.containsExit(caseBlock)) {
				caseRegion = new Region(stack.peekRegion());
			} else {
				BlockNode next = fallThroughCases.get(caseBlock);
				stack.addExit(next);
				caseRegion = regionMaker.makeRegion(caseBlock);
				stack.removeExit(next);
				if (next != null) {
					next.add(AFlag.FALL_THROUGH);
					caseRegion.add(AFlag.FALL_THROUGH);
				}
			}
			sw.addCase(keysList, caseRegion);
		}
	}

	@Nullable
	private BlockNode getOneIntersectionBlock(BlockNode out, BitSet caseBlocks, BitSet fallThroughSet) {
		BitSet caseExits = BlockUtils.copyBlocksBitSet(mth, fallThroughSet);
		caseExits.clear(out.getPos());
		caseExits.and(caseBlocks);
		return BlockUtils.bitSetToOneBlock(mth, caseExits);
	}

	private @Nullable BlockNode calcSwitchOut(BlockNode block, SwitchInsn insn, RegionStack stack) {
		// union of case blocks dominance frontier
		// works if no fallthrough cases and no returns inside switch
		BitSet outs = BlockUtils.newBlocksBitSet(mth);
		for (BlockNode s : block.getCleanSuccessors()) {
			if (s.contains(AFlag.LOOP_END)) {
				// loop end dom frontier is loop start, ignore it
				continue;
			}
			outs.or(s.getDomFrontier());
		}
		outs.clear(block.getId());
		outs.clear(mth.getExitBlock().getId());

		BlockNode out = null;
		if (outs.cardinality() == 1) {
			// single exit
			out = BlockUtils.bitSetToOneBlock(mth, outs);
		} else {
			// several switch exits
			// possible 'return', 'continue' or fallthrough in one of the cases
			LoopInfo loop = mth.getLoopForBlock(block);
			if (loop != null) {
				outs.andNot(loop.getStart().getPostDoms());
				outs.andNot(loop.getEnd().getPostDoms());
				BlockNode loopEnd = loop.getEnd();
				if (outs.cardinality() == 2 && outs.get(loopEnd.getId())) {
					// insert 'continue' for cases lead to loop end
					// expect only 2 exits: loop end and switch out
					List<BlockNode> outList = BlockUtils.bitSetToBlocks(mth, outs);
					outList.remove(loopEnd);
					BlockNode possibleOut = Utils.getOne(outList);
					if (possibleOut != null && insertContinueInSwitch(block, possibleOut, loopEnd)) {
						outs.clear(loopEnd.getId());
						out = possibleOut;
					}
				}
				if (outs.isEmpty()) {
					// all exits inside switch, keep inside to exit from loop
					return mth.getExitBlock();
				}
			}
			if (out == null) {
				BlockNode imPostDom = block.getIPostDom();
				if (imPostDom != null && outs.get(imPostDom.getId())) {
					out = imPostDom;
				} else {
					outs.andNot(block.getPostDoms());
					out = BlockUtils.bitSetToOneBlock(mth, outs);
				}
			}
		}
		if (out != null && mth.isPreExitBlock(out)) {
			// include 'return' or 'throw' in case blocks
			out = mth.getExitBlock();
		}
		BlockNode imPostDom = block.getIPostDom();
		if (out == null && imPostDom == mth.getExitBlock()) {
			// all exits inside switch
			// check if all returns are equals and should be treated as single out block
			return allSameReturns(stack);
		}
		if (imPostDom != null
				&& imPostDom == insn.getDefTargetBlock()
				&& block.getCleanSuccessors().contains(imPostDom)
				&& block.getDomFrontier().get(imPostDom.getId())) {
			// add exit to stop on empty 'default' block
			stack.addExit(imPostDom);
		}
		if (out == null) {
			mth.addWarnComment("Failed to find 'out' block for switch in " + block + ". Please report as an issue.");
			// fallback option; should work in most cases
			out = imPostDom;
		}
		if (out != null && regionMaker.isProcessed(out) && BlockUtils.isPathExists(out, block)) {
			// 'out' block already processed, prevent endless loop
			// in this case it might be that 'out' is the LOOP_START of a loop and occurs before 'block'
			// just try the immediate post dominator as a fallback
			mth.addWarnComment("Switch 'out' block " + out + " for " + block + " already processed. Defaulting to fallback option.");
			stack.addExit(out);
			if (imPostDom != null && (!regionMaker.isProcessed(imPostDom) || !BlockUtils.isPathExists(imPostDom, block))) {
				out = imPostDom;
			} else {
				out = mth.getExitBlock();
			}
		}
		return out;
	}

	private BlockNode allSameReturns(RegionStack stack) {
		BlockNode exitBlock = mth.getExitBlock();
		List<BlockNode> preds = exitBlock.getPredecessors();
		int count = preds.size();
		if (count == 1) {
			return preds.get(0);
		}
		if (mth.getReturnType() == ArgType.VOID) {
			for (BlockNode pred : preds) {
				InsnNode insn = BlockUtils.getLastInsn(pred);
				if (insn == null || insn.getType() != InsnType.RETURN) {
					return exitBlock;
				}
			}
		} else {
			List<InsnArg> returnArgs = new ArrayList<>();
			for (BlockNode pred : preds) {
				InsnNode insn = BlockUtils.getLastInsn(pred);
				if (insn == null || insn.getType() != InsnType.RETURN) {
					return exitBlock;
				}
				returnArgs.add(insn.getArg(0));
			}
			InsnArg firstArg = returnArgs.get(0);
			if (firstArg.isRegister()) {
				RegisterArg reg = (RegisterArg) firstArg;
				for (int i = 1; i < count; i++) {
					InsnArg arg = returnArgs.get(i);
					if (!arg.isRegister() || !((RegisterArg) arg).sameCodeVar(reg)) {
						return exitBlock;
					}
				}
			} else {
				for (int i = 1; i < count; i++) {
					InsnArg arg = returnArgs.get(i);
					if (!arg.equals(firstArg)) {
						return exitBlock;
					}
				}
			}
		}
		// confirmed
		stack.addExits(preds);
		// ignore other returns
		for (int i = 1; i < count; i++) {
			BlockNode block = preds.get(i);
			block.add(AFlag.REMOVE);
			block.add(AFlag.ADDED_TO_REGION);
		}
		return preds.get(0);
	}

	/**
	 * Remove empty case blocks:
	 * 1. single 'default' case
	 * 2. filler cases if switch is 'packed' and 'default' case is empty
	 */
	private void removeEmptyCases(SwitchInsn insn, SwitchRegion sw, BlockNode defCase, BlockNode outBlock) {
		boolean defaultCaseIsEmpty;
		if (defCase == null) {
			defaultCaseIsEmpty = true;
		} else {
			defaultCaseIsEmpty = sw.getCases().stream()
					.anyMatch(c -> c.getKeys().contains(SwitchRegion.DEFAULT_CASE_KEY)
							&& canRemove(c.getContainer(), outBlock));
		}
		if (defaultCaseIsEmpty) {
			List<CaseInfo> cases = new ArrayList<>(sw.getCases());
			for (CaseInfo caseInfo : cases) {
				if (canRemove(caseInfo.getContainer(), outBlock)) {
					List<Object> keys = caseInfo.getKeys();
					if (keys.contains(SwitchRegion.DEFAULT_CASE_KEY) || insn.isPacked()) {
						// Remove case and mark all blocks as don't generate
						RegionUtils.addToAll(mth, caseInfo.getContainer(), AFlag.DONT_GENERATE);
						sw.getCases().remove(caseInfo);
					}
				}
			}
		}
	}

	/*
	 * Check container is empty and all paths through container are empty up until outBlock
	 */
	private boolean canRemove(IContainer container, BlockNode outBlock) {
		if (RegionUtils.isEmpty(container)) {
			if (container instanceof BlockNode) {
				// Base case - empty path from block node to outBlock
				return BlockUtils.followEmptyPath((BlockNode) container) == outBlock;
			} else if (container instanceof IRegion) {
				// Recursive case - every subBlock can be removed
				List<IContainer> subBlocks = ((IRegion) container).getSubBlocks();
				for (IContainer subBlock : subBlocks) {
					if (!canRemove(subBlock, outBlock)) {
						return false;
					}
				}
				return true;
			}
			LOG.debug("Unexpected container type in switch");
		}
		return false;
	}

	private boolean isBadCasesOrder(Map<BlockNode, List<Object>> blocksMap, Map<BlockNode, BlockNode> fallThroughCases) {
		BlockNode nextCaseBlock = null;
		for (BlockNode caseBlock : blocksMap.keySet()) {
			if (nextCaseBlock != null && !caseBlock.equals(nextCaseBlock)) {
				return true;
			}
			nextCaseBlock = fallThroughCases.get(caseBlock);
		}
		return nextCaseBlock != null;
	}

	private void filterBadFallThroughCases(Map<BlockNode, List<Object>> blocksMap,
			Map<BlockNode, BlockNode> fallThroughCases) {
		BlockNode prevCaseBlock = null;
		for (BlockNode caseBlock : blocksMap.keySet()) {
			if (prevCaseBlock != null) {
				BlockNode expectedNext = fallThroughCases.get(prevCaseBlock);
				if (expectedNext != null && !expectedNext.equals(caseBlock)) {
					fallThroughCases.remove(prevCaseBlock);
				}
			}
			prevCaseBlock = caseBlock;
		}
		if (prevCaseBlock != null) {
			fallThroughCases.remove(prevCaseBlock);
		}
	}

	private Map<BlockNode, List<Object>> reOrderSwitchCases(Map<BlockNode, List<Object>> blocksMap,
			Map<BlockNode, BlockNode> fallThroughCases) {
		Map<BlockNode, BlockNode> nextMap = new LinkedHashMap<>();
		Map<BlockNode, BlockNode> prevMap = new LinkedHashMap<>();
		Map<BlockNode, List<BlockNode>> extraPreds = new LinkedHashMap<>();
		for (Map.Entry<BlockNode, BlockNode> entry : fallThroughCases.entrySet()) {
			BlockNode from = entry.getKey();
			BlockNode to = entry.getValue();
			if (to == null || from.equals(to) || !blocksMap.containsKey(from) || !blocksMap.containsKey(to)) {
				continue;
			}
			if (isChainReachable(nextMap, to, from)) {
				continue;
			}
			if (!prevMap.containsKey(to)) {
				nextMap.put(from, to);
				prevMap.put(to, from);
			} else {
				extraPreds.computeIfAbsent(to, k -> new ArrayList<>(2)).add(from);
			}
		}

		Map<BlockNode, List<Object>> newBlocksMap = new LinkedHashMap<>(blocksMap.size());
		Set<BlockNode> inProgress = new HashSet<>();
		for (BlockNode key : blocksMap.keySet()) {
			emitFallThroughChain(key, blocksMap, nextMap, prevMap, extraPreds, inProgress, newBlocksMap);
		}
		return newBlocksMap;
	}

	private boolean isChainReachable(Map<BlockNode, BlockNode> nextMap, BlockNode start, BlockNode target) {
		BlockNode cur = start;
		while (cur != null) {
			if (cur.equals(target)) {
				return true;
			}
			cur = nextMap.get(cur);
		}
		return false;
	}

	private void emitFallThroughChain(BlockNode node,
			Map<BlockNode, List<Object>> blocksMap,
			Map<BlockNode, BlockNode> nextMap,
			Map<BlockNode, BlockNode> prevMap,
			Map<BlockNode, List<BlockNode>> extraPreds,
			Set<BlockNode> inProgress,
			Map<BlockNode, List<Object>> newBlocksMap) {
		if (newBlocksMap.containsKey(node)) {
			return;
		}
		BlockNode head = node;
		while (prevMap.containsKey(head)) {
			head = prevMap.get(head);
		}
		if (!inProgress.add(head)) {
			return;
		}
		List<BlockNode> chain = new ArrayList<>();
		BlockNode cur = head;
		while (cur != null && !newBlocksMap.containsKey(cur)) {
			chain.add(cur);
			cur = nextMap.get(cur);
		}
		for (BlockNode c : chain) {
			List<BlockNode> preds = extraPreds.get(c);
			if (preds != null) {
				for (BlockNode pred : preds) {
					emitFallThroughChain(pred, blocksMap, nextMap, prevMap, extraPreds, inProgress, newBlocksMap);
				}
			}
		}
		for (BlockNode c : chain) {
			if (!newBlocksMap.containsKey(c)) {
				newBlocksMap.put(c, blocksMap.get(c));
			}
		}
	}

	private boolean insertContinueInSwitch(BlockNode switchBlock, BlockNode switchOut, BlockNode loopEnd) {
		boolean inserted = false;
		for (BlockNode caseBlock : switchBlock.getCleanSuccessors()) {
			if (caseBlock.getDomFrontier().get(loopEnd.getId()) && caseBlock != switchOut) {
				// search predecessor of loop end on path from this successor
				Set<BlockNode> list = new HashSet<>(BlockUtils.collectBlocksDominatedBy(mth, caseBlock, caseBlock));
				if (list.contains(switchOut) || switchOut.getPredecessors().stream().anyMatch(list::contains)) {
					// 'continue' not needed
				} else {
					for (BlockNode p : loopEnd.getPredecessors()) {
						if (list.contains(p) || p == caseBlock) {
							if (p.isSynthetic()) {
								p.getInstructions().add(new InsnNode(InsnType.CONTINUE, 0));
								inserted = true;
							}
							break;
						}
					}
				}
			}
		}
		return inserted;
	}

	/**
	 * Add break to every exit edge from 'case' region.
	 * 'Break' optimizations (code duplication, unreachable, etc.) will be done at
	 * {@link SwitchBreakVisitor}
	 */
	private static void insertBreaksForCase(MethodNode mth, SwitchRegion switchRegion, IContainer caseContainer) {
		BlockSet caseBlocks = new BlockSet(mth);
		RegionUtils.visitBlockNodes(mth, caseContainer, caseBlocks::add);
		DepthRegionTraversal.traverse(mth, caseContainer, new AbstractRegionVisitor() {
			@Override
			public void leaveRegion(MethodNode mth, IRegion region) {
				boolean insertBreak = false;
				if (region == caseContainer) {
					// top region
					insertBreak = true;
				} else {
					IContainer lastContainer = ListUtils.last(region.getSubBlocks());
					if (lastContainer instanceof BlockNode) {
						BlockNode lastBlock = (BlockNode) lastContainer;
						for (BlockNode successor : lastBlock.getSuccessors()) {
							if (!caseBlocks.contains(successor)
									&& !(caseContainer.contains(AFlag.FALL_THROUGH) && successor.contains(AFlag.FALL_THROUGH))) {
								insertBreak = true;
								break;
							}
						}
					}
				}
				if (insertBreak && canAppendBreak(region)) {
					region.getSubBlocks().add(buildBreakContainer(switchRegion));
				}
			}
		});
	}

	public static boolean canAppendBreak(IRegion region) {
		return !region.contains(AFlag.FALL_THROUGH) && !RegionUtils.hasExitBlock(region);
	}

	public static InsnContainer buildBreakContainer(SwitchRegion switchRegion) {
		InsnNode breakInsn = new InsnNode(InsnType.BREAK, 0);
		breakInsn.add(AFlag.SYNTHETIC);
		breakInsn.addAttr(new RegionRefAttr(switchRegion));
		return new InsnContainer(breakInsn);
	}
}
