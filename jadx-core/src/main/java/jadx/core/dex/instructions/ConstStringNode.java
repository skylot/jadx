package jadx.core.dex.instructions;

import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.StringUtils;

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
	public InsnNode copy() {
		return copyCommonParams(new ConstStringNode(str));
	}

	@Override
	public boolean isSame(InsnNode obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ConstStringNode) || !super.isSame(obj)) {
			return false;
		}
		ConstStringNode other = (ConstStringNode) obj;
		return str.equals(other.str);
	}

	@Override
	public String toString() {
		return super.toString() + ' ' + StringUtils.getInstance().unescapeString(str);
	}
}
