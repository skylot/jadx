package jadx.dex.visitors.regions;

import jadx.dex.nodes.MethodNode;
import jadx.dex.trycatch.ExceptionHandler;
import jadx.dex.visitors.AbstractVisitor;
import jadx.utils.exceptions.JadxException;

/**
 * Pack blocks into regions for code generation
 */
public class RegionMakerVisitor extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode())
			return;

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
	}
}
