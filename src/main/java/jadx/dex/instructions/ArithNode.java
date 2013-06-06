package jadx.dex.instructions;

import jadx.dex.instructions.args.ArgType;
import jadx.dex.instructions.args.InsnArg;
import jadx.dex.instructions.args.RegisterArg;
import jadx.dex.nodes.InsnNode;
import jadx.utils.InsnUtils;

import com.android.dx.io.instructions.DecodedInstruction;

public class ArithNode extends InsnNode {

	private final ArithOp op;

	public ArithNode(DecodedInstruction insn, ArithOp op, ArgType type, boolean literal) {
		super(InsnType.ARITH, 2);
		this.op = op;
		setResult(InsnArg.reg(insn, 0, type));

		int rc = insn.getRegisterCount();
		if (literal) {
			if (rc == 1) {
				// self
				addReg(insn, 0, type);
				addLit(insn, type);
			} else if (rc == 2) {
				// normal
				addReg(insn, 1, type);
				addLit(insn, type);
			}
		} else {
			if (rc == 2) {
				// self
				addReg(insn, 0, type);
				addReg(insn, 1, type);
			} else if (rc == 3) {
				// normal
				addReg(insn, 1, type);
				addReg(insn, 2, type);
			}
		}
		assert getArgsCount() == 2;
	}

	public ArithNode(ArithOp op, RegisterArg res, InsnArg a, InsnArg b) {
		super(InsnType.ARITH, 2);
		this.op = op;
		setResult(res);
		addArg(a);
		addArg(b);
	}

	public ArithNode(ArithOp op, RegisterArg res, InsnArg a) {
		super(InsnType.ARITH, 1);
		this.op = op;
		setResult(res);
		addArg(a);
	}

	public ArithOp getOp() {
		return op;
	}

	@Override
	public String toString() {
		return InsnUtils.formatOffset(offset) + ": "
				+ InsnUtils.insnTypeToString(insnType)
				+ getResult() + " = "
				+ getArg(0) + " "
				+ op.getSymbol() + " "
				+ (getArgsCount() == 2 ? getArg(1) : "");
	}

}
