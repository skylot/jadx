package jadx.core.dex.visitors.ssa;

import java.util.Arrays;

import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.MethodNode;

final class RenameState {
	private final MethodNode mth;
	private final BlockNode block;
	private final SSAVar[] vars;
	private final int[] versions;

	public static RenameState init(MethodNode mth) {
		int regsCount = mth.getRegsCount();
		RenameState state = new RenameState(
				mth,
				mth.getEnterBlock(),
				new SSAVar[regsCount],
				new int[regsCount]);
		RegisterArg thisArg = mth.getThisArg();
		if (thisArg != null) {
			state.startVar(thisArg);
		}
		for (RegisterArg arg : mth.getArgRegs()) {
			state.startVar(arg);
		}
		return state;
	}

	public static RenameState copyFrom(RenameState state, BlockNode block) {
		return new RenameState(
				state.mth,
				block,
				Arrays.copyOf(state.vars, state.vars.length),
				state.versions);
	}

	private RenameState(MethodNode mth, BlockNode block, SSAVar[] vars, int[] versions) {
		this.mth = mth;
		this.block = block;
		this.vars = vars;
		this.versions = versions;
	}

	public BlockNode getBlock() {
		return block;
	}

	public SSAVar getVar(int regNum) {
		return vars[regNum];
	}

	public SSAVar startVar(RegisterArg regArg) {
		int regNum = regArg.getRegNum();
		int version = versions[regNum]++;
		SSAVar ssaVar = mth.makeNewSVar(regNum, version, regArg);
		vars[regNum] = ssaVar;
		return ssaVar;
	}
}
