package jadx.core.utils;

import java.util.Objects;

import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IContainer;

public class BlockParentContainer {

	private final IContainer parent;
	private final IBlock block;

	public BlockParentContainer(IContainer parent, IBlock block) {
		this.parent = Objects.requireNonNull(parent);
		this.block = Objects.requireNonNull(block);
	}

	public IBlock getBlock() {
		return block;
	}

	public IContainer getParent() {
		return parent;
	}

	@Override
	public String toString() {
		return "BlockParentContainer{" + block + ", parent=" + parent + '}';
	}
}
