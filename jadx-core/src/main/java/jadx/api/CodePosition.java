package jadx.api;

public final class CodePosition {

	private final JavaClass cls;
	private final int line;
	private final int offset;

	public CodePosition(JavaClass cls, int line, int offset) {
		this.cls = cls;
		this.line = line;
		this.offset = offset;
	}

	public CodePosition(int line, int offset) {
		this.cls = null;
		this.line = line;
		this.offset = offset;
	}

	public JavaClass getJavaClass() {
		return cls;
	}

	public int getLine() {
		return line;
	}

	public int getOffset() {
		return offset;
	}

	public boolean isSet() {
		return line != 0 || offset != 0;
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
		return line + ":" + offset + (cls != null ? " " + cls : "");
	}
}
