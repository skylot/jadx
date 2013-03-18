package jadx.dex.instructions.mods;

import jadx.dex.instructions.IfNode;
import jadx.dex.instructions.IfOp;
import jadx.dex.instructions.InsnType;
import jadx.dex.instructions.args.InsnArg;
import jadx.dex.nodes.InsnNode;
import jadx.dex.nodes.MethodNode;
import jadx.utils.InsnUtils;
import jadx.utils.Utils;

public class TernaryInsn extends IfNode {

	public TernaryInsn(MethodNode mth, IfOp op, InsnNode then, InsnNode els) {
		super(mth, op, then.getOffset(),
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
