package jadx.core.dex.visitors.regions;

import jadx.core.dex.attributes.AttributeFlag;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.IfRegion;
import jadx.core.dex.regions.LoopRegion;
import jadx.core.dex.regions.Region;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.exceptions.JadxException;

import java.util.List;

/**
 * Pack blocks into regions for code generation
 */
public class RegionMakerVisitor extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode()) {
			return;
		}
		RegionMaker rm = new RegionMaker(mth);
		RegionStack state = new RegionStack(mth);

		// fill region structure
		mth.setRegion(rm.makeRegion(mth.getEnterBlock(), state));

		if (mth.getExceptionHandlers() != null) {
			state = new RegionStack(mth);
			for (ExceptionHandler handler : mth.getExceptionHandlers()) {
				rm.processExcHandler(handler, state);
			}
		}

		postProcessRegions(mth);
	}

	private static void postProcessRegions(MethodNode mth) {
		// make try-catch regions
		DepthRegionTraverser.traverse(mth, new ProcessTryCatchRegions(mth), mth.getRegion());

		// merge conditions in loops
		if (mth.getLoopsCount() != 0) {
			DepthRegionTraverser.traverseAll(mth, new AbstractRegionVisitor() {
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

		DepthRegionTraverser.traverseAll(mth, new AbstractRegionVisitor() {
			@Override
			public void leaveRegion(MethodNode mth, IRegion region) {
				if (region instanceof IfRegion) {
					processIfRegion((IfRegion) region);

				}
			}
		});

		// remove useless returns in void methods
		if (mth.getReturnType().equals(ArgType.VOID)) {
			DepthRegionTraverser.traverseAll(mth, new ProcessReturnInsns());
		}
	}

	private static void processIfRegion(IfRegion ifRegion) {
		if (ifRegion.simplifyCondition()) {
//			IfCondition condition = ifRegion.getCondition();
//			if (condition.getMode() == IfCondition.Mode.NOT) {
//				ifRegion.invert();
//			}
		}

		// mark if-else-if chains
		IContainer elsRegion = ifRegion.getElseRegion();
		if (elsRegion instanceof IfRegion) {
			elsRegion.getAttributes().add(AttributeFlag.ELSE_IF_CHAIN);
		} else if (elsRegion instanceof Region) {
			List<IContainer> subBlocks = ((Region) elsRegion).getSubBlocks();
			if (subBlocks.size() == 1 && subBlocks.get(0) instanceof IfRegion) {
				subBlocks.get(0).getAttributes().add(AttributeFlag.ELSE_IF_CHAIN);
			}
		}
	}
}
