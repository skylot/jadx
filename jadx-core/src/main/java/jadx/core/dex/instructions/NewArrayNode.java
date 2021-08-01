package jadx.core.dex.instructions;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.InsnNode;

public class NewArrayNode extends InsnNode {

	private final ArgType arrType;

	public NewArrayNode(ArgType arrType, int argsCount) {
		super(InsnType.NEW_ARRAY, argsCount);
		this.arrType = arrType;
	}

	public ArgType getArrayType() {
		return arrType;
	}

	public int getDimension() {
		return arrType.getArrayDimension();
	}

	@Override
	public boolean isSame(InsnNode obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof NewArrayNode) || !super.isSame(obj)) {
			return false;
		}
		NewArrayNode other = (NewArrayNode) obj;
		return arrType == other.arrType;
	}

	@Override
	public InsnNode copy() {
		return copyCommonParams(new NewArrayNode(arrType, getArgsCount()));
	}

	@Override
	public String toString() {
		return super.toString() + " type: " + arrType;
	}
}
