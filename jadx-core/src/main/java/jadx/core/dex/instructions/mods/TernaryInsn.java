package jadx.core.dex.instructions.mods;

import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.regions.IfCondition;
import jadx.core.utils.InsnUtils;
import jadx.core.utils.Utils;

public class TernaryInsn extends InsnNode {

	private final IfCondition condition;

	public TernaryInsn(IfCondition condition, RegisterArg result, InsnArg th, InsnArg els) {
		super(InsnType.TERNARY, 2);
		this.condition = condition;
		setResult(result);
		addArg(th);
		addArg(els);
	}

	public IfCondition getCondition() {
		return condition;
	}

	@Override
	public String toString() {
		return InsnUtils.formatOffset(offset) + ": TERNARY"
				+ getResult() + " = "
				+ Utils.listToString(getArguments());
	}
}
