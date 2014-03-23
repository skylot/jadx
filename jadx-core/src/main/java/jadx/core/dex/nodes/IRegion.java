package jadx.core.dex.nodes;

import java.util.List;

public interface IRegion extends IContainer {

	IRegion getParent();

	List<IContainer> getSubBlocks();

	boolean replaceSubBlock(IContainer oldBlock, IContainer newBlock);
}
