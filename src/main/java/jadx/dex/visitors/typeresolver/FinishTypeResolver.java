package jadx.dex.visitors.typeresolver;

import jadx.dex.nodes.BlockNode;
import jadx.dex.nodes.InsnNode;
import jadx.dex.nodes.MethodNode;
import jadx.dex.visitors.AbstractVisitor;
import jadx.dex.visitors.typeresolver.finish.CheckTypeVisitor;
import jadx.dex.visitors.typeresolver.finish.PostTypeResolver;
import jadx.dex.visitors.typeresolver.finish.SelectTypeVisitor;

public class FinishTypeResolver extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode())
			return;

		boolean change;
		int i = 0;
		do {
			change = false;
			for (BlockNode block : mth.getBasicBlocks())
				for (InsnNode insn : block.getInstructions())
					if (PostTypeResolver.visit(insn))
						change = true;

			i++;
			if (i > 1000)
				break;
		} while (change);

		// last chance to set correct value (just use first type from 'possible' list)
		for (BlockNode block : mth.getBasicBlocks())
			for (InsnNode insn : block.getInstructions())
				SelectTypeVisitor.visit(insn);

		// check
		for (BlockNode block : mth.getBasicBlocks())
			for (InsnNode insn : block.getInstructions())
				CheckTypeVisitor.visit(mth, insn);
	}
}
