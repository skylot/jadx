package jadx.core.dex.nodes;

import jadx.api.core.nodes.IRenameNode;

public interface IDexNode extends IRenameNode {

	String typeName();

	RootNode root();

	String getInputFileName();
}
