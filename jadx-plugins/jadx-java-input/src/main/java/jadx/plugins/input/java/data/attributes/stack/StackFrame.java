package jadx.plugins.input.java.data.attributes.stack;

public class StackFrame {
	private final int offset;
	private final StackFrameType type;

	private int stackSize;
	private StackValueType[] stackValueTypes;
	private int localsCount;

	public StackFrame(int offset, StackFrameType type) {
		this.offset = offset;
		this.type = type;
	}

	public int getOffset() {
		return offset;
	}

	public StackFrameType getType() {
		return type;
	}

	public int getLocalsCount() {
		return localsCount;
	}

	public void setLocalsCount(int localsCount) {
		this.localsCount = localsCount;
	}

	public int getStackSize() {
		return stackSize;
	}

	public void setStackSize(int stackSize) {
		this.stackSize = stackSize;
	}

	public StackValueType[] getStackValueTypes() {
		return stackValueTypes;
	}

	public void setStackValueTypes(StackValueType[] stackValueTypes) {
		this.stackValueTypes = stackValueTypes;
	}
}
