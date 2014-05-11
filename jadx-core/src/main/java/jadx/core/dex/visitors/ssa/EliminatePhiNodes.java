package jadx.core.dex.visitors.ssa;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.PhiListAttr;
import jadx.core.dex.instructions.PhiInsn;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.exceptions.JadxException;

import java.util.Iterator;
import java.util.List;

public class EliminatePhiNodes extends AbstractVisitor {
	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode()) {
			return;
		}
		enumerateSVars(mth);
		removePhiInstructions(mth);
	}

	public static void enumerateSVars(MethodNode mth) {
		for (SSAVar sVar : mth.getSVars()) {
			if (sVar.isUsedInPhi()) {
				sVar.mergeName(sVar.getUsedInPhi().getResult());
			}
		}
	}

//	public static void enumerateSVars(MethodNode mth) {
//		List<SSAVar> vars = mth.getSVars();
//		int varsSize = vars.size();
//		Deque<SSAVar> workList = new LinkedList<SSAVar>();
//		for (int i = 0; i < varsSize; i++) {
//			SSAVar ssaVar = vars.get(i);
//			ssaVar.setVarId(i);
//			if (ssaVar.isUsedInPhi()) {
//				workList.add(ssaVar);
//			}
//		}
//
//		int k = 0;
//		while (!workList.isEmpty()) {
//			SSAVar var = workList.pop();
//			RegisterArg assignVar = var.getUsedInPhi().getResult();
//			// set same name and variable ID
//			var.mergeName(assignVar);
//			SSAVar assignSVar = assignVar.getSVar();
//			int varId = assignSVar.getVarId();
//			var.setVarId(varId);
//
//			if (assignSVar.isUsedInPhi()) {
//				PhiInsn assignPhi = assignSVar.getUsedInPhi();
//				SSAVar asVar = assignPhi.getResult().getSVar();
//				if (asVar.getVarId() != varId) {
//					asVar.setVarId(varId);
//					for (int i = 0; i < assignPhi.getArgsCount(); i++) {
//						workList.push(assignPhi.getArg(i).getSVar());
//					}
//				}
//			}
//			if (k++ > 1000) {
//				throw new JadxRuntimeException("Can't calculate variable id");
//			}
//		}
//	}

	private static void removePhiInstructions(MethodNode mth) {
		for (BlockNode block : mth.getBasicBlocks()) {
			PhiListAttr phiList = block.get(AType.PHI_LIST);
			if (phiList == null) {
				continue;
			}
			List<PhiInsn> list = phiList.getList();
			for (PhiInsn phiInsn : list) {
				for (Iterator<InsnNode> iterator = block.getInstructions().iterator(); iterator.hasNext(); ) {
					InsnNode insn = iterator.next();
					if (insn == phiInsn) {
						iterator.remove();
					}
				}
			}
		}
	}
}
