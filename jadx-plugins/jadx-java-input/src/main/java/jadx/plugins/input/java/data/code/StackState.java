package jadx.plugins.input.java.data.code;

import java.util.Arrays;

public class StackState {

	/**
	 * Stack value type
	 */
	public enum SVType {
		NARROW, // int, float, etc
		WIDE, // long, double
	}

	private int pos = -1;
	private final SVType[] stack;

	public StackState(int maxStack) {
		this.stack = new SVType[maxStack];
	}

	private StackState(int pos, SVType[] stack) {
		this.pos = pos;
		this.stack = stack;
	}

	public StackState copy() {
		return new StackState(pos, Arrays.copyOf(stack, stack.length));
	}

	public int peek() {
		return pos;
	}

	public int peekAt(int at) {
		return pos - at;
	}

	public SVType peekTypeAt(int at) {
		int p = pos - at;
		if (checkStackIndex(p)) {
			return stack[p];
		}
		return SVType.NARROW;
	}

	public int push(SVType type) {
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
