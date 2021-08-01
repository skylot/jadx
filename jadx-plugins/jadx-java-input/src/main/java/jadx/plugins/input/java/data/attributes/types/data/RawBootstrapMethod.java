package jadx.plugins.input.java.data.attributes.types.data;

public class RawBootstrapMethod {
	private final int methodHandleIdx;
	private final int[] args;

	public RawBootstrapMethod(int methodHandleIdx, int[] args) {
		this.methodHandleIdx = methodHandleIdx;
		this.args = args;
	}

	public int getMethodHandleIdx() {
		return methodHandleIdx;
	}

	public int[] getArgs() {
		return args;
	}
}
