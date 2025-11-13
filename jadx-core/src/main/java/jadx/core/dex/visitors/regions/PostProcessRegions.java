package jadx.core.dex.visitors.regions;

import java.util.Collections;
import java.util.List;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.EdgeInsnAttr;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnContainer;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.Region;
import jadx.core.dex.regions.SwitchRegion;
import jadx.core.dex.regions.loops.LoopRegion;
import jadx.core.dex.visitors.regions.maker.SwitchRegionMaker;

public final class PostProcessRegions extends AbstractRegionVisitor {
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
			SwitchRegionMaker.insertBreaks(mth, (SwitchRegion) region);
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

	private PostProcessRegions() {
		// singleton
	}
}
