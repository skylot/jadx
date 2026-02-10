package jadx.core.dex.visitors.finaly.traverser.state;

import java.util.List;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;

public final class TraverserBlockInfo {

	private final BlockNode block;

	// These offsets are the instruction indices NOT an instruction size.
	private int bottomOffset;
	private int topOffset;
	private int bottomImplicitCount;

	public TraverserBlockInfo(final BlockNode block) {
		this(block, 0, 0, 0);
	}

	public TraverserBlockInfo(final BlockNode block, final int bottomOffset, final int topOffset, final int bottomImplicitCount) {
		this.bottomOffset = bottomOffset;
		this.topOffset = topOffset;
		this.block = block;
		this.bottomImplicitCount = bottomImplicitCount;
	}

	@Override
	public final String toString() {
		return toString("");
	}

	public final String toString(final String indent) {
		final StringBuilder sb = new StringBuilder("BlockInsnInfo - ");

		sb.append(block.toString());
		sb.append(" [↑ ");
		sb.append(bottomOffset);
		sb.append("] [↓ ");
		sb.append(topOffset);
		sb.append("] ");

		return sb.toString();
	}

	public final TraverserBlockInfo duplicate() {
		return new TraverserBlockInfo(block, bottomOffset, topOffset, bottomImplicitCount);
	}

	public final BlockNode getBlock() {
		return block;
	}

	public final int getTopOffset() {
		return topOffset;
	}

	public final void setTopOffset(final int topOffset) {
		this.topOffset = topOffset;
	}

	public final int getBottomOffset() {
		return bottomOffset;
	}

	public final void setBottomOffset(final int bottomOffset) {
		this.bottomOffset = bottomOffset;
	}

	public final int getBottomImplicitCount() {
		return bottomImplicitCount;
	}

	public final void setBottomImplicitOffset(final int bottomImplicitCount) {
		this.bottomImplicitCount = bottomImplicitCount;
	}

	public final List<InsnNode> getInsnsSlice() {
		final List<InsnNode> insns = block.getInstructions();

		final int totalSkippedCount = bottomOffset + topOffset;
		if (totalSkippedCount > insns.size()) {
			throw new IndexOutOfBoundsException("Attempted to get instructions slice of block " + block.toString() + " with "
					+ totalSkippedCount + " skipped instructions whilst only having " + insns.size() + " instructions in block.");
		}

		final int startIndex = topOffset;
		final int endIndex = insns.size() - bottomOffset;

		return insns.subList(startIndex, endIndex);
	}
}
