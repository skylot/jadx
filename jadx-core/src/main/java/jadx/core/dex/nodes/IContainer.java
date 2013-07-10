package jadx.core.dex.nodes;

import jadx.core.dex.attributes.AttributesList;
import jadx.core.dex.attributes.IAttributeNode;

public interface IContainer extends IAttributeNode {

	@Override
	public AttributesList getAttributes();

}
