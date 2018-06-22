package jadx.core.dex.regions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.attributes.AttrNode;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;

public abstract class AbstractRegion extends AttrNode implements IRegion {
	private static final Logger LOG = LoggerFactory.getLogger(AbstractRegion.class);

	private IRegion parent;

	public AbstractRegion(IRegion parent) {
		this.parent = parent;
	}

	@Override
	public IRegion getParent() {
		return parent;
	}

	@Override
	public void setParent(IRegion parent) {
		this.parent = parent;
	}

	@Override
	public boolean replaceSubBlock(IContainer oldBlock, IContainer newBlock) {
		LOG.warn("Replace sub block not supported for class \"{}\"", this.getClass());
		return false;
	}

	public void updateParent(IContainer container, IRegion newParent) {
		if (container instanceof IRegion) {
			((IRegion) container).setParent(newParent);
		}
	}
}
