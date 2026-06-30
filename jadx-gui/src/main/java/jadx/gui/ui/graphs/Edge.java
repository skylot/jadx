package jadx.gui.ui.graphs;

class Edge {
	private final int source;
	private final int dest;

	public Edge(int source, int dest) {
		this.source = source;
		this.dest = dest;
	}

	public int getDest() {
		return dest;
	}

	public int getSource() {
		return source;
	}

	@Override
	public boolean equals(Object otherObject) {
		if (!(otherObject instanceof Edge)) {
			return false;
		}
		Edge other = (Edge) otherObject;
		return this.source == other.source && this.dest == other.dest;
	}

	@Override
	public int hashCode() {
		return source + 31 * dest;
	}
}
