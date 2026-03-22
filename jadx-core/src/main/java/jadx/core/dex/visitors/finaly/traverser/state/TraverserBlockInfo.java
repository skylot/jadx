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

	public TraverserBlockInfo(BlockNode block) {
		this(block, 0, 0, 0);
	}

	public TraverserBlockInfo(BlockNode block, int bottomOffset, int topOffset, int bottomImplicitCount) {
		this.bottomOffset = bottomOffset;
		this.topOffset = topOffset;
		this.block = block;
		this.bottomImplicitCount = bottomImplicitCount;
	}

	@Override
	public String toString() {
		return toString("");
	}

	public String toString(String indent) {
		StringBuilder sb = new StringBuilder("BlockInsnInfo - ");
		sb.append(block.toString());
		sb.append(" [↑ ");
		sb.append(bottomOffset);
		sb.append("] [↓ ");
		sb.append(topOffset);
		sb.append("] ");
		return sb.toString();
	}

	public TraverserBlockInfo duplicate() {
		return new TraverserBlockInfo(block, bottomOffset, topOffset, bottomImplicitCount);
	}

	public BlockNode getBlock() {
		return block;
	}

	public int getTopOffset() {
		return topOffset;
	}

	public void setTopOffset(int topOffset) {
		this.topOffset = topOffset;
	}

	public int getBottomOffset() {
		return bottomOffset;
	}

	public void setBottomOffset(int bottomOffset) {
		this.bottomOffset = bottomOffset;
	}

	public int getBottomImplicitCount() {
		return bottomImplicitCount;
	}

	public void setBottomImplicitOffset(int bottomImplicitCount) {
		this.bottomImplicitCount = bottomImplicitCount;
	}

	public List<InsnNode> getInsnsSlice() {
		List<InsnNode> insns = block.getInstructions();
		int totalSkippedCount = bottomOffset + topOffset;
		if (totalSkippedCount > insns.size()) {
			throw new IndexOutOfBoundsException("Attempted to get instructions slice of block " + block.toString() + " with "
					+ totalSkippedCount + " skipped instructions whilst only having " + insns.size() + " instructions in block.");
		}
		int startIndex = topOffset;
		int endIndex = insns.size() - bottomOffset;
		return insns.subList(startIndex, endIndex);
	}
}
