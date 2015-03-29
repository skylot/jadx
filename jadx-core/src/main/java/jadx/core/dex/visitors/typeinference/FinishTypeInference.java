package jadx.core.dex.visitors.typeinference;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.AbstractVisitor;

public class FinishTypeInference extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}

		boolean change;
		int i = 0;
		do {
			change = false;
			for (BlockNode block : mth.getBasicBlocks()) {
				for (InsnNode insn : block.getInstructions()) {
					if (PostTypeInference.process(mth, insn)) {
						change = true;
					}
				}
			}
			i++;
			if (i > 1000) {
				break;
			}
		} while (change);

		// last chance to set correct value (just use first type from 'possible' list)
		DexNode dex = mth.dex();
		for (BlockNode block : mth.getBasicBlocks()) {
			for (InsnNode insn : block.getInstructions()) {
				SelectTypeVisitor.visit(dex, insn);
			}
		}

		// check
		for (BlockNode block : mth.getBasicBlocks()) {
			for (InsnNode insn : block.getInstructions()) {
				CheckTypeVisitor.visit(mth, insn);
			}
		}
	}
}
