package jadx.core.dex.instructions;

import org.jetbrains.annotations.NotNull;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.InsnNode;

public class FilledNewArrayNode extends InsnNode {

	private final ArgType elemType;

	public FilledNewArrayNode(@NotNull ArgType elemType, int size) {
		super(InsnType.FILLED_NEW_ARRAY, size);
		this.elemType = elemType;
	}

	public ArgType getElemType() {
		return elemType;
	}

	public ArgType getArrayType() {
		return ArgType.array(elemType);
	}

	@Override
	public boolean isSame(InsnNode obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof FilledNewArrayNode) || !super.isSame(obj)) {
			return false;
		}
		FilledNewArrayNode other = (FilledNewArrayNode) obj;
		return elemType == other.elemType;
	}

	@Override
	public String toString() {
		return super.toString() + " elemType: " + elemType;
	}
}
