package jadx.core.dex.nodes;

import jadx.core.dex.attributes.IAttributeNode;

public interface IContainer extends IAttributeNode {

	/**
	 * Unique id for use in 'toString()' method
	 */
	String baseString();
}
