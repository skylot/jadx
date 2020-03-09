package jadx.core.utils;

import java.util.Objects;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;

public class BlockInsnPair {
	private final BlockNode block;
	private final InsnNode insn;

	public BlockInsnPair(BlockNode block, InsnNode insn) {
		this.block = block;
		this.insn = insn;
	}

	public BlockNode getBlock() {
		return block;
	}

	public InsnNode getInsn() {
		return insn;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof BlockInsnPair)) {
			return false;
		}
		BlockInsnPair that = (BlockInsnPair) o;
		return block.equals(that.block) && insn.equals(that.insn);
	}

	@Override
	public int hashCode() {
		return Objects.hash(block, insn);
	}

	@Override
	public String toString() {
		return "BlockInsnPair{" + block + ": " + insn + '}';
	}
}
