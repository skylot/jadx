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
	public IndexInsnNode copy() {
		return copyCommonParams(new IndexInsnNode(insnType, index, getArgsCount()));
	}

	@Override
	public boolean isSame(InsnNode obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof IndexInsnNode) || !super.isSame(obj)) {
			return false;
		}
		IndexInsnNode other = (IndexInsnNode) obj;
		return index == null ? other.index == null : index.equals(other.index);
	}

	@Override
	public String toString() {
		return super.toString() + " " + InsnUtils.indexToString(index);
	}
}
