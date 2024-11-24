package jadx.core.dex.visitors.regions.maker;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.nodes.LoopInfo;
import jadx.core.dex.attributes.nodes.RegionRefAttr;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.SwitchInsn;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.Region;
import jadx.core.dex.regions.SwitchRegion;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.RegionUtils;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;

final class SwitchRegionMaker {
	private final MethodNode mth;
	private final RegionMaker regionMaker;

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

		processFallThroughCases(sw, out, stack, blocksMap);
		removeEmptyCases(insn, sw, defCase);

		stack.pop();
		return out;
	}

	private void processFallThroughCases(SwitchRegion sw, @Nullable BlockNode out,
			RegionStack stack, Map<BlockNode, List<Object>> blocksMap) {
		Map<BlockNode, BlockNode> fallThroughCases = new LinkedHashMap<>();
		if (out != null) {
			// detect fallthrough cases
			BitSet caseBlocks = BlockUtils.blocksToBitSet(mth, blocksMap.keySet());
			caseBlocks.clear(out.getId());
			for (BlockNode successor : sw.getHeader().getCleanSuccessors()) {
				BitSet df = successor.getDomFrontier();
				if (df.intersects(caseBlocks)) {
					BlockNode fallThroughBlock = getOneIntersectionBlock(out, caseBlocks, df);
					fallThroughCases.put(successor, fallThroughBlock);
				}
			}
			// check fallthrough cases order
			if (!fallThroughCases.isEmpty() && isBadCasesOrder(blocksMap, fallThroughCases)) {
				Map<BlockNode, List<Object>> newBlocksMap = reOrderSwitchCases(blocksMap, fallThroughCases);
				if (isBadCasesOrder(newBlocksMap, fallThroughCases)) {
					mth.addWarnComment("Can't fix incorrect switch cases order, some code will duplicate");
					fallThroughCases.clear();
				} else {
					blocksMap = newBlocksMap;
				}
			}
		}

		for (Map.Entry<BlockNode, List<Object>> entry : blocksMap.entrySet()) {
			List<Object> keysList = entry.getValue();
			BlockNode caseBlock = entry.getKey();
			if (stack.containsExit(caseBlock)) {
				sw.addCase(keysList, new Region(stack.peekRegion()));
			} else {
				BlockNode next = fallThroughCases.get(caseBlock);
				stack.addExit(next);
				Region caseRegion = regionMaker.makeRegion(caseBlock);
				stack.removeExit(next);
				if (next != null) {
					next.add(AFlag.FALL_THROUGH);
					caseRegion.add(AFlag.FALL_THROUGH);
				}
				sw.addCase(keysList, caseRegion);
				// 'break' instruction will be inserted in RegionMakerVisitor.PostRegionVisitor
			}
		}
	}

	@Nullable
	private BlockNode getOneIntersectionBlock(BlockNode out, BitSet caseBlocks, BitSet fallThroughSet) {
		BitSet caseExits = BlockUtils.copyBlocksBitSet(mth, fallThroughSet);
		caseExits.clear(out.getId());
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
				if (outs.get(imPostDom.getId())) {
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
		if (imPostDom == insn.getDefTargetBlock()
				&& block.getCleanSuccessors().contains(imPostDom)
				&& block.getDomFrontier().get(imPostDom.getId())) {
			// add exit to stop on empty 'default' block
			stack.addExit(imPostDom);
		}
		if (out == null) {
			mth.addWarnComment("Failed to find 'out' block for switch in " + block + ". Please report as an issue.");
			// fallback option; should work in most cases
			out = block.getIPostDom();
		}
		if (out != null && regionMaker.isProcessed(out)) {
			// 'out' block already processed, prevent endless loop
			throw new JadxRuntimeException("Failed to find switch 'out' block (already processed)");
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
					InsnArg arg = returnArgs.get(1);
					if (!arg.isRegister() || !((RegisterArg) arg).sameCodeVar(reg)) {
						return exitBlock;
					}
				}
			} else {
				for (int i = 1; i < count; i++) {
					InsnArg arg = returnArgs.get(1);
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
	private void removeEmptyCases(SwitchInsn insn, SwitchRegion sw, BlockNode defCase) {
		boolean defaultCaseIsEmpty;
		if (defCase == null) {
			defaultCaseIsEmpty = true;
		} else {
			defaultCaseIsEmpty = sw.getCases().stream()
					.anyMatch(c -> c.getKeys().contains(SwitchRegion.DEFAULT_CASE_KEY)
							&& RegionUtils.isEmpty(c.getContainer()));
		}
		if (defaultCaseIsEmpty) {
			sw.getCases().removeIf(caseInfo -> {
				if (RegionUtils.isEmpty(caseInfo.getContainer())) {
					List<Object> keys = caseInfo.getKeys();
					if (keys.contains(SwitchRegion.DEFAULT_CASE_KEY)) {
						return true;
					}
					if (insn.isPacked()) {
						return true;
					}
				}
				return false;
			});
		}
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

	private Map<BlockNode, List<Object>> reOrderSwitchCases(Map<BlockNode, List<Object>> blocksMap,
			Map<BlockNode, BlockNode> fallThroughCases) {
		List<BlockNode> list = new ArrayList<>(blocksMap.size());
		list.addAll(blocksMap.keySet());
		list.sort((a, b) -> {
			BlockNode nextA = fallThroughCases.get(a);
			if (nextA != null) {
				if (b.equals(nextA)) {
					return -1;
				}
			} else if (a.equals(fallThroughCases.get(b))) {
				return 1;
			}
			return 0;
		});

		Map<BlockNode, List<Object>> newBlocksMap = new LinkedHashMap<>(blocksMap.size());
		for (BlockNode key : list) {
			newBlocksMap.put(key, blocksMap.get(key));
		}
		return newBlocksMap;
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
						if (list.contains(p)) {
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
}
