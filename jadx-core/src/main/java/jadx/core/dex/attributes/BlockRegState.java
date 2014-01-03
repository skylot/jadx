package jadx.core.dex.attributes;

import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.TypedVar;
import jadx.core.dex.nodes.MethodNode;

public final class BlockRegState {

	private final RegisterArg[] regs;

	public BlockRegState(MethodNode mth) {
		this.regs = new RegisterArg[mth.getRegsCount()];
		for (int i = 0; i < regs.length; i++) {
			regs[i] = new RegisterArg(i);
		}
	}

	public BlockRegState(BlockRegState state) {
		this.regs = new RegisterArg[state.regs.length];
		System.arraycopy(state.regs, 0, regs, 0, state.regs.length);
	}

	public void assignReg(RegisterArg arg) {
		regs[arg.getRegNum()] = arg;
		arg.getTypedVar().getUseList().add(arg);
	}

	public void use(RegisterArg arg) {
		RegisterArg reg = regs[arg.getRegNum()];
		TypedVar regType = reg.getTypedVar();
		if (regType == null) {
			regType = new TypedVar(arg.getType());
			reg.forceSetTypedVar(regType);
		}
		arg.replaceTypedVar(reg);
		reg.getTypedVar().getUseList().add(arg);
	}

	public RegisterArg getRegister(int r) {
		return regs[r];
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		for (RegisterArg reg : regs) {
			if (reg.getTypedVar() != null) {
				if (str.length() != 0) {
					str.append(", ");
				}
				str.append(reg.toString());
			}
		}
		return str.toString();
	}
}
