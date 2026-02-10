package jadx.core.dex.visitors.finaly.traverser.visitors;

import java.util.List;
import java.util.ListIterator;

import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.visitors.finaly.traverser.state.TraverserBlockInfo;
import jadx.core.dex.visitors.finaly.traverser.state.TraverserState;

public final class ImplicitInsnBlockTraverserVisitor extends AbstractBlockTraverserVisitor {

	public static boolean isInstructionImplicit(final InsnNode node) {
		// An instruction is implicit if it can be safely skipped for comparison when traversing in reverse
		// order.
		// The presence of a GOTO should be reflected in the structure of a block graph.
		// i.e. a GOTO should be the last instruction of a block with a single successor.
		// Another example might be NOP.
		final InsnType type = node.getType();
		switch (type) {
			case GOTO:
				return true;
			default:
				return false;
		}
	}

	public ImplicitInsnBlockTraverserVisitor(final TraverserState state) {
		super(state);
	}

	@Override
	public final TraverserState visit(final BlockNode block) {
		final TraverserBlockInfo insnInfo = getState().getBlockInsnInfo();

		final List<InsnNode> insns = insnInfo.getInsnsSlice();
		final ListIterator<InsnNode> insnsIterator = insns.listIterator(insns.size());

		/**
		 * The number of instructions that have been identified as "implicit" instructions.
		 */
		int bottomDelta = 0;
		while (insnsIterator.hasPrevious()) {
			final InsnNode insn = insnsIterator.previous();
			if (!isInstructionImplicit(insn)) {
				break;
			}
			bottomDelta++;
		}

		insnInfo.setBottomOffset(insnInfo.getBottomOffset() + bottomDelta);
		insnInfo.setBottomImplicitOffset(insnInfo.getBottomImplicitCount() + bottomDelta);
		return getState();
	}
}
