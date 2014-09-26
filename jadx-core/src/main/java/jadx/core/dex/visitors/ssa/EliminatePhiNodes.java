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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EliminatePhiNodes extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(EliminatePhiNodes.class);

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
}
