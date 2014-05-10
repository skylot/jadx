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
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof IndexInsnNode) || !super.equals(obj)) {
			return false;
		}
		IndexInsnNode that = (IndexInsnNode) obj;
		return index == null ? that.index == null : index.equals(that.index);
	}

	@Override
	public int hashCode() {
		return 31 * super.hashCode() + (index != null ? index.hashCode() : 0);
	}

	@Override
	public String toString() {
		return super.toString() + " " + InsnUtils.indexToString(index);
	}
}
