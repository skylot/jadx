package jadx.plugins.input.java.data.code;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jadx.api.plugins.input.insns.Opcode;
import jadx.plugins.input.java.data.DataReader;
import jadx.plugins.input.java.data.JavaClassData;
import jadx.plugins.input.java.data.code.StackState.SVType;

public class CodeDecodeState {
	private final JavaClassData clsData;
	private final DataReader reader;
	private final int maxStack;
	private final Set<Integer> excHandlers;

	private final Map<Integer, StackState> jumpStack = new HashMap<>(); // save current stack for jump target

	private JavaInsnData insn;
	private StackState stack;
	private boolean excHandler;

	public CodeDecodeState(JavaClassData clsData, DataReader reader, int maxStack, Set<Integer> excHandlers) {
		this.clsData = clsData;
		this.reader = reader;
		this.maxStack = maxStack;
		this.excHandlers = excHandlers;
		this.stack = new StackState(maxStack);
	}

	public void onInsn(int offset) {
		StackState stackState = jumpStack.get(offset);
		if (stackState != null) {
			this.stack = stackState;
		}
		if (excHandlers.contains(offset)) {
			stack.push(SVType.NARROW); // push exception
			excHandler = true;
		} else {
			excHandler = false;
		}
	}

	public void registerJump(int jumpOffset) {
		Integer key = jumpOffset;
		if (!jumpStack.containsKey(key)) {
			jumpStack.put(key, stack.copy());
		}
	}

	public void decoded() {
		if (excHandler && insn.getOpcode() == Opcode.MOVE) {
			// replace first 'move' in exception handler with 'move-exception'
			insn.setOpcode(Opcode.MOVE_EXCEPTION);
			insn.setRegsCount(1);
		}
	}

	public JavaInsnData insn() {
		return insn;
	}

	public void setInsn(JavaInsnData insn) {
		this.insn = insn;
	}

	public DataReader reader() {
		return reader;
	}

	public JavaClassData clsData() {
		return clsData;
	}

	public CodeDecodeState local(int arg, int local) {
		insn.setArgReg(arg, localToReg(local));
		return this;
	}

	public CodeDecodeState pop(int arg) {
		insn.setArgReg(arg, stack.pop());
		return this;
	}

	public CodeDecodeState peek(int arg) {
		insn.setArgReg(arg, stack.peek());
		return this;
	}

	public SVType peekType(int at) {
		return stack.peekTypeAt(at);
	}

	public CodeDecodeState peekFrom(int pos, int arg) {
		insn.setArgReg(arg, stack.peekAt(pos));
		return this;
	}

	public CodeDecodeState push(int arg) {
		insn.setArgReg(arg, stack.push(SVType.NARROW));
		return this;
	}

	public CodeDecodeState push(int arg, SVType type) {
		insn.setArgReg(arg, stack.push(type));
		return this;
	}

	public CodeDecodeState pushWide(int arg) {
		insn.setArgReg(arg, stack.push(SVType.WIDE));
		return this;
	}

	public void discard() {
		stack.pop();
	}

	public void discardWord() {
		SVType type = stack.peekTypeAt(0);
		stack.pop();
		if (type == SVType.NARROW) {
			stack.pop();
		}
	}

	public void clear() {
		stack.clear();
	}

	public int push(String type) {
		return stack.push(getSVType(type));
	}

	/**
	 * Must be after all pop and push
	 */
	public void jump(int offset) {
		int jumpOffset = insn.getOffset() + offset;
		insn.setTarget(jumpOffset);
		registerJump(jumpOffset);
	}

	public CodeDecodeState idx(int idx) {
		insn.setIndex(idx);
		return this;
	}

	public CodeDecodeState lit(long lit) {
		insn.setLiteral(lit);
		return this;
	}

	private int localToReg(int local) {
		return maxStack + local;
	}

	public SVType fieldType() {
		String type = insn.constPoolReader().getFieldType(insn().getIndex());
		return getSVType(type);
	}

	public SVType getSVType(String type) {
		if (type.equals("J") || type.equals("D")) {
			return SVType.WIDE;
		}
		return SVType.NARROW;
	}

	public int u1() {
		return reader.readU1();
	}

	public int u2() {
		return reader.readU2();
	}

	public int s1() {
		return reader.readS1();
	}

	public int s2() {
		return reader.readS2();
	}
}
