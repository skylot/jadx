package jadx.core.dex.nodes;

import jadx.core.dex.attributes.IAttributeNode;

public interface IContainer extends IAttributeNode {

	// unique id for use in 'toString()' method
	String baseString();
}
