package jadx.core.dex.regions.conditions;

import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.IfOp;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.LiteralArg;

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
		return insn.getArg(1);
	}

	public IfNode getInsn() {
		return insn;
	}

	public Compare invert() {
		insn.invertCondition();
		return this;
	}

	/**
	 * Change 'a != false' to 'a == true'
	 */
	public void normalize() {
		if (getOp() == IfOp.NE && getB().isLiteral() && getB().equals(LiteralArg.FALSE)) {
			insn.changeCondition(IfOp.EQ, getA(), LiteralArg.TRUE);
		}
	}

	@Override
	public String toString() {
		return getA() + " " + getOp().getSymbol() + " " + getB();
	}
}
