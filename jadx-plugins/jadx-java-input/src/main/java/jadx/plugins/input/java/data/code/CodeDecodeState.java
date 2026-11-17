package jadx.plugins.input.java.data.code;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.insns.InsnData;
import jadx.api.plugins.input.insns.Opcode;
import jadx.core.utils.Utils;
import jadx.plugins.input.java.data.DataReader;
import jadx.plugins.input.java.data.JavaClassData;
import jadx.plugins.input.java.data.attributes.stack.StackFrame;
import jadx.plugins.input.java.data.attributes.stack.StackValueType;
import jadx.plugins.input.java.data.attributes.types.StackMapTableAttr;

@SuppressWarnings("UnusedReturnValue")
public class CodeDecodeState {
	private final JavaClassData clsData;
	private final DataReader reader;
	private final int maxStack;
	private final Set<Integer> excHandlers;
	private final StackMapTableAttr stackMapTable;
	private final Map<Integer, StackState> jumpStack = new HashMap<>(); // save current stack for jump target

	private @Nullable Supplier<Map<Integer, StackState>> jumpStackCollector;
	private @Nullable Map<Integer, StackState> collectedJumpStack;
	private @Nullable Consumer<Integer> newJumpListener;

	private JavaInsnData insn;
	private StackState stack;
	private boolean excHandler;
	private boolean fallThrough = true;

	public CodeDecodeState(JavaClassData clsData, DataReader reader, int maxStack,
			Set<Integer> excHandlers, @Nullable StackMapTableAttr stackMapTable) {
		this.clsData = clsData;
		this.reader = reader;
		this.maxStack = maxStack;
		this.excHandlers = excHandlers;
		this.stack = new StackState(maxStack);
		this.stackMapTable = Utils.getOrElse(stackMapTable, StackMapTableAttr.EMPTY);
	}

	public void onInsn(int offset) {
		StackState newStack = loadStack(offset);
		if (newStack != null) {
			this.stack = newStack;
		} else if (!fallThrough && !excHandlers.contains(offset)) {
			// instruction reachable only by jumps from later instructions (or unreachable)
			StackState collectedStack = getCollectedJumpStack().get(offset);
			if (collectedStack != null) {
				this.stack = collectedStack.copy();
			}
		}
		fallThrough = isFallThrough(insn.getOpcode());
		if (excHandlers.contains(offset)) {
			clear();
			stack.push(StackValueType.NARROW); // push exception
			excHandler = true;
		} else {
			excHandler = false;
		}
	}

	private @Nullable StackState loadStack(int offset) {
		StackState stackState = jumpStack.get(offset);
		if (stackState != null) {
			return stackState.copy();
		}
		StackFrame frame = stackMapTable.getFor(offset);
		if (frame != null) {
			return new StackState(maxStack).fillFromFrame(frame);
		}
		return null;
	}

	private Map<Integer, StackState> getCollectedJumpStack() {
		Map<Integer, StackState> collected = collectedJumpStack;
		if (collected == null) {
			collected = jumpStackCollector != null ? jumpStackCollector.get() : Collections.emptyMap();
			collectedJumpStack = collected;
		}
		return collected;
	}

	private static boolean isFallThrough(Opcode opcode) {
		switch (opcode) {
			case GOTO:
			case RETURN:
			case RETURN_VOID:
			case THROW:
			case PACKED_SWITCH:
			case SPARSE_SWITCH:
			case JAVA_RET:
				return false;
			default:
				return true;
		}
	}

	public void registerJump(int jumpOffset) {
		Integer key = jumpOffset;
		if (!jumpStack.containsKey(key)) {
			jumpStack.put(key, stack.copy());
			if (newJumpListener != null) {
				newJumpListener.accept(key);
			}
		}
	}

	/**
	 * Set lazy provider of stack for jump targets, used if target visited before jump instruction
	 */
	public void setJumpStackCollector(Supplier<Map<Integer, StackState>> jumpStackCollector) {
		this.jumpStackCollector = jumpStackCollector;
	}

	public void setNewJumpListener(Consumer<Integer> newJumpListener) {
		this.newJumpListener = newJumpListener;
	}

	public Map<Integer, StackState> getJumpStack() {
		return jumpStack;
	}

	/**
	 * Start decoding from method start, jump target or exception handler
	 */
	public void startBlock() {
		stack = new StackState(maxStack);
		fallThrough = true;
	}

	public boolean isFallThrough() {
		return fallThrough;
	}

	public static void decodeForStack(InsnData insnData) {
		try {
			insnData.decode();
		} catch (Exception e) {
			// ignore, error will be reported at main decoding
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

	public StackValueType peekType(int at) {
		return stack.peekTypeAt(at);
	}

	public CodeDecodeState peekFrom(int pos, int arg) {
		insn.setArgReg(arg, stack.peekAt(pos));
		return this;
	}

	public CodeDecodeState push(int arg) {
		insn.setArgReg(arg, stack.push(StackValueType.NARROW));
		return this;
	}

	public CodeDecodeState push(int arg, StackValueType type) {
		insn.setArgReg(arg, stack.push(type));
		return this;
	}

	public CodeDecodeState pushWide(int arg) {
		insn.setArgReg(arg, stack.push(StackValueType.WIDE));
		return this;
	}

	public int insert(int pos, StackValueType type) {
		return stack.insert(pos, type);
	}

	public void discard() {
		stack.pop();
	}

	public void discardWord() {
		StackValueType type = stack.peekTypeAt(0);
		stack.pop();
		if (type == StackValueType.NARROW) {
			stack.pop();
		}
	}

	public CodeDecodeState clear() {
		stack.clear();
		return this;
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

	public StackValueType fieldType() {
		String type = insn.constPoolReader().getFieldType(insn().getIndex());
		return getSVType(type);
	}

	public StackValueType getSVType(String type) {
		if (type.equals("J") || type.equals("D")) {
			return StackValueType.WIDE;
		}
		return StackValueType.NARROW;
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
