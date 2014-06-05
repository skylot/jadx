package jadx.core.dex.visitors.ssa;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.PhiListAttr;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.PhiInsn;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.InstructionRemover;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.exceptions.JadxRuntimeException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class SSATransform extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode()) {
			return;
		}
		process(mth);
	}

	private void process(MethodNode mth) {
		LiveVarAnalysis la = new LiveVarAnalysis(mth);
		la.runAnalysis();
		for (int i = 0; i < mth.getRegsCount(); i++) {
			placePhi(mth, i, la);
		}
		renameVariables(mth);
		removeUselessPhi(mth);
	}

	private void placePhi(MethodNode mth, int regNum, LiveVarAnalysis la) {
		List<BlockNode> blocks = mth.getBasicBlocks();
		int blocksCount = blocks.size();
		BitSet hasPhi = new BitSet(blocksCount);
		BitSet processed = new BitSet(blocksCount);
		Deque<BlockNode> workList = new LinkedList<BlockNode>();

		BitSet assignBlocks = la.getAssignBlocks(regNum);
		for (int id = assignBlocks.nextSetBit(0); id >= 0; id = assignBlocks.nextSetBit(id + 1)) {
			processed.set(id);
			workList.add(blocks.get(id));
		}
		while (!workList.isEmpty()) {
			BlockNode block = workList.pop();
			BitSet domFrontier = block.getDomFrontier();
			for (int id = domFrontier.nextSetBit(0); id >= 0; id = domFrontier.nextSetBit(id + 1)) {
				if (!hasPhi.get(id) && la.isLive(id, regNum)) {
					BlockNode df = blocks.get(id);
					addPhi(df, regNum);
					hasPhi.set(id);
					if (!processed.get(id)) {
						processed.set(id);
						workList.add(df);
					}
				}
			}
		}
	}

	private void addPhi(BlockNode block, int regNum) {
		PhiListAttr phiList = block.get(AType.PHI_LIST);
		if (phiList == null) {
			phiList = new PhiListAttr();
			block.addAttr(phiList);
		}
		PhiInsn phiInsn = new PhiInsn(regNum, block.getPredecessors().size());
		phiList.getList().add(phiInsn);
		block.getInstructions().add(0, phiInsn);
	}

	private void renameVariables(MethodNode mth) {
		int regsCount = mth.getRegsCount();
		SSAVar[] vars = new SSAVar[regsCount];
		int[] versions = new int[regsCount];
		// init method arguments
		for (RegisterArg arg : mth.getArguments(true)) {
			int regNum = arg.getRegNum();
			vars[regNum] = mth.makeNewSVar(regNum, versions, arg);
		}
		renameVar(mth, vars, versions, mth.getEnterBlock());
	}

	private void renameVar(MethodNode mth, SSAVar[] vars, int[] vers, BlockNode block) {
		SSAVar[] inputVars = Arrays.copyOf(vars, vars.length);
		for (InsnNode insn : block.getInstructions()) {
			if (insn.getType() != InsnType.PHI) {
				for (InsnArg arg : insn.getArguments()) {
					if (arg.isRegister()) {
						RegisterArg reg = (RegisterArg) arg;
						int regNum = reg.getRegNum();
						SSAVar var = vars[regNum];
						if (var == null) {
							var = mth.makeNewSVar(regNum, vers, null);
							vars[regNum] = var;
						}
						var.use(reg);
					}
				}
			}
			RegisterArg result = insn.getResult();
			if (result != null) {
				int regNum = result.getRegNum();
				vars[regNum] = mth.makeNewSVar(regNum, vers, result);
			}
		}
		for (BlockNode s : block.getSuccessors()) {
			PhiListAttr phiList = s.get(AType.PHI_LIST);
			if (phiList != null) {
				int j = s.getPredecessors().indexOf(block);
				if (j == -1) {
					throw new JadxRuntimeException("Can't find predecessor for " + block + " " + s);
				}
				for (PhiInsn phiInsn : phiList.getList()) {
					int regNum = phiInsn.getResult().getRegNum();
					SSAVar var = vars[regNum];
					if (var == null) {
						var = mth.makeNewSVar(regNum, vers, null);
						vars[regNum] = var;
					}
					var.use(phiInsn.getArg(j));
					var.setUsedInPhi(phiInsn);
				}
			}
		}
		for (BlockNode domOn : block.getDominatesOn()) {
			renameVar(mth, vars, vers, domOn);
		}
		System.arraycopy(inputVars, 0, vars, 0, vars.length);
	}

	private void removeUselessPhi(MethodNode mth) {
		List<PhiInsn> insnToRemove = new ArrayList<PhiInsn>();
		for (SSAVar var : mth.getSVars()) {
			// phi result not used
			if (var.getUseCount() == 0) {
				InsnNode assignInsn = var.getAssign().getParentInsn();
				if (assignInsn != null && assignInsn.getType() == InsnType.PHI) {
					insnToRemove.add((PhiInsn) assignInsn);
				}
			}
		}
		for (BlockNode block : mth.getBasicBlocks()) {
			PhiListAttr phiList = block.get(AType.PHI_LIST);
			if (phiList == null) {
				continue;
			}
			for (PhiInsn phi : phiList.getList()) {
				removePhiWithSameArgs(phi, insnToRemove);
			}
		}
		removePhiList(mth, insnToRemove);
	}

	private void removePhiWithSameArgs(PhiInsn phi, List<PhiInsn> insnToRemove) {
		if (phi.getArgsCount() <= 1) {
			insnToRemove.add(phi);
			return;
		}
		boolean allSame = true;
		SSAVar var = phi.getArg(0).getSVar();
		for (int i = 1; i < phi.getArgsCount(); i++) {
			if (var != phi.getArg(i).getSVar()) {
				allSame = false;
				break;
			}
		}
		if (allSame) {
			// replace
			insnToRemove.add(phi);
			SSAVar assign = phi.getResult().getSVar();
			for (RegisterArg arg : new ArrayList<RegisterArg>(assign.getUseList())) {
				assign.removeUse(arg);
				var.use(arg);
			}
		}
	}

	private void removePhiList(MethodNode mth, List<PhiInsn> insnToRemove) {
		if (insnToRemove.isEmpty()) {
			return;
		}
		for (BlockNode block : mth.getBasicBlocks()) {
			PhiListAttr phiList = block.get(AType.PHI_LIST);
			if (phiList == null) {
				continue;
			}
			List<PhiInsn> list = phiList.getList();
			for (PhiInsn phiInsn : insnToRemove) {
				if (list.remove(phiInsn)) {
					for (InsnArg arg : phiInsn.getArguments()) {
						((RegisterArg) arg).getSVar().setUsedInPhi(null);
					}
					InstructionRemover.remove(mth, block, phiInsn);
				}
			}
			if (list.isEmpty()) {
				block.remove(AType.PHI_LIST);
			}
		}
		insnToRemove.clear();
	}
}
