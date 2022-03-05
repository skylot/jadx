package jadx.core.dex.regions.conditions;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.IfOp;
import jadx.core.dex.instructions.args.InsnArg;

public final class Compare {
	private final IfNode insn;

	public Compare(IfNode insn) {
		insn.add(AFlag.HIDDEN);
		this.insn = insn;
	}

	public IfOp getOp() {
		return insn.getOp();
	}

	public InsnArg getA() {
		return insn.getArg(0);
	}

	public InsnArg getB() {
		return insn.getArg(1);
	}

	public IfNode getInsn() {
		return insn;
	}

	public Compare invert() {
		insn.invertCondition();
		return this;
	}

	public void normalize() {
		insn.normalize();
	}

	@Override
	public String toString() {
		return getA() + " " + getOp().getSymbol() + ' ' + getB();
	}
}
