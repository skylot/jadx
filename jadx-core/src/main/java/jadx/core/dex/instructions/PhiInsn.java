package jadx.core.dex.instructions;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.InstructionRemover;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;

public final class PhiInsn extends InsnNode {

	private final Map<RegisterArg, BlockNode> blockBinds;

	public PhiInsn(int regNum, int predecessors) {
		super(InsnType.PHI, predecessors);
		this.blockBinds = new LinkedHashMap<>(predecessors);
		setResult(InsnArg.reg(regNum, ArgType.UNKNOWN));
		add(AFlag.DONT_INLINE);
	}

	public RegisterArg bindArg(BlockNode pred) {
		RegisterArg arg = InsnArg.reg(getResult().getRegNum(), getResult().getType());
		bindArg(arg, pred);
		return arg;
	}

	public void bindArg(RegisterArg arg, BlockNode pred) {
		if (blockBinds.containsValue(pred)) {
			throw new JadxRuntimeException("Duplicate predecessors in PHI insn: " + pred + ", " + this);
		}
		addArg(arg);
		blockBinds.put(arg, pred);
	}

	public BlockNode getBlockByArg(RegisterArg arg) {
		return blockBinds.get(arg);
	}

	public Map<RegisterArg, BlockNode> getBlockBinds() {
		return blockBinds;
	}

	@Override
	@NotNull
	public RegisterArg getArg(int n) {
		return (RegisterArg) super.getArg(n);
	}

	@Override
	public boolean removeArg(InsnArg arg) {
		if (!(arg instanceof RegisterArg)) {
			return false;
		}
		RegisterArg reg = (RegisterArg) arg;
		if (super.removeArg(reg)) {
			blockBinds.remove(reg);
			InstructionRemover.fixUsedInPhiFlag(reg);
			return true;
		}
		return false;
	}

	@Override
	public boolean replaceArg(InsnArg from, InsnArg to) {
		if (!(from instanceof RegisterArg) || !(to instanceof RegisterArg)) {
			return false;
		}
		BlockNode pred = getBlockByArg((RegisterArg) from);
		if (pred == null) {
			throw new JadxRuntimeException("Unknown predecessor block by arg " + from + " in PHI: " + this);
		}
		if (removeArg(from)) {
			bindArg((RegisterArg) to, pred);
		}
		return true;
	}

	@Override
	public void setArg(int n, InsnArg arg) {
		throw new JadxRuntimeException("Unsupported operation for PHI node");
	}

	@Override
	public String toString() {
		return "PHI: " + getResult() + " = " + Utils.listToString(getArguments())
				+ " binds: " + blockBinds;
	}
}
