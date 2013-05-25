package jadx.dex.instructions;

import jadx.dex.instructions.args.ArgType;
import jadx.dex.instructions.args.InsnArg;
import jadx.dex.instructions.args.LiteralArg;
import jadx.dex.instructions.args.PrimitiveType;
import jadx.utils.InsnUtils;

import com.android.dx.io.instructions.DecodedInstruction;

public class IfNode extends GotoNode {

	protected boolean zeroCmp;
	protected IfOp op;

	public IfNode(int targ, InsnArg then, InsnArg els) {
		super(InsnType.IF, targ);
		addArg(then);
		if (els == null) {
			zeroCmp = true;
		} else {
			zeroCmp = false;
			addArg(els);
		}
	}

	public IfNode(DecodedInstruction insn, IfOp op) {
		super(InsnType.IF, insn.getTarget());
		this.op = op;

		ArgType type = ArgType.unknown(
				PrimitiveType.INT, PrimitiveType.OBJECT, PrimitiveType.ARRAY,
				PrimitiveType.BOOLEAN, PrimitiveType.SHORT, PrimitiveType.CHAR);

		addReg(insn, 0, type);
		if (insn.getRegisterCount() == 1) {
			zeroCmp = true;
		} else {
			zeroCmp = false;
			addReg(insn, 1, type);
		}
	}

	public IfOp getOp() {
		return op;
	}

	public boolean isZeroCmp() {
		return zeroCmp;
	}

	public void invertOp(int targ) {
		op = op.invert();
		target = targ;
	}

	public void changeCondition(InsnArg arg1, InsnArg arg2, IfOp op) {
		this.op = op;
		this.zeroCmp = arg2.isLiteral() && ((LiteralArg) arg2).getLiteral() == 0;
		setArg(0, arg1);
		if (!zeroCmp) {
			if (getArgsCount() == 2)
				setArg(1, arg2);
			else
				addArg(arg2);
		}
	}

	@Override
	public String toString() {
		return InsnUtils.formatOffset(offset) + ": "
				+ InsnUtils.insnTypeToString(insnType)
				+ getArg(0) + " " + op.getSymbol()
				+ " " + (zeroCmp ? "0" : getArg(1))
				+ "  -> " + InsnUtils.formatOffset(target);
	}
}
