package jadx.core.dex.visitors.regions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.EdgeInsnAttr;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnContainer;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.Region;
import jadx.core.dex.regions.SwitchRegion;
import jadx.core.dex.regions.loops.LoopRegion;
import jadx.core.utils.RegionUtils;

final class PostProcessRegions extends AbstractRegionVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(PostProcessRegions.class);

	private static final IRegionVisitor INSTANCE = new PostProcessRegions();

	static void process(MethodNode mth) {
		DepthRegionTraversal.traverse(mth, INSTANCE);
	}

	@Override
	public void leaveRegion(MethodNode mth, IRegion region) {
		if (region instanceof LoopRegion) {
			// merge conditions in loops
			LoopRegion loop = (LoopRegion) region;
			loop.mergePreCondition();
		} else if (region instanceof SwitchRegion) {
			// insert 'break' in switch cases (run after try/catch insertion)
			processSwitch(mth, (SwitchRegion) region);
		} else if (region instanceof Region) {
			insertEdgeInsn((Region) region);
		}
	}

	/**
	 * Insert insn block from edge insn attribute.
	 */
	private static void insertEdgeInsn(Region region) {
		List<IContainer> subBlocks = region.getSubBlocks();
		if (subBlocks.isEmpty()) {
			return;
		}
		IContainer last = subBlocks.get(subBlocks.size() - 1);
		List<EdgeInsnAttr> edgeInsnAttrs = last.getAll(AType.EDGE_INSN);
		if (edgeInsnAttrs.isEmpty()) {
			return;
		}
		EdgeInsnAttr insnAttr = edgeInsnAttrs.get(0);
		if (!insnAttr.getStart().equals(last)) {
			return;
		}
		if (last instanceof BlockNode) {
			BlockNode block = (BlockNode) last;
			if (block.getInstructions().isEmpty()) {
				block.getInstructions().add(insnAttr.getInsn());
				return;
			}
		}
		List<InsnNode> insns = Collections.singletonList(insnAttr.getInsn());
		region.add(new InsnContainer(insns));
	}

	private static void processSwitch(MethodNode mth, SwitchRegion sw) {
		for (IContainer c : sw.getBranches()) {
			if (c instanceof Region) {
				Set<IBlock> blocks = new HashSet<>();
				RegionUtils.getAllRegionBlocks(c, blocks);
				if (blocks.isEmpty()) {
					addBreakToContainer((Region) c);
				} else {
					for (IBlock block : blocks) {
						if (block instanceof BlockNode) {
							addBreakForBlock(mth, c, blocks, (BlockNode) block);
						}
					}
				}
			}
		}
	}

	private static void addBreakToContainer(Region c) {
		if (RegionUtils.hasExitEdge(c)) {
			return;
		}
		List<InsnNode> insns = new ArrayList<>(1);
		insns.add(new InsnNode(InsnType.BREAK, 0));
		c.add(new InsnContainer(insns));
	}

	private static void addBreakForBlock(MethodNode mth, IContainer c, Set<IBlock> blocks, BlockNode bn) {
		for (BlockNode s : bn.getCleanSuccessors()) {
			if (!blocks.contains(s)
					&& !bn.contains(AFlag.ADDED_TO_REGION)
					&& !s.contains(AFlag.FALL_THROUGH)) {
				addBreak(mth, c, bn);
				return;
			}
		}
	}

	private static void addBreak(MethodNode mth, IContainer c, BlockNode bn) {
		IContainer blockContainer = RegionUtils.getBlockContainer(c, bn);
		if (blockContainer instanceof Region) {
			addBreakToContainer((Region) blockContainer);
		} else if (c instanceof Region) {
			addBreakToContainer((Region) c);
		} else {
			LOG.warn("Can't insert break, container: {}, block: {}, mth: {}", blockContainer, bn, mth);
		}
	}

	private PostProcessRegions() {
		// singleton
	}
}
