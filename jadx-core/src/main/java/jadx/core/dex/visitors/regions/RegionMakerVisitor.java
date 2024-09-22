package jadx.core.dex.visitors.regions;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.dex.visitors.JadxVisitor;
import jadx.core.dex.visitors.regions.maker.ExcHandlersRegionMaker;
import jadx.core.dex.visitors.regions.maker.RegionMaker;
import jadx.core.dex.visitors.regions.maker.SynchronizedRegionMaker;
import jadx.core.dex.visitors.shrink.CodeShrinkVisitor;
import jadx.core.utils.exceptions.JadxException;

@JadxVisitor(
		name = "RegionMakerVisitor",
		desc = "Pack blocks into regions for code generation"
)
public class RegionMakerVisitor extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode() || mth.getBasicBlocks().isEmpty()) {
			return;
		}
		RegionMaker rm = new RegionMaker(mth);
		mth.setRegion(rm.makeMthRegion());
		if (!mth.isNoExceptionHandlers()) {
			new ExcHandlersRegionMaker(mth, rm).process();
		}
		processForceInlineInsns(mth);
		ProcessTryCatchRegions.process(mth);
		PostProcessRegions.process(mth);
		CleanRegions.process(mth);
		if (mth.getAccessFlags().isSynchronized()) {
			SynchronizedRegionMaker.removeSynchronized(mth);
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

	@Override
	public String getName() {
		return "RegionMakerVisitor";
	}
}
