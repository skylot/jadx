package jadx.api;

public final class CodePosition {

	private final JavaNode node;
	private final int line;
	private final int offset;

	public CodePosition(JavaNode node, int line, int offset) {
		this.node = node;
		this.line = line;
		this.offset = offset;
	}

	public CodePosition(int line, int offset) {
		this.node = null;
		this.line = line;
		this.offset = offset;
	}

	public JavaNode getNode() {
		return node;
	}

	public JavaClass getJavaClass() {
		JavaClass parent = node.getDeclaringClass();
		if (parent == null && node instanceof JavaClass) {
			return (JavaClass) node;
		}
		return parent;
	}

	public int getLine() {
		return line;
	}

	public int getOffset() {
		return offset;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		CodePosition that = (CodePosition) o;
		return line == that.line && offset == that.offset;
	}

	@Override
	public int hashCode() {
		return line + 31 * offset;
	}

	@Override
	public String toString() {
		return line + ":" + offset + (node != null ? " " + node : "");
	}
}
