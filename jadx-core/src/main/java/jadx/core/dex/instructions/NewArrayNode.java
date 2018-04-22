package jadx.core.dex.instructions;

import org.jetbrains.annotations.NotNull;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.InsnNode;

public class NewArrayNode extends InsnNode {

	private final ArgType arrType;

	public NewArrayNode(@NotNull ArgType arrType, RegisterArg res, InsnArg size) {
		super(InsnType.NEW_ARRAY, 1);
		this.arrType = arrType;
		setResult(res);
		addArg(size);
	}

	public ArgType getArrayType() {
		return arrType;
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
	public String toString() {
		return super.toString() + " type: " + arrType;
	}
}
