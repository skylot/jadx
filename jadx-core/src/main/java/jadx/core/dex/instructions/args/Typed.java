package jadx.core.dex.instructions.args;

import jadx.core.dex.attributes.AttrNode;

public abstract class Typed extends AttrNode {

	protected ArgType type;

	public ArgType getType() {
		return type;
	}

	public void setType(ArgType type) {
		this.type = type;
	}

	public boolean isTypeImmutable() {
		return false;
	}
}
