package jadx.core.dex.nodes;

public class Edge {
	private final BlockNode source;
	private final BlockNode target;

	public Edge(BlockNode source, BlockNode target) {
		this.source = source;
		this.target = target;
	}

	public BlockNode getSource() {
		return source;
	}

	public BlockNode getTarget() {
		return target;
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
