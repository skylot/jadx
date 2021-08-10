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
import jadx.core.dex.regions.SynchronizedRegion;
import jadx.core.dex.regions.loops.LoopRegion;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.dex.visitors.shrink.CodeShrinkVisitor;
import jadx.core.utils.InsnRemover;
import jadx.core.utils.RegionUtils;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxException;

/**
 * Pack blocks into regions for code generation
 */
public class RegionMakerVisitor extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(RegionMakerVisitor.class);

	private static final IRegionVisitor POST_REGION_VISITOR = new PostRegionVisitor();

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode() || mth.getBasicBlocks().isEmpty()) {
			return;
		}
		RegionMaker rm = new RegionMaker(mth);
		RegionStack state = new RegionStack(mth);

		// fill region structure
		BlockNode startBlock = Utils.first(mth.getEnterBlock().getCleanSuccessors());
		mth.setRegion(rm.makeRegion(startBlock, state));

		if (!mth.isNoExceptionHandlers()) {
			IRegion expOutBlock = rm.processTryCatchBlocks(mth);
			if (expOutBlock != null) {
				mth.getRegion().add(expOutBlock);
			}
		}
		postProcessRegions(mth);
	}

	private static void postProcessRegions(MethodNode mth) {
		processForceInlineInsns(mth);

		// make try-catch regions
		ProcessTryCatchRegions.process(mth);

		DepthRegionTraversal.traverse(mth, POST_REGION_VISITOR);

		CleanRegions.process(mth);

		if (mth.getAccessFlags().isSynchronized()) {
			removeSynchronized(mth);
		}
	}

	private static void processForceInlineInsns(MethodNode mth) {
		boolean needShrink = mth.getBasicBlocks().stream()
				.flatMap(block -> block.getInstructions().stream())
				.anyMatch(insn -> insn.contains(AFlag.FORCE_ASSIGN_INLINE));
		if (needShrink) {
			CodeShrinkVisitor.shrinkMethod(mth);
		}
	}

	private static final class PostRegionVisitor extends AbstractRegionVisitor {
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
	}

	private static void removeSynchronized(MethodNode mth) {
		Region startRegion = mth.getRegion();
		List<IContainer> subBlocks = startRegion.getSubBlocks();
		if (!subBlocks.isEmpty() && subBlocks.get(0) instanceof SynchronizedRegion) {
			SynchronizedRegion synchRegion = (SynchronizedRegion) subBlocks.get(0);
			InsnNode synchInsn = synchRegion.getEnterInsn();
			if (!synchInsn.getArg(0).isThis()) {
				LOG.warn("In synchronized method {}, top region not synchronized by 'this' {}", mth, synchInsn);
				return;
			}
			// replace synchronized block with inner region
			startRegion.getSubBlocks().set(0, synchRegion.getRegion());
			// remove 'monitor-enter' instruction
			InsnRemover.remove(mth, synchInsn);
			// remove 'monitor-exit' instruction
			for (InsnNode exit : synchRegion.getExitInsns()) {
				InsnRemover.remove(mth, exit);
			}
			// run region cleaner again
			CleanRegions.process(mth);
			// assume that CodeShrinker will be run after this
		}
	}
}
