package jadx.core.dex.visitors.ssa;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.PhiListAttr;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.PhiInsn;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.exceptions.JadxRuntimeException;

import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EliminatePhiNodes extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(EliminatePhiNodes.class);

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode()) {
			return;
		}
		replaceMergeInstructions(mth);
		removePhiInstructions(mth);
	}

	private static void removePhiInstructions(MethodNode mth) {
		for (BlockNode block : mth.getBasicBlocks()) {
			PhiListAttr phiList = block.get(AType.PHI_LIST);
			if (phiList == null) {
				continue;
			}
			List<PhiInsn> list = phiList.getList();
			for (PhiInsn phiInsn : list) {
				removeInsn(mth, block, phiInsn);
			}
		}
	}

	private static void removeInsn(MethodNode mth, BlockNode block, PhiInsn phiInsn) {
		Iterator<InsnNode> it = block.getInstructions().iterator();
		while (it.hasNext()) {
			InsnNode insn = it.next();
			if (insn == phiInsn) {
				it.remove();
				return;
			}
		}
		LOG.warn("Phi node not removed: {}, mth: {}", phiInsn, mth);
	}

	private void replaceMergeInstructions(MethodNode mth) {
		for (BlockNode block : mth.getBasicBlocks()) {
			List<InsnNode> insns = block.getInstructions();
			if (insns.isEmpty()) {
				continue;
			}
			InsnNode insn = insns.get(0);
			if (insn.getType() == InsnType.MERGE) {
				replaceMerge(mth, block, insn);
			}
		}
	}

	/**
	 * Replace 'MERGE' with 'PHI' insn.
	 */
	private void replaceMerge(MethodNode mth, BlockNode block, InsnNode insn) {
		if (insn.getArgsCount() != 2) {
			throw new JadxRuntimeException("Unexpected count of arguments in merge insn: " + insn);
		}
		RegisterArg oldArg = (RegisterArg) insn.getArg(1);
		RegisterArg newArg = (RegisterArg) insn.getArg(0);
		int newRegNum = newArg.getRegNum();
		if (oldArg.getRegNum() == newRegNum) {
			throw new JadxRuntimeException("Unexpected register number in merge insn: " + insn);
		}
		SSAVar oldSVar = oldArg.getSVar();
		RegisterArg assignArg = oldSVar.getAssign();

		InsnNode assignParentInsn = assignArg.getParentInsn();
		BlockNode assignBlock = BlockUtils.getBlockByInsn(mth, assignParentInsn);
		if (assignBlock == null) {
			throw new JadxRuntimeException("Unknown assign block for " + assignParentInsn);
		}
		BlockNode assignPred = null;
		for (BlockNode pred : block.getPredecessors()) {
			if (BlockUtils.isPathExists(assignBlock, pred)) {
				assignPred = pred;
				break;
			}
		}
		if (assignPred == null) {
			throw new JadxRuntimeException("Assign predecessor not found for " + assignBlock + " from " + block);
		}

		// all checks passed
		RegisterArg newAssignArg = oldArg.duplicate(newRegNum, null);
		SSAVar newSVar = mth.makeNewSVar(newRegNum, mth.getNextSVarVersion(newRegNum), newAssignArg);
		newSVar.setName(oldSVar.getName());
		newSVar.setType(assignArg.getType());

		if (assignParentInsn != null) {
			assignParentInsn.setResult(newAssignArg);
		}
		for (RegisterArg useArg : oldSVar.getUseList()) {
			RegisterArg newUseArg = useArg.duplicate(newRegNum, newSVar);
			InsnNode parentInsn = useArg.getParentInsn();
			if (parentInsn != null) {
				newSVar.use(newUseArg);
				parentInsn.replaceArg(useArg, newUseArg);
			}
		}
		block.getInstructions().remove(0);
		PhiInsn phiInsn = SSATransform.addPhi(mth, block, newRegNum);
		phiInsn.setResult(insn.getResult());

		phiInsn.bindArg(newAssignArg.duplicate(), assignPred);
		phiInsn.bindArg(newArg.duplicate(),
				BlockUtils.selectOtherSafe(assignPred, block.getPredecessors()));
	}
}
