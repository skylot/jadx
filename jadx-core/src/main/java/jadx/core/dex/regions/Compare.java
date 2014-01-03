package jadx.core.dex.regions;

import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.IfOp;
import jadx.core.dex.instructions.args.InsnArg;

public final class Compare {
	private final IfNode insn;

	public Compare(IfNode insn) {
		this.insn = insn;
	}

	public IfOp getOp() {
		return insn.getOp();
	}

	public InsnArg getA() {
		return insn.getArg(0);
	}

	public InsnArg getB() {
		if (insn.isZeroCmp()) {
			return InsnArg.lit(0, getA().getType());
		} else {
			return insn.getArg(1);
		}
	}

	public Compare invert() {
		insn.invertCondition();
		return this;
	}

	@Override
	public String toString() {
		return getA() + " " + getOp().getSymbol() + " " + getB();
	}
}
