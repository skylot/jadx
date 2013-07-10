package jadx.core.dex.nodes;

import java.util.List;

public interface IRegion extends IContainer {

	public IRegion getParent();

	public List<IContainer> getSubBlocks();

}
