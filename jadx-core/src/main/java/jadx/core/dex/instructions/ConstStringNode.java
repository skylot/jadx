package jadx.core.dex.instructions;

import jadx.core.dex.nodes.InsnNode;

public class ConstStringNode extends InsnNode {

	private final String str;

	public ConstStringNode(String str) {
		super(InsnType.CONST_STR, 0);
		this.str = str;
	}

	public String getString() {
		return str;
	}

	@Override
	public String toString() {
		return super.toString() + " \"" + str + "\"";
	}
}
