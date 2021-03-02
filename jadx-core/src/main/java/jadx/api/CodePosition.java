package jadx.api;

public final class CodePosition {

	private final int line;
	private final int offset;
	private final int pos;

	public CodePosition(int line, int offset, int pos) {
		this.line = line;
		this.offset = offset;
		this.pos = pos;
	}

	public CodePosition(int line) {
		this(line, 0, -1);
	}

	@Deprecated
	public CodePosition(int line, int offset) {
		this(line, offset, -1);
	}

	public int getPos() {
		return pos;
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
		StringBuilder sb = new StringBuilder();
		sb.append(line);
		if (offset != 0) {
			sb.append(':').append(offset);
		}
		if (pos > 0) {
			sb.append('@').append(pos);
		}
		return sb.toString();
	}
}
