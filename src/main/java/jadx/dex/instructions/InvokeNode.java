package jadx.dex.instructions;

import jadx.dex.info.MethodInfo;
import jadx.dex.instructions.args.ArgType;
import jadx.dex.instructions.args.InsnArg;
import jadx.dex.nodes.InsnNode;
import jadx.dex.nodes.MethodNode;
import jadx.utils.InsnUtils;
import jadx.utils.Utils;

import com.android.dx.io.instructions.DecodedInstruction;

public class InvokeNode extends InsnNode {

	private final InvokeType type;
	private final MethodInfo mth;

	public InvokeNode(MethodNode method, DecodedInstruction insn, InvokeType type, boolean isRange,
			int resReg) {
		super(method, InsnType.INVOKE);
		this.mth = MethodInfo.fromDex(method.dex(), insn.getIndex());
		this.type = type;

		if (resReg >= 0)
			setResult(InsnArg.reg(resReg, mth.getReturnType()));

		int k = isRange ? insn.getA() : 0;
		if (type != InvokeType.STATIC) {
			int r = isRange ? k : InsnUtils.getArg(insn, k);
			addReg(r, mth.getDeclClass().getType());
			k++;
		}

		for (ArgType arg : mth.getArgumentsTypes()) {
			addReg(isRange ? k : InsnUtils.getArg(insn, k), arg);
			k += arg.getRegCount();
		}
	}

	public InvokeType getInvokeType() {
		return type;
	}

	public MethodInfo getCallMth() {
		return mth;
	}

	@Override
	public String toString() {
		return InsnUtils.formatOffset(offset) + ": "
				+ InsnUtils.insnTypeToString(insnType)
				+ (getResult() == null ? "" : getResult() + " = ")
				+ Utils.listToString(getArguments())
				+ " " + mth
				+ " type: " + type;
	}
}
