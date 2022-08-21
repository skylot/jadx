package jadx.core.dex.nodes;

import jadx.api.data.IRenameNode;

public interface IDexNode extends IRenameNode {

	String typeName();

	RootNode root();

	String getInputFileName();
}
