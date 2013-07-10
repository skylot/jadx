package jadx.core.dex.instructions.mods;

import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.IfOp;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.InsnUtils;
import jadx.core.utils.Utils;

public class TernaryInsn extends IfNode {

	public TernaryInsn(IfOp op, InsnNode then, InsnNode els) {
		super(then.getOffset(),
				InsnArg.wrap(then),
				els == null ? null : InsnArg.wrap(els));
	}

	@Override
	public InsnType getType() {
		return InsnType.TERNARY;
	}

	@Override
	public String toString() {
		return InsnUtils.formatOffset(offset) + ": TERNARY"
				+ getResult() + " = "
				+ Utils.listToString(getArguments());
	}
}
