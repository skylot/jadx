package jadx.core.dex.instructions;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.InsnRemover;
import jadx.core.utils.exceptions.JadxRuntimeException;

public final class PhiInsn extends InsnNode {

	// map arguments to blocks (in same order as in arguments list)
	private final List<BlockNode> blockBinds;

	public PhiInsn(int regNum, int predecessors) {
		this(predecessors);
		setResult(InsnArg.reg(regNum, ArgType.UNKNOWN));
		add(AFlag.DONT_INLINE);
		add(AFlag.DONT_GENERATE);
	}

	private PhiInsn(int argsCount) {
		super(InsnType.PHI, argsCount);
		this.blockBinds = new ArrayList<>(argsCount);
	}

	public RegisterArg bindArg(BlockNode pred) {
		RegisterArg arg = InsnArg.reg(getResult().getRegNum(), getResult().getInitType());
		bindArg(arg, pred);
		return arg;
	}

	public void bindArg(RegisterArg arg, BlockNode pred) {
		if (blockBinds.contains(pred)) {
			throw new JadxRuntimeException("Duplicate predecessors in PHI insn: " + pred + ", " + this);
		}
		super.addArg(arg);
		blockBinds.add(pred);
	}

	@Nullable
	public BlockNode getBlockByArg(RegisterArg arg) {
		int index = getArgIndex(arg);
		if (index == -1) {
			return null;
		}
		return blockBinds.get(index);
	}

	public BlockNode getBlockByArgIndex(int argIndex) {
		return blockBinds.get(argIndex);
	}

	@Override
	@NotNull
	public RegisterArg getArg(int n) {
		return (RegisterArg) super.getArg(n);
	}

	@Override
	public boolean removeArg(InsnArg arg) {
		int index = getArgIndex(arg);
		if (index == -1) {
			return false;
		}
		removeArg(index);
		return true;
	}

	@Override
	public RegisterArg removeArg(int index) {
		RegisterArg reg = (RegisterArg) super.removeArg(index);
		blockBinds.remove(index);
		reg.getSVar().updateUsedInPhiList();
		return reg;
	}

	@Nullable
	public RegisterArg getArgBySsaVar(SSAVar ssaVar) {
		if (getArgsCount() == 0) {
			return null;
		}
		for (InsnArg insnArg : getArguments()) {
			RegisterArg reg = (RegisterArg) insnArg;
			if (reg.getSVar() == ssaVar) {
				return reg;
			}
		}
		return null;
	}

	@Override
	public boolean replaceArg(InsnArg from, InsnArg to) {
		if (!(from instanceof RegisterArg) || !(to instanceof RegisterArg)) {
			return false;
		}

		int argIndex = getArgIndex(from);
		if (argIndex == -1) {
			return false;
		}
		((RegisterArg) to).getSVar().addUsedInPhi(this);
		super.setArg(argIndex, to);

		InsnRemover.unbindArgUsage(null, from);
		((RegisterArg) from).getSVar().updateUsedInPhiList();
		return true;
	}

	@Override
	public void addArg(InsnArg arg) {
		throw new JadxRuntimeException("Direct addArg is forbidden for PHI insn, bindArg must be used");
	}

	@Override
	public void setArg(int n, InsnArg arg) {
		throw new JadxRuntimeException("Direct setArg is forbidden for PHI insn, bindArg must be used");
	}

	@Override
	public InsnNode copy() {
		return copyCommonParams(new PhiInsn(getArgsCount()));
	}

	@Override
	public String toString() {
		return baseString() + " binds: " + blockBinds + attributesString();
	}
}
