package jadx.plugins.input.java.data.code;

import java.util.Arrays;

import jadx.plugins.input.java.data.attributes.stack.StackFrame;
import jadx.plugins.input.java.data.attributes.stack.StackValueType;

public class StackState {
	private int pos = -1;
	private final StackValueType[] stack;

	public StackState(int maxStack) {
		this.stack = new StackValueType[maxStack];
	}

	private StackState(int pos, StackValueType[] stack) {
		this.pos = pos;
		this.stack = stack;
	}

	public StackState copy() {
		return new StackState(pos, Arrays.copyOf(stack, stack.length));
	}

	public StackState fillFromFrame(StackFrame frame) {
		int stackSize = frame.getStackSize();
		this.pos = stackSize - 1;
		if (stackSize > 0) {
			System.arraycopy(frame.getStackValueTypes(), 0, this.stack, 0, stackSize);
		}
		return this;
	}

	public int peek() {
		return pos;
	}

	public int peekAt(int at) {
		return pos - at;
	}

	public StackValueType peekTypeAt(int at) {
		int p = pos - at;
		if (checkStackIndex(p)) {
			return stack[p];
		}
		return StackValueType.NARROW;
	}

	public int insert(int at, StackValueType type) {
		int p = pos - at;
		System.arraycopy(stack, p, stack, p + 1, at);
		stack[p] = type;
		pos++;
		return p;
	}

	public int push(StackValueType type) {
		int p = ++pos;
		if (checkStackIndex(p)) {
			stack[p] = type;
		}
		return p;
	}

	private boolean checkStackIndex(int p) {
		return p >= 0 && p < stack.length;
	}

	public int pop() {
		return pos--;
	}

	public void clear() {
		pos = -1;
	}

	@Override
	public String toString() {
		int size = pos + 1;
		String arr;
		if (size == 0) {
			arr = "empty";
		} else if (size > 0 && size < stack.length) {
			arr = Arrays.toString(Arrays.copyOf(stack, size));
		} else {
			arr = Arrays.toString(stack) + " (max)";
		}
		return "Stack: " + size + ": " + arr;
	}
}
