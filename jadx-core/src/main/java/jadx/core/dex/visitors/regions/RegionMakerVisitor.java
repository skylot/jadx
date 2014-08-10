package jadx.core.dex.visitors.regions;

import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.LoopRegion;
import jadx.core.dex.regions.Region;
import jadx.core.dex.regions.SynchronizedRegion;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.InstructionRemover;
import jadx.core.utils.exceptions.JadxException;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pack blocks into regions for code generation
 */
public class RegionMakerVisitor extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(RegionMakerVisitor.class);

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode()) {
			return;
		}
		RegionMaker rm = new RegionMaker(mth);
		RegionStack state = new RegionStack(mth);

		// fill region structure
		mth.setRegion(rm.makeRegion(mth.getEnterBlock(), state));

		if (!mth.isNoExceptionHandlers()) {
			rm.processTryCatchBlocks(mth);
		}

		postProcessRegions(mth);
	}

	private static void postProcessRegions(MethodNode mth) {
		// make try-catch regions
		ProcessTryCatchRegions.process(mth);

		// merge conditions in loops
		if (mth.getLoopsCount() != 0) {
			DepthRegionTraversal.traverseAll(mth, new AbstractRegionVisitor() {
				@Override
				public void enterRegion(MethodNode mth, IRegion region) {
					if (region instanceof LoopRegion) {
						LoopRegion loop = (LoopRegion) region;
						loop.mergePreCondition();
					}
				}
			});
		}

		CleanRegions.process(mth);

		if (mth.getAccessFlags().isSynchronized()) {
			removeSynchronized(mth);
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
			InstructionRemover.remove(mth, synchInsn);
			// remove 'monitor-exit' instruction
			for (InsnNode exit : synchRegion.getExitInsns()) {
				InstructionRemover.remove(mth, exit);
			}
			// run region cleaner again
			CleanRegions.process(mth);
			// assume that CodeShrinker will be run after this
		}
	}
}
