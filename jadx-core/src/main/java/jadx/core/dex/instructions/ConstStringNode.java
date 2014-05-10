package jadx.core.dex.instructions;

import jadx.core.dex.nodes.InsnNode;

public final class ConstStringNode extends InsnNode {

	private final String str;

	public ConstStringNode(String str) {
		super(InsnType.CONST_STR, 0);
		this.str = str;
	}

	public String getString() {
		return str;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ConstStringNode) || !super.equals(obj)) {
			return false;
		}
		ConstStringNode that = (ConstStringNode) obj;
		return str.equals(that.str);
	}

	@Override
	public int hashCode() {
		return 31 * super.hashCode() + str.hashCode();
	}

	@Override
	public String toString() {
		return super.toString() + " \"" + str + "\"";
	}
}
