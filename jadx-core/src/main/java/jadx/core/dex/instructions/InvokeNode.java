package jadx.core.dex.instructions;

import org.jetbrains.annotations.Nullable;

import com.android.dx.io.instructions.DecodedInstruction;

import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.InsnUtils;

public class InvokeNode extends InsnNode implements CallMthInterface {

	private final InvokeType type;
	private final MethodInfo mth;

	public InvokeNode(MethodInfo mth, DecodedInstruction insn, InvokeType type, boolean isRange, int resReg) {
		super(InsnType.INVOKE, mth.getArgsCount() + (type == InvokeType.STATIC ? 0 : 1));
		this.mth = mth;
		this.type = type;

		if (resReg >= 0) {
			setResult(InsnArg.reg(resReg, mth.getReturnType()));
		}

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

	public InvokeNode(MethodInfo mth, InvokeType invokeType, int argsCount) {
		super(InsnType.INVOKE, argsCount);
		this.mth = mth;
		this.type = invokeType;
	}

	public InvokeType getInvokeType() {
		return type;
	}

	@Override
	public MethodInfo getCallMth() {
		return mth;
	}

	@Override
	@Nullable
	public RegisterArg getInstanceArg() {
		if (type != InvokeType.STATIC && getArgsCount() > 0) {
			InsnArg firstArg = getArg(0);
			if (firstArg.isRegister()) {
				return ((RegisterArg) firstArg);
			}
		}
		return null;
	}

	@Override
	public int getFirstArgOffset() {
		return type == InvokeType.STATIC ? 0 : 1;
	}

	@Override
	public InsnNode copy() {
		return copyCommonParams(new InvokeNode(mth, type, getArgsCount()));
	}

	@Override
	public boolean isSame(InsnNode obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof InvokeNode) || !super.isSame(obj)) {
			return false;
		}
		InvokeNode other = (InvokeNode) obj;
		return type == other.type && mth.equals(other.mth);
	}

	@Override
	public String toString() {
		return super.toString() + ' ' + mth + " type: " + type;
	}
}
