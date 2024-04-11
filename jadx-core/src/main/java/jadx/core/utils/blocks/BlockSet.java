package jadx.core.utils.blocks;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.EmptyBitSet;

public class BlockSet {

	public static BlockSet from(MethodNode mth, Collection<BlockNode> blocks) {
		BlockSet newBS = new BlockSet(mth);
		newBS.set(blocks);
		return newBS;
	}

	private final MethodNode mth;
	private final BitSet bs;

	public BlockSet(MethodNode mth) {
		this.mth = mth;
		this.bs = new BitSet(mth.getBasicBlocks().size());
	}

	public boolean get(BlockNode block) {
		return bs.get(block.getId());
	}

	public void set(BlockNode block) {
		bs.set(block.getId());
	}

	public void set(Collection<BlockNode> blocks) {
		blocks.forEach(this::set);
	}

	public boolean checkAndSet(BlockNode block) {
		int id = block.getId();
		boolean state = bs.get(id);
		bs.set(id);
		return state;
	}

	public boolean intersects(List<BlockNode> blocks) {
		for (BlockNode block : blocks) {
			if (get(block)) {
				return true;
			}
		}
		return false;
	}

	public BlockSet intersect(List<BlockNode> blocks) {
		BlockSet input = from(mth, blocks);
		BlockSet result = new BlockSet(mth);
		BitSet resultBS = result.bs;
		resultBS.or(this.bs);
		resultBS.and(input.bs);
		return result;
	}

	public boolean isEmpty() {
		return bs.cardinality() == 0;
	}

	public int size() {
		return bs.cardinality();
	}

	public @Nullable BlockNode getOne() {
		if (bs.cardinality() == 1) {
			return mth.getBasicBlocks().get(bs.nextSetBit(0));
		}
		return null;
	}

	public void forEach(Consumer<? super BlockNode> consumer) {
		if (bs.isEmpty()) {
			return;
		}
		List<BlockNode> blocks = mth.getBasicBlocks();
		for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
			consumer.accept(blocks.get(i));
		}
	}

	public List<BlockNode> toList() {
		if (bs == null || bs == EmptyBitSet.EMPTY) {
			return Collections.emptyList();
		}
		int size = bs.cardinality();
		if (size == 0) {
			return Collections.emptyList();
		}
		List<BlockNode> mthBlocks = mth.getBasicBlocks();
		List<BlockNode> blocks = new ArrayList<>(size);
		for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
			blocks.add(mthBlocks.get(i));
		}
		return blocks;
	}

	@Override
	public String toString() {
		return toList().toString();
	}
}
