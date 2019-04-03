package jadx.core.dex.visitors.regions.variables;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.loops.ForLoop;
import jadx.core.dex.regions.loops.LoopRegion;
import jadx.core.dex.regions.loops.LoopType;
import jadx.core.dex.visitors.regions.TracedRegionVisitor;

class CollectUsageRegionVisitor extends TracedRegionVisitor {
	private final List<RegisterArg> args = new ArrayList<>();
	private final Map<SSAVar, VarUsage> usageMap = new LinkedHashMap<>();

	public Map<SSAVar, VarUsage> getUsageMap() {
		return usageMap;
	}

	@Override
	public void processBlockTraced(MethodNode mth, IBlock block, IRegion curRegion) {
		UsePlace usePlace = new UsePlace(curRegion, block);
		regionProcess(usePlace);
		int len = block.getInstructions().size();
		for (int i = 0; i < len; i++) {
			InsnNode insn = block.getInstructions().get(i);
			processInsn(insn, usePlace);
		}
	}

	private void regionProcess(UsePlace usePlace) {
		IRegion region = usePlace.getRegion();
		if (region instanceof LoopRegion) {
			LoopRegion loopRegion = (LoopRegion) region;
			LoopType loopType = loopRegion.getType();
			if (loopType instanceof ForLoop) {
				ForLoop forLoop = (ForLoop) loopType;
				processInsn(forLoop.getInitInsn(), usePlace);
				processInsn(forLoop.getIncrInsn(), usePlace);
			}
		}
	}

	void processInsn(InsnNode insn, UsePlace usePlace) {
		if (insn == null) {
			return;
		}
		// result
		RegisterArg result = insn.getResult();
		if (result != null && result.isRegister()) {
			if (!result.contains(AFlag.DONT_GENERATE)) {
				VarUsage usage = getUsage(result.getSVar());
				usage.getAssigns().add(usePlace);
			}
		}
		// args
		args.clear();
		insn.getRegisterArgs(args);
		for (RegisterArg arg : args) {
			if (!arg.contains(AFlag.DONT_GENERATE)) {
				VarUsage usage = getUsage(arg.getSVar());
				usage.getUses().add(usePlace);
			}
		}
	}

	private VarUsage getUsage(SSAVar ssaVar) {
		return usageMap.computeIfAbsent(ssaVar, VarUsage::new);
	}
}
