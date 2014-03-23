package jadx.core.dex.regions;

import jadx.core.dex.attributes.AttrNode;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;

public abstract class AbstractRegion extends AttrNode implements IRegion {

	private IRegion parent;

	public AbstractRegion(IRegion parent) {
		this.parent = parent;
	}

	@Override
	public IRegion getParent() {
		return parent;
	}

	public void setParent(IRegion parent) {
		this.parent = parent;
	}

	@Override
	public boolean replaceSubBlock(IContainer oldBlock, IContainer newBlock) {
		// TODO: implement for others regions
		return false;
	}
}
