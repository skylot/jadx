package jadx.core.dex.instructions;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.Utils;

public class PhiInsn extends InsnNode {

	public PhiInsn(int regNum, int predecessors) {
		super(InsnType.PHI, predecessors);
		setResult(InsnArg.reg(regNum, ArgType.UNKNOWN));
		for (int i = 0; i < predecessors; i++) {
			addReg(regNum, ArgType.UNKNOWN);
		}
	}

	@Override
	public RegisterArg getArg(int n) {
		return (RegisterArg) super.getArg(n);
	}

	@Override
	public String toString() {
		return "PHI: " + getResult() + " = " + Utils.listToString(getArguments());
	}
}
