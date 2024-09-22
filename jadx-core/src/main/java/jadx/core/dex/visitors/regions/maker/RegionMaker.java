package jadx.core.dex.visitors.regions.maker;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.EdgeInsnAttr;
import jadx.core.dex.attributes.nodes.LoopInfo;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.SwitchInsn;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnContainer;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.Region;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.exceptions.JadxOverflowException;

import static jadx.core.utils.BlockUtils.getNextBlock;

public class RegionMaker {
	private final MethodNode mth;
	private final RegionStack stack;

	private final IfRegionMaker ifMaker;
	private final LoopRegionMaker loopMaker;

	private final BitSet processedBlocks;
	private final int regionsLimit;

	private int regionsCount;

	public RegionMaker(MethodNode mth) {
		this.mth = mth;
		this.stack = new RegionStack(mth);
		this.ifMaker = new IfRegionMaker(mth, this);
		this.loopMaker = new LoopRegionMaker(mth, this, ifMaker);
		int blocksCount = mth.getBasicBlocks().size();
		this.processedBlocks = new BitSet(blocksCount);
		this.regionsLimit = blocksCount * 100;
	}

	public Region makeMthRegion() {
		return makeRegion(mth.getEnterBlock());
	}

	Region makeRegion(BlockNode startBlock) {
		Objects.requireNonNull(startBlock);
		Region region = new Region(stack.peekRegion());
		if (stack.containsExit(startBlock)) {
			insertEdgeInsns(region, startBlock);
			return region;
		}

		int startBlockId = startBlock.getId();
		if (processedBlocks.get(startBlockId)) {
			mth.addWarn("Removed duplicated region for block: " + startBlock + ' ' + startBlock.getAttributesString());
			return region;
		}
		processedBlocks.set(startBlockId);

		BlockNode next = startBlock;
		while (next != null) {
			next = traverse(region, next);
			regionsCount++;
			if (regionsCount > regionsLimit) {
				throw new JadxOverflowException("Regions count limit reached");
			}
		}
		return region;
	}

	/**
	 * Recursively traverse all blocks from 'block' until block from 'exits'
	 */
	private BlockNode traverse(IRegion r, BlockNode block) {
		if (block.contains(AFlag.MTH_EXIT_BLOCK)) {
			return null;
		}
		BlockNode next = null;
		boolean processed = false;

		List<LoopInfo> loops = block.getAll(AType.LOOP);
		int loopCount = loops.size();
		if (loopCount != 0 && block.contains(AFlag.LOOP_START)) {
			if (loopCount == 1) {
				next = loopMaker.process(r, loops.get(0), stack);
				processed = true;
			} else {
				for (LoopInfo loop : loops) {
					if (loop.getStart() == block) {
						next = loopMaker.process(r, loop, stack);
						processed = true;
						break;
					}
				}
			}
		}

		InsnNode insn = BlockUtils.getLastInsn(block);
		if (!processed && insn != null) {
			switch (insn.getType()) {
				case IF:
					next = ifMaker.process(r, block, (IfNode) insn, stack);
					processed = true;
					break;

				case SWITCH:
					SwitchRegionMaker switchMaker = new SwitchRegionMaker(mth, this);
					next = switchMaker.process(r, block, (SwitchInsn) insn, stack);
					processed = true;
					break;

				case MONITOR_ENTER:
					SynchronizedRegionMaker syncMaker = new SynchronizedRegionMaker(mth, this);
					next = syncMaker.process(r, block, insn, stack);
					processed = true;
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

	private void insertEdgeInsns(Region region, BlockNode exitBlock) {
		List<EdgeInsnAttr> edgeInsns = exitBlock.getAll(AType.EDGE_INSN);
		if (edgeInsns.isEmpty()) {
			return;
		}
		List<InsnNode> insns = new ArrayList<>(edgeInsns.size());
		addOneInsnOfType(insns, edgeInsns, InsnType.BREAK);
		addOneInsnOfType(insns, edgeInsns, InsnType.CONTINUE);
		region.add(new InsnContainer(insns));
	}

	private void addOneInsnOfType(List<InsnNode> insns, List<EdgeInsnAttr> edgeInsns, InsnType insnType) {
		for (EdgeInsnAttr edgeInsn : edgeInsns) {
			InsnNode insn = edgeInsn.getInsn();
			if (insn.getType() == insnType) {
				insns.add(insn);
				return;
			}
		}
	}

	RegionStack getStack() {
		return stack;
	}

	boolean isProcessed(BlockNode block) {
		return processedBlocks.get(block.getId());
	}

	void clearBlockProcessedState(BlockNode block) {
		processedBlocks.clear(block.getId());
	}
}
