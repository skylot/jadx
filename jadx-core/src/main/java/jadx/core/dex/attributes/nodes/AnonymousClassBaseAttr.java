package jadx.core.dex.attributes.nodes;

import jadx.api.plugins.input.data.attributes.PinnedAttribute;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.instructions.args.ArgType;

public class AnonymousClassBaseAttr extends PinnedAttribute {

	private final ArgType baseType;

	public AnonymousClassBaseAttr(ArgType baseType) {
		this.baseType = baseType;
	}

	public ArgType getBaseType() {
		return baseType;
	}

	@Override
	public AType<AnonymousClassBaseAttr> getAttrType() {
		return AType.ANONYMOUS_CLASS_BASE;
	}

	@Override
	public String toString() {
		return "AnonymousClassBaseAttr{" + baseType + '}';
	}
}
