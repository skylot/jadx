package jadx.core.dex.regions;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;

import java.util.Collections;
import java.util.List;

public final class TernaryRegion extends AbstractRegion {
	private final IBlock container;

	public TernaryRegion(IRegion parent, BlockNode block) {
		super(parent);
		this.container = block;
	}

	public IBlock getBlock() {
		return container;
	}

	@Override
	public List<IContainer> getSubBlocks() {
		return Collections.singletonList((IContainer) container);
	}

	@Override
	public String toString() {
		return "TERN:" + container;
	}
}
