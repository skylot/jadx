package jadx.core.dex.instructions;

import jadx.core.dex.nodes.InsnNode;

public class ConstStringInsn extends InsnNode {

	private final String str;

	public ConstStringInsn(String str) {
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
