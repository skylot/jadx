package jadx.core.dex.attributes;

public abstract class AttrNode implements IAttributeNode {

	private AttributesList attributesList;

	@Override
	public AttributesList getAttributes() {
		if (attributesList == null) {
			attributesList = new AttributesList();
		}
		return attributesList;
	}

}
