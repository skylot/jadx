package jadx.core.dex.regions;

import jadx.core.dex.attributes.AttrNode;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	public void setParent(IRegion parent) {
		this.parent = parent;
	}

	@Override
	public boolean replaceSubBlock(IContainer oldBlock, IContainer newBlock) {
		LOG.warn("Replace sub block not supported for class \"{}\"", this.getClass());
		return false;
	}
}
