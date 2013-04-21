package jadx.dex.instructions;

import jadx.dex.nodes.InsnNode;
import jadx.utils.InsnUtils;

public class IndexInsnNode extends InsnNode {

	protected final Object index;

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
