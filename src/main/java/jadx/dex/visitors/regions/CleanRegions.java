package jadx.dex.visitors.regions;

import jadx.dex.nodes.BlockNode;
import jadx.dex.nodes.IContainer;
import jadx.dex.nodes.IRegion;
import jadx.dex.nodes.MethodNode;
import jadx.dex.regions.Region;
import jadx.dex.visitors.AbstractVisitor;
import jadx.utils.exceptions.JadxException;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CleanRegions extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(CleanRegions.class);

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode() || mth.getBasicBlocks().size() == 0)
			return;

		IRegionVisitor removeEmptyBlocks = new AbstractRegionVisitor() {
			@Override
			public void enterRegion(MethodNode mth, IRegion region) {
				if (!(region instanceof Region))
					return;

				for (Iterator<IContainer> it = region.getSubBlocks().iterator(); it.hasNext();) {
					IContainer container = it.next();
					if (container instanceof BlockNode) {
						BlockNode block = (BlockNode) container;
						if (block.getInstructions().isEmpty()) {
							try {
								it.remove();
							} catch (UnsupportedOperationException e) {
								LOG.warn("Can't remove block: {} from: {}, mth: {}", block, region, mth);
							}
						}
					}

				}
			}
		};
		DepthRegionTraverser.traverseAll(mth, removeEmptyBlocks);

	}
}
