package jadx.core.dex.visitors.regions;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.exceptions.JadxException;

public class PostRegionVisitor extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		IContainer startRegion = mth.getRegion();
		if (mth.isNoCode() || startRegion == null) {
			return;
		}
		DepthRegionTraverser.traverse(mth, new ProcessTryCatchRegions(mth), startRegion);
		if (mth.getLoopsCount() != 0) {
			DepthRegionTraverser.traverse(mth, new ProcessLoopRegions(), startRegion);
		}
		if (mth.getReturnType().equals(ArgType.VOID)) {
			DepthRegionTraverser.traverseAll(mth, new ProcessReturnInsns());
		}
	}
}
