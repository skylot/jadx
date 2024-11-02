package jadx.core.dex.visitors.blocks;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.AbstractVisitor;

public class BlockFinisher extends AbstractVisitor {
	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode() || mth.getBasicBlocks().isEmpty()) {
			return;
		}
		if (!mth.contains(AFlag.DISABLE_BLOCKS_LOCK)) {
			mth.finishBasicBlocks();
		}
	}
}
