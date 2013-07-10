package jadx.core.dex.instructions;

import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.InsnUtils;

public class IndexInsnNode extends InsnNode {

	private final Object index;

	public IndexInsnNode(InsnType type, Object index, int argCount) {
		super(type, argCount);
		this.index = index;
	}

	public Object getIndex() {
		return index;
	}

	@Override
	public String toString() {
		return super.toString() + " " + InsnUtils.indexToString(index);
	}
}
