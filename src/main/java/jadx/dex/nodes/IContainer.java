package jadx.dex.nodes;

import jadx.dex.attributes.AttributesList;
import jadx.dex.attributes.IAttributeNode;

public interface IContainer extends IAttributeNode {

	@Override
	public AttributesList getAttributes();

}
