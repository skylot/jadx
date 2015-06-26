package jadx.core.dex.visitors.regions;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.Region;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CleanRegions {
	private static final Logger LOG = LoggerFactory.getLogger(CleanRegions.class);

	private CleanRegions() {
	}

	public static void process(MethodNode mth) {
		if (mth.isNoCode() || mth.getBasicBlocks().isEmpty()) {
			return;
		}
		IRegionVisitor removeEmptyBlocks = new AbstractRegionVisitor() {
			@Override
			public boolean enterRegion(MethodNode mth, IRegion region) {
				if (!(region instanceof Region)) {
					return true;
				}

				for (Iterator<IContainer> it = region.getSubBlocks().iterator(); it.hasNext(); ) {
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
				return true;
			}
		};
		DepthRegionTraversal.traverse(mth, removeEmptyBlocks);
	}
}
