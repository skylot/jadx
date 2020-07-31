package jadx.core.dex.instructions;

import java.util.Objects;

import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.InsnUtils;
import jadx.core.utils.Utils;

public class IndexInsnNode extends InsnNode {

	private Object index;

	public IndexInsnNode(InsnType type, Object index, int argCount) {
		super(type, argCount);
		this.index = index;
	}

	public Object getIndex() {
		return index;
	}

	public void updateIndex(Object index) {
		this.index = index;
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
		return Objects.equals(index, other.index);
	}

	@Override
	public String toString() {
		switch (insnType) {
			case CAST:
			case CHECK_CAST:
				StringBuilder sb = new StringBuilder();
				sb.append(InsnUtils.formatOffset(offset)).append(": ");
				sb.append(insnType).append(' ');
				if (getResult() != null) {
					sb.append(getResult()).append(" = ");
				}
				sb.append('(').append(InsnUtils.indexToString(index)).append(") ");
				sb.append(Utils.listToString(getArguments()));
				return sb.toString();

			default:
				return super.toString() + ' ' + InsnUtils.indexToString(index);
		}
	}
}
