package jadx.core.dex.attributes.nodes;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttribute;
import jadx.core.dex.instructions.args.ArgType;

public class GenericInfoAttr implements IAttribute {
	private final ArgType[] genericTypes;
	private boolean explicit;

	public GenericInfoAttr(ArgType[] genericTypes) {
		this.genericTypes = genericTypes;
	}

	public ArgType[] getGenericTypes() {
		return genericTypes;
	}

	public boolean isExplicit() {
		return explicit;
	}

	public void setExplicit(boolean explicit) {
		this.explicit = explicit;
	}

	@Override
	public AType<GenericInfoAttr> getType() {
		return AType.GENERIC_INFO;
	}
}
