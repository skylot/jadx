package jadx.core.utils.blocks;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.EmptyBitSet;

/**
 * BlockNode set implementation based on BitSet.
 */
public class BlockSet implements Iterable<BlockNode> {

	public static BlockSet empty(MethodNode mth) {
		return new BlockSet(mth);
	}

	public static BlockSet from(MethodNode mth, Collection<BlockNode> blocks) {
		BlockSet newBS = new BlockSet(mth);
		newBS.addAll(blocks);
		return newBS;
	}

	private final MethodNode mth;
	private final BitSet bs;

	public BlockSet(MethodNode mth) {
		this.mth = mth;
		this.bs = new BitSet(mth.getBasicBlocks().size());
	}

	public boolean contains(BlockNode block) {
		return bs.get(block.getPos());
	}

	public void add(BlockNode block) {
		bs.set(block.getPos());
	}

	public void addAll(Collection<BlockNode> blocks) {
		blocks.forEach(this::add);
	}

	public void addAll(BlockSet otherBlockSet) {
		bs.or(otherBlockSet.bs);
	}

	public void remove(BlockNode block) {
		bs.clear(block.getPos());
	}

	public void remove(Collection<BlockNode> blocks) {
		blocks.forEach(this::remove);
	}

	public boolean addChecked(BlockNode block) {
		int id = block.getPos();
		boolean state = bs.get(id);
		bs.set(id);
		return state;
	}

	public boolean containsAll(List<BlockNode> blocks) {
		for (BlockNode block : blocks) {
			if (!contains(block)) {
				return false;
			}
		}
		return true;
	}

	public boolean intersects(List<BlockNode> blocks) {
		for (BlockNode block : blocks) {
			if (contains(block)) {
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
		return bs.isEmpty();
	}

	public int size() {
		return bs.cardinality();
	}

	public void remove() {
		bs.clear();
	}

	public @Nullable BlockNode getOne() {
		if (bs.cardinality() == 1) {
			return mth.getBasicBlocks().get(bs.nextSetBit(0));
		}
		return null;
	}

	public BlockNode getFirst() {
		return mth.getBasicBlocks().get(bs.nextSetBit(0));
	}

	@Override
	public void forEach(Consumer<? super BlockNode> consumer) {
		if (bs.isEmpty()) {
			return;
		}
		List<BlockNode> blocks = mth.getBasicBlocks();
		for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
			consumer.accept(blocks.get(i));
		}
	}

	@Override
	public @NotNull Iterator<BlockNode> iterator() {
		return new BlockSetIterator(bs, size(), mth.getBasicBlocks());
	}

	@Override
	public Spliterator<BlockNode> spliterator() {
		int size = size();
		BlockSetIterator iterator = new BlockSetIterator(bs, size, mth.getBasicBlocks());
		return Spliterators.spliterator(iterator, size, Spliterator.ORDERED | Spliterator.DISTINCT);
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

	private static final class BlockSetIterator implements Iterator<BlockNode> {
		private final BitSet bs;
		private final int size;
		private final List<BlockNode> blocks;

		private int cursor;
		private int start;

		public BlockSetIterator(BitSet bs, int size, List<BlockNode> blocks) {
			this.bs = bs;
			this.size = size;
			this.blocks = blocks;
		}

		@Override
		public boolean hasNext() {
			return cursor != size;
		}

		@Override
		public BlockNode next() {
			int pos = bs.nextSetBit(start);
			if (pos == -1) {
				throw new NoSuchElementException();
			}
			start = pos + 1;
			cursor++;
			return blocks.get(pos);
		}
	}
}
