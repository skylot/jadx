package jadx.core.dex.visitors.typeresolver;

import jadx.core.dex.attributes.BlockRegState;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.AbstractVisitor;

import java.util.List;

public class TypeResolver extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}
		visitBlocks(mth);
		visitEdges(mth);

		// clear register states
		for (BlockNode block : mth.getBasicBlocks()) {
			block.setStartState(null);
			block.setEndState(null);
		}
	}

	private static void visitBlocks(MethodNode mth) {
		for (BlockNode block : mth.getBasicBlocks()) {
			BlockRegState state = new BlockRegState(mth);

			if (block == mth.getEnterBlock()) {
				for (RegisterArg arg : mth.getArguments(true)) {
					state.assignReg(arg);
				}
			}
			block.setStartState(new BlockRegState(state));

			for (InsnNode insn : block.getInstructions()) {
				for (InsnArg arg : insn.getArguments()) {
					if (arg.isRegister()) {
						state.use((RegisterArg) arg);
					}
				}
				if (insn.getResult() != null) {
					state.assignReg(insn.getResult());
				}
			}

			block.setEndState(new BlockRegState(state));
		}
	}

	private void visitEdges(MethodNode mth) {
		List<BlockNode> preds = mth.getBasicBlocks();
		boolean changed;
		do {
			changed = false;
			for (BlockNode block : preds) {
				for (BlockNode pred : block.getPredecessors()) {
					if (connectEdges(mth, pred, block, true)) {
						changed = true;
					}
				}
			}
		} while (changed);

		for (BlockNode block : mth.getBasicBlocks()) {
			for (BlockNode dest : block.getSuccessors()) {
				connectEdges(mth, block, dest, false);
			}
		}
	}

	private static boolean connectEdges(MethodNode mth, BlockNode from, BlockNode to, boolean back) {
		BlockRegState end = from.getEndState();
		BlockRegState start = to.getStartState();

		boolean changed = false;
		for (int r = 0; r < mth.getRegsCount(); r++) {
			RegisterArg sr = start.getRegister(r);
			RegisterArg er = end.getRegister(r);

			if (back) {
				if (er.getTypedVar() == null && sr.getTypedVar() != null) {
					er.replaceTypedVar(sr);
					changed = true;
				}
			} else {
				if (sr.getTypedVar() != null && er.getTypedVar() != null) {
					sr.replaceTypedVar(er);
					changed = true;
				}
			}
		}
		return changed;
	}
}
