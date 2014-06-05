package jadx.core.dex.visitors.ssa;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.PhiListAttr;
import jadx.core.dex.instructions.PhiInsn;
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
