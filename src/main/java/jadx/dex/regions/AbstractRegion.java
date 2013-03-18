package jadx.dex.regions;

import jadx.dex.attributes.AttrNode;
import jadx.dex.nodes.IRegion;

public abstract class AbstractRegion extends AttrNode implements IRegion {

	private final IRegion parent;

	public AbstractRegion(IRegion parent) {
		this.parent = parent;
	}

	@Override
	public IRegion getParent() {
		return parent;
	}

}
