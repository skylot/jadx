package jadx.core.dex.nodes;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AttrNode;

public class Edge extends AttrNode {
	private final BlockNode source;
	private final BlockNode target;

	public Edge(BlockNode source, BlockNode target) {
		this(source, target, false);
	}

	public Edge(BlockNode source, BlockNode target, boolean isSynthetic) {
		if (isSynthetic) {
			this.add(AFlag.SYNTHETIC);
		}

		this.source = source;
		this.target = target;

	}

	public BlockNode getSource() {
		return source;
	}

	public BlockNode getTarget() {
		return target;
	}

	public boolean isSynthetic() {
		return this.contains(AFlag.SYNTHETIC);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Edge edge = (Edge) o;
		return source.equals(edge.source) && target.equals(edge.target);
	}

	@Override
	public int hashCode() {
		return source.hashCode() + 31 * target.hashCode();
	}

	@Override
	public String toString() {
		return "Edge: " + source + " -> " + target;
	}
}
