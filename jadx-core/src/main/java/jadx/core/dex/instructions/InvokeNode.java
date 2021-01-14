package jadx.core.dex.instructions;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.insns.InsnData;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.nodes.InsnNode;

public class InvokeNode extends BaseInvokeNode {

	private final InvokeType type;
	private final MethodInfo mth;

	public InvokeNode(MethodInfo mthInfo, InsnData insn, InvokeType invokeType, boolean isRange) {
		this(mthInfo, insn, invokeType, invokeType != InvokeType.STATIC, isRange);
	}

	public InvokeNode(MethodInfo mth, InsnData insn, InvokeType type, boolean instanceCall, boolean isRange) {
		super(InsnType.INVOKE, mth.getArgsCount() + (instanceCall ? 1 : 0));
		this.mth = mth;
		this.type = type;

		int k = isRange ? insn.getReg(0) : 0;
		if (instanceCall) {
			int r = isRange ? k : insn.getReg(k);
			addReg(r, mth.getDeclClass().getType());
			k++;
		}

		for (ArgType arg : mth.getArgumentsTypes()) {
			addReg(isRange ? k : insn.getReg(k), arg);
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
	public InsnArg getInstanceArg() {
		if (type != InvokeType.STATIC && getArgsCount() > 0) {
			return getArg(0);
		}
		return null;
	}

	@Override
	public boolean isStaticCall() {
		return type == InvokeType.STATIC;
	}

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
		return super.toString() + " type: " + type + " call: " + mth;
	}
}
