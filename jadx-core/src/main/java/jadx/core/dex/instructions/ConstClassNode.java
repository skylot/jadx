package jadx.core.dex.instructions;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.InsnNode;

public final class ConstClassNode extends InsnNode {

	private final ArgType clsType;

	public ConstClassNode(ArgType clsType) {
		super(InsnType.CONST_CLASS, 0);
		this.clsType = clsType;
	}

	public ArgType getClsType() {
		return clsType;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ConstClassNode) || !super.equals(obj)) {
			return false;
		}
		ConstClassNode that = (ConstClassNode) obj;
		return clsType.equals(that.clsType);
	}

	@Override
	public int hashCode() {
		return 31 * super.hashCode() + clsType.hashCode();
	}

	@Override
	public String toString() {
		return super.toString() + " " + clsType;
	}
}
