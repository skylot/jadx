package jadx.core.dex.instructions.mods;

import java.util.Collection;

import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.regions.conditions.IfCondition;
import jadx.core.utils.InsnUtils;
import jadx.core.utils.Utils;

public final class TernaryInsn extends InsnNode {

	private IfCondition condition;

	public TernaryInsn(IfCondition condition, RegisterArg result) {
		this(condition, result, LiteralArg.TRUE, LiteralArg.FALSE);
	}

	public TernaryInsn(IfCondition condition, RegisterArg result, InsnArg th, InsnArg els) {
		super(InsnType.TERNARY, 2);
		setResult(result);

		if (th.equals(LiteralArg.FALSE) && els.equals(LiteralArg.TRUE)) {
			// inverted
			this.condition = IfCondition.invert(condition);
			addArg(els);
			addArg(th);
		} else {
			this.condition = condition;
			addArg(th);
			addArg(els);
		}
	}

	public IfCondition getCondition() {
		return condition;
	}

	public void simplifyCondition() {
		condition = IfCondition.simplify(condition);
		if (condition.getMode() == IfCondition.Mode.NOT) {
			invert();
		}
	}

	private void invert() {
		condition = IfCondition.invert(condition);
		InsnArg tmp = getArg(0);
		setArg(0, getArg(1));
		setArg(1, tmp);
	}

	@Override
	public void getRegisterArgs(Collection<RegisterArg> list) {
		super.getRegisterArgs(list);
		list.addAll(condition.getRegisterArgs());
	}

	@Override
	public boolean isSame(InsnNode obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof TernaryInsn) || !super.isSame(obj)) {
			return false;
		}
		TernaryInsn that = (TernaryInsn) obj;
		return condition.equals(that.condition);
	}

	@Override
	public String toString() {
		return InsnUtils.formatOffset(offset) + ": TERNARY"
				+ getResult() + " = "
				+ Utils.listToString(getArguments());
	}
}
