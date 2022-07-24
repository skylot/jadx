package jadx.plugins.input.java.data.code;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.insns.InsnIndexType;
import jadx.api.plugins.input.insns.Opcode;
import jadx.plugins.input.java.data.code.StackState.SVType;
import jadx.plugins.input.java.data.code.decoders.IJavaInsnDecoder;
import jadx.plugins.input.java.data.code.decoders.InvokeDecoder;
import jadx.plugins.input.java.data.code.decoders.LoadConstDecoder;
import jadx.plugins.input.java.data.code.decoders.LookupSwitchDecoder;
import jadx.plugins.input.java.data.code.decoders.TableSwitchDecoder;
import jadx.plugins.input.java.data.code.decoders.WideDecoder;

import static jadx.plugins.input.java.data.code.StackState.SVType.NARROW;
import static jadx.plugins.input.java.data.code.StackState.SVType.WIDE;

@SuppressWarnings("SpellCheckingInspection")
public class JavaInsnsRegister {

	private static final JavaInsnInfo[] INSN_INFO;

	public static final long FLOAT_ZERO = Float.floatToIntBits(0.0f);
	public static final long FLOAT_ONE = Float.floatToIntBits(1.0f);
	public static final long FLOAT_TWO = Float.floatToIntBits(2.0f);

	public static final long DOUBLE_ZERO = Double.doubleToLongBits(0.0d);
	public static final long DOUBLE_ONE = Double.doubleToLongBits(1.0d);

	static {
		JavaInsnInfo[] arr = new JavaInsnInfo[0xCA];
		INSN_INFO = arr;
		register(arr, 0x00, "nop", 0, 0, Opcode.NOP, null);

		constInsn(arr, 0x01, "aconst_null", Opcode.CONST, 0);
		constInsn(arr, 0x02, "iconst_m1", Opcode.CONST, -1);
		constInsn(arr, 0x03, "iconst_0", Opcode.CONST, 0);
		constInsn(arr, 0x04, "iconst_1", Opcode.CONST, 1);
		constInsn(arr, 0x05, "iconst_2", Opcode.CONST, 2);
		constInsn(arr, 0x06, "iconst_3", Opcode.CONST, 3);
		constInsn(arr, 0x07, "iconst_4", Opcode.CONST, 4);
		constInsn(arr, 0x08, "iconst_5", Opcode.CONST, 5);

		constInsn(arr, 0x09, "lconst_0", Opcode.CONST_WIDE, 0L);
		constInsn(arr, 0x0a, "lconst_1", Opcode.CONST_WIDE, 1L);

		constInsn(arr, 0x0b, "fconst_0", Opcode.CONST, FLOAT_ZERO);
		constInsn(arr, 0x0c, "fconst_1", Opcode.CONST, FLOAT_ONE);
		constInsn(arr, 0x0d, "fconst_2", Opcode.CONST, FLOAT_TWO);

		constInsn(arr, 0x0e, "dconst_0", Opcode.CONST_WIDE, DOUBLE_ZERO);
		constInsn(arr, 0x0f, "dconst_1", Opcode.CONST_WIDE, DOUBLE_ONE);

		register(arr, 0x10, "bipush", 1, 2, Opcode.CONST, s -> s.lit(s.s1()).push(0));
		register(arr, 0x11, "sipush", 2, 2, Opcode.CONST, s -> s.lit(s.s2()).push(0));

		loadConst(arr, 0x12, "ldc", false);
		loadConst(arr, 0x13, "ldc_w", true);
		loadConst(arr, 0x14, "ldc2_w", true);

		register(arr, 0x15, "iload", 1, 2, Opcode.MOVE, s -> s.local(1, s.u1()).push(0));
		register(arr, 0x16, "lload", 1, 2, Opcode.MOVE_WIDE, s -> s.local(1, s.u1()).pushWide(0));
		register(arr, 0x17, "fload", 1, 2, Opcode.MOVE, s -> s.local(1, s.u1()).push(0));
		register(arr, 0x18, "dload", 1, 2, Opcode.MOVE_WIDE, s -> s.local(1, s.u1()).pushWide(0));
		register(arr, 0x19, "aload", 1, 2, Opcode.MOVE, s -> s.local(1, s.u1()).push(0));

		register(arr, 0x1a, "iload_0", 0, 2, Opcode.MOVE, s -> s.local(1, 0).push(0));
		register(arr, 0x1b, "iload_1", 0, 2, Opcode.MOVE, s -> s.local(1, 1).push(0));
		register(arr, 0x1c, "iload_2", 0, 2, Opcode.MOVE, s -> s.local(1, 2).push(0));
		register(arr, 0x1d, "iload_3", 0, 2, Opcode.MOVE, s -> s.local(1, 3).push(0));

		register(arr, 0x1e, "lload_0", 0, 2, Opcode.MOVE_WIDE, s -> s.local(1, 0).pushWide(0));
		register(arr, 0x1f, "lload_1", 0, 2, Opcode.MOVE_WIDE, s -> s.local(1, 1).pushWide(0));
		register(arr, 0x20, "lload_2", 0, 2, Opcode.MOVE_WIDE, s -> s.local(1, 2).pushWide(0));
		register(arr, 0x21, "lload_3", 0, 2, Opcode.MOVE_WIDE, s -> s.local(1, 3).pushWide(0));

		register(arr, 0x22, "fload_0", 0, 2, Opcode.MOVE, s -> s.local(1, 0).push(0));
		register(arr, 0x23, "fload_1", 0, 2, Opcode.MOVE, s -> s.local(1, 1).push(0));
		register(arr, 0x24, "fload_2", 0, 2, Opcode.MOVE, s -> s.local(1, 2).push(0));
		register(arr, 0x25, "fload_3", 0, 2, Opcode.MOVE, s -> s.local(1, 3).push(0));

		register(arr, 0x26, "dload_0", 0, 2, Opcode.MOVE_WIDE, s -> s.local(1, 0).pushWide(0));
		register(arr, 0x27, "dload_1", 0, 2, Opcode.MOVE_WIDE, s -> s.local(1, 1).pushWide(0));
		register(arr, 0x28, "dload_2", 0, 2, Opcode.MOVE_WIDE, s -> s.local(1, 2).pushWide(0));
		register(arr, 0x29, "dload_3", 0, 2, Opcode.MOVE_WIDE, s -> s.local(1, 3).pushWide(0));

		register(arr, 0x2a, "aload_0", 0, 2, Opcode.MOVE, s -> s.local(1, 0).push(0));
		register(arr, 0x2b, "aload_1", 0, 2, Opcode.MOVE, s -> s.local(1, 1).push(0));
		register(arr, 0x2c, "aload_2", 0, 2, Opcode.MOVE, s -> s.local(1, 2).push(0));
		register(arr, 0x2d, "aload_3", 0, 2, Opcode.MOVE, s -> s.local(1, 3).push(0));

		register(arr, 0x2e, "iaload", 0, 3, Opcode.AGET, aget());
		register(arr, 0x2f, "laload", 0, 3, Opcode.AGET_WIDE, agetWide());
		register(arr, 0x30, "faload", 0, 3, Opcode.AGET, aget());
		register(arr, 0x31, "daload", 0, 3, Opcode.AGET_WIDE, agetWide());
		register(arr, 0x32, "aaload", 0, 3, Opcode.AGET_OBJECT, aget());
		register(arr, 0x33, "baload", 0, 3, Opcode.AGET_BYTE_BOOLEAN, aget());
		register(arr, 0x34, "caload", 0, 3, Opcode.AGET_CHAR, aget());
		register(arr, 0x35, "saload", 0, 3, Opcode.AGET_SHORT, aget());

		register(arr, 0x36, "istore", 1, 2, Opcode.MOVE, s -> s.pop(1).local(0, s.u1()));
		register(arr, 0x37, "lstore", 1, 2, Opcode.MOVE_WIDE, s -> s.pop(1).local(0, s.u1()));
		register(arr, 0x38, "fstore", 1, 2, Opcode.MOVE, s -> s.pop(1).local(0, s.u1()));
		register(arr, 0x39, "dstore", 1, 2, Opcode.MOVE_WIDE, s -> s.pop(1).local(0, s.u1()));
		register(arr, 0x3a, "astore", 1, 2, Opcode.MOVE, s -> s.pop(1).local(0, s.u1()));

		register(arr, 0x3b, "istore_0", 0, 2, Opcode.MOVE, s -> s.pop(1).local(0, 0));
		register(arr, 0x3c, "istore_1", 0, 2, Opcode.MOVE, s -> s.pop(1).local(0, 1));
		register(arr, 0x3d, "istore_2", 0, 2, Opcode.MOVE, s -> s.pop(1).local(0, 2));
		register(arr, 0x3e, "istore_3", 0, 2, Opcode.MOVE, s -> s.pop(1).local(0, 3));

		register(arr, 0x3f, "lstore_0", 0, 2, Opcode.MOVE_WIDE, s -> s.pop(1).local(0, 0));
		register(arr, 0x40, "lstore_1", 0, 2, Opcode.MOVE_WIDE, s -> s.pop(1).local(0, 1));
		register(arr, 0x41, "lstore_2", 0, 2, Opcode.MOVE_WIDE, s -> s.pop(1).local(0, 2));
		register(arr, 0x42, "lstore_3", 0, 2, Opcode.MOVE_WIDE, s -> s.pop(1).local(0, 3));

		register(arr, 0x43, "fstore_0", 0, 2, Opcode.MOVE, s -> s.pop(1).local(0, 0));
		register(arr, 0x44, "fstore_1", 0, 2, Opcode.MOVE, s -> s.pop(1).local(0, 1));
		register(arr, 0x45, "fstore_2", 0, 2, Opcode.MOVE, s -> s.pop(1).local(0, 2));
		register(arr, 0x46, "fstore_3", 0, 2, Opcode.MOVE, s -> s.pop(1).local(0, 3));

		register(arr, 0x47, "dstore_0", 0, 2, Opcode.MOVE_WIDE, s -> s.pop(1).local(0, 0));
		register(arr, 0x48, "dstore_1", 0, 2, Opcode.MOVE_WIDE, s -> s.pop(1).local(0, 1));
		register(arr, 0x49, "dstore_2", 0, 2, Opcode.MOVE_WIDE, s -> s.pop(1).local(0, 2));
		register(arr, 0x4a, "dstore_3", 0, 2, Opcode.MOVE_WIDE, s -> s.pop(1).local(0, 3));

		register(arr, 0x4b, "astore_0", 0, 2, Opcode.MOVE, s -> s.pop(1).local(0, 0));
		register(arr, 0x4c, "astore_1", 0, 2, Opcode.MOVE, s -> s.pop(1).local(0, 1));
		register(arr, 0x4d, "astore_2", 0, 2, Opcode.MOVE, s -> s.pop(1).local(0, 2));
		register(arr, 0x4e, "astore_3", 0, 2, Opcode.MOVE, s -> s.pop(1).local(0, 3));

		register(arr, 0x4f, "iastore", 0, 3, Opcode.APUT, aput());
		register(arr, 0x50, "lastore", 0, 3, Opcode.APUT_WIDE, aput());
		register(arr, 0x51, "fastore", 0, 3, Opcode.APUT, aput());
		register(arr, 0x52, "dastore", 0, 3, Opcode.APUT_WIDE, aput());
		register(arr, 0x53, "aastore", 0, 3, Opcode.APUT_OBJECT, aput());
		register(arr, 0x54, "bastore", 0, 3, Opcode.APUT_BYTE_BOOLEAN, aput());
		register(arr, 0x55, "castore", 0, 3, Opcode.APUT_CHAR, aput());
		register(arr, 0x56, "sastore", 0, 3, Opcode.APUT_SHORT, aput());

		register(arr, 0x57, "pop", 0, 0, Opcode.NOP, CodeDecodeState::discard);
		register(arr, 0x58, "pop2", 0, 0, Opcode.NOP, CodeDecodeState::discardWord);

		register(arr, 0x59, "dup", 0, 2, Opcode.MOVE, s -> s.peek(1).push(0, s.peekType(1)));
		register(arr, 0x5a, "dup_x1", 0, 6, Opcode.MOVE_MULTI,
				s -> s.push(0, s.peekType(1)).peekFrom(1, 1)
						.peekFrom(1, 2).peekFrom(2, 3)
						.peekFrom(2, 4).peekFrom(0, 5));
		register(arr, 0x5b, "dup_x2", 0, 8, Opcode.MOVE_MULTI,
				s -> s.push(0, s.peekType(1)).peekFrom(1, 1)
						.peekFrom(1, 2).peekFrom(2, 3)
						.peekFrom(2, 4).peekFrom(3, 5)
						.peekFrom(3, 6).peekFrom(0, 7));
		register(arr, 0x5c, "dup2", 0, 4, Opcode.MOVE_MULTI, s -> {
			if (s.peekType(0) == NARROW) {
				s.peekFrom(0, 3).peekFrom(1, 1).push(0, NARROW).push(2, NARROW);
			} else {
				s.peek(1).push(0, s.peekType(1));
			}
		});
		register(arr, 0x5d, "dup2_x1", 0, 10, Opcode.MOVE_MULTI,
				s -> {
					if (s.peekType(0) == NARROW) {
						s.push(0, NARROW).peekFrom(2, 1)
								.push(2, NARROW).peekFrom(2, 3)
								.peekFrom(2, 4).peekFrom(4, 5)
								.peekFrom(3, 6).peekFrom(0, 7)
								.peekFrom(4, 8).peekFrom(1, 9);
					} else {
						s.insn().setRegsCount(6);
						s.push(0, WIDE).peekFrom(1, 1)
								.peekFrom(1, 2).peekFrom(2, 3)
								.peekFrom(2, 4).peekFrom(0, 5);
					}
				});
		register(arr, 0x5f, "swap", 0, 6, Opcode.MOVE_MULTI,
				s -> s.peekFrom(-1, 0).peekFrom(1, 1)
						.peekFrom(1, 2).peekFrom(0, 3)
						.peekFrom(0, 4).peekFrom(-1, 5));

		register(arr, 0x60, "iadd", 0, 3, Opcode.ADD_INT, twoRegsWithResult(NARROW));
		register(arr, 0x61, "ladd", 0, 3, Opcode.ADD_LONG, twoRegsWithResult(WIDE));
		register(arr, 0x62, "fadd", 0, 3, Opcode.ADD_FLOAT, twoRegsWithResult(NARROW));
		register(arr, 0x63, "dadd", 0, 3, Opcode.ADD_DOUBLE, twoRegsWithResult(WIDE));

		register(arr, 0x64, "isub", 0, 3, Opcode.SUB_INT, twoRegsWithResult(NARROW));
		register(arr, 0x65, "lsub", 0, 3, Opcode.SUB_LONG, twoRegsWithResult(WIDE));
		register(arr, 0x66, "fsub", 0, 3, Opcode.SUB_FLOAT, twoRegsWithResult(NARROW));
		register(arr, 0x67, "dsub", 0, 3, Opcode.SUB_DOUBLE, twoRegsWithResult(WIDE));

		register(arr, 0x68, "imul", 0, 3, Opcode.MUL_INT, twoRegsWithResult(NARROW));
		register(arr, 0x69, "lmul", 0, 3, Opcode.MUL_LONG, twoRegsWithResult(WIDE));
		register(arr, 0x6a, "fmul", 0, 3, Opcode.MUL_FLOAT, twoRegsWithResult(NARROW));
		register(arr, 0x6b, "dmul", 0, 3, Opcode.MUL_DOUBLE, twoRegsWithResult(WIDE));

		register(arr, 0x6c, "idiv", 0, 3, Opcode.DIV_INT, twoRegsWithResult(NARROW));
		register(arr, 0x6d, "ldiv", 0, 3, Opcode.DIV_LONG, twoRegsWithResult(WIDE));
		register(arr, 0x6e, "fdiv", 0, 3, Opcode.DIV_FLOAT, twoRegsWithResult(NARROW));
		register(arr, 0x6f, "ddiv", 0, 3, Opcode.DIV_DOUBLE, twoRegsWithResult(WIDE));

		register(arr, 0x70, "irem", 0, 3, Opcode.REM_INT, twoRegsWithResult(NARROW));
		register(arr, 0x71, "lrem", 0, 3, Opcode.REM_LONG, twoRegsWithResult(WIDE));
		register(arr, 0x72, "frem", 0, 3, Opcode.REM_FLOAT, twoRegsWithResult(NARROW));
		register(arr, 0x73, "drem", 0, 3, Opcode.REM_DOUBLE, twoRegsWithResult(WIDE));

		register(arr, 0x74, "ineg", 0, 2, Opcode.NEG_INT, oneRegWithResult(NARROW));
		register(arr, 0x75, "lneg", 0, 2, Opcode.NEG_LONG, oneRegWithResult(WIDE));
		register(arr, 0x76, "fneg", 0, 2, Opcode.NEG_FLOAT, oneRegWithResult(NARROW));
		register(arr, 0x77, "dneg", 0, 2, Opcode.NEG_DOUBLE, oneRegWithResult(WIDE));

		register(arr, 0x78, "ishl", 0, 3, Opcode.SHL_INT, twoRegsWithResult(NARROW));
		register(arr, 0x79, "lshl", 0, 3, Opcode.SHL_LONG, twoRegsWithResult(WIDE));
		register(arr, 0x7a, "ishr", 0, 3, Opcode.SHR_INT, twoRegsWithResult(NARROW));
		register(arr, 0x7b, "lshr", 0, 3, Opcode.SHR_LONG, twoRegsWithResult(WIDE));
		register(arr, 0x7c, "iushr", 0, 3, Opcode.USHR_INT, twoRegsWithResult(NARROW));
		register(arr, 0x7d, "lushr", 0, 3, Opcode.USHR_LONG, twoRegsWithResult(WIDE));

		register(arr, 0x7e, "iand", 0, 3, Opcode.AND_INT, twoRegsWithResult(NARROW));
		register(arr, 0x7f, "land", 0, 3, Opcode.AND_LONG, twoRegsWithResult(WIDE));
		register(arr, 0x80, "ior", 0, 3, Opcode.OR_INT, twoRegsWithResult(NARROW));
		register(arr, 0x81, "lor", 0, 3, Opcode.OR_LONG, twoRegsWithResult(WIDE));
		register(arr, 0x82, "ixor", 0, 3, Opcode.XOR_INT, twoRegsWithResult(NARROW));
		register(arr, 0x83, "lxor", 0, 3, Opcode.XOR_LONG, twoRegsWithResult(WIDE));

		register(arr, 0x84, "iinc", 2, 2, Opcode.ADD_INT_LIT, s -> {
			int varNum = s.u1();
			s.local(0, varNum).local(1, varNum).lit(s.reader().readS1());
		});

		register(arr, 0x85, "i2l", 0, 2, Opcode.INT_TO_LONG, oneRegWithResult(WIDE));
		register(arr, 0x86, "i2f", 0, 2, Opcode.INT_TO_FLOAT, oneRegWithResult(NARROW));
		register(arr, 0x87, "i2d", 0, 2, Opcode.INT_TO_DOUBLE, oneRegWithResult(WIDE));
		register(arr, 0x88, "l2i", 0, 2, Opcode.LONG_TO_INT, oneRegWithResult(NARROW));
		register(arr, 0x89, "l2f", 0, 2, Opcode.LONG_TO_FLOAT, oneRegWithResult(NARROW));
		register(arr, 0x8a, "l2d", 0, 2, Opcode.LONG_TO_DOUBLE, oneRegWithResult(WIDE));
		register(arr, 0x8b, "f2i", 0, 2, Opcode.FLOAT_TO_INT, oneRegWithResult(NARROW));
		register(arr, 0x8c, "f2l", 0, 2, Opcode.FLOAT_TO_LONG, oneRegWithResult(WIDE));
		register(arr, 0x8d, "f2d", 0, 2, Opcode.FLOAT_TO_DOUBLE, oneRegWithResult(WIDE));
		register(arr, 0x8e, "d2i", 0, 2, Opcode.DOUBLE_TO_INT, oneRegWithResult(NARROW));
		register(arr, 0x8f, "d2l", 0, 2, Opcode.DOUBLE_TO_LONG, oneRegWithResult(WIDE));
		register(arr, 0x90, "d2f", 0, 2, Opcode.DOUBLE_TO_FLOAT, oneRegWithResult(NARROW));
		register(arr, 0x91, "i2b", 0, 2, Opcode.INT_TO_BYTE, oneRegWithResult(NARROW));
		register(arr, 0x92, "i2c", 0, 2, Opcode.INT_TO_CHAR, oneRegWithResult(NARROW));
		register(arr, 0x93, "i2s", 0, 2, Opcode.INT_TO_SHORT, oneRegWithResult(NARROW));

		register(arr, 0x94, "lcmp", 0, 3, Opcode.CMP_LONG, twoRegsWithResult(NARROW));
		register(arr, 0x95, "fcmpl", 0, 3, Opcode.CMPL_FLOAT, twoRegsWithResult(NARROW));
		register(arr, 0x96, "fcmpg", 0, 3, Opcode.CMPG_FLOAT, twoRegsWithResult(NARROW));
		register(arr, 0x97, "dcmpl", 0, 3, Opcode.CMPL_DOUBLE, twoRegsWithResult(NARROW));
		register(arr, 0x98, "dcmpg", 0, 3, Opcode.CMPG_DOUBLE, twoRegsWithResult(NARROW));

		register(arr, 0x99, "ifeq", 2, 1, Opcode.IF_EQZ, zeroCmp());
		register(arr, 0x9a, "ifne", 2, 1, Opcode.IF_NEZ, zeroCmp());
		register(arr, 0x9b, "iflt", 2, 1, Opcode.IF_LTZ, zeroCmp());
		register(arr, 0x9c, "ifge", 2, 1, Opcode.IF_GEZ, zeroCmp());
		register(arr, 0x9d, "ifgt", 2, 1, Opcode.IF_GTZ, zeroCmp());
		register(arr, 0x9e, "ifle", 2, 1, Opcode.IF_LEZ, zeroCmp());

		register(arr, 0x9f, "if_icmpeq", 2, 2, Opcode.IF_EQ, cmp());
		register(arr, 0xa0, "if_icmpne", 2, 2, Opcode.IF_NE, cmp());
		register(arr, 0xa1, "if_icmplt", 2, 2, Opcode.IF_LT, cmp());
		register(arr, 0xa2, "if_icmpge", 2, 2, Opcode.IF_GE, cmp());
		register(arr, 0xa3, "if_icmpgt", 2, 2, Opcode.IF_GT, cmp());
		register(arr, 0xa4, "if_icmple", 2, 2, Opcode.IF_LE, cmp());
		register(arr, 0xa5, "if_acmpeq", 2, 2, Opcode.IF_EQ, cmp());
		register(arr, 0xa6, "if_acmpne", 2, 2, Opcode.IF_NE, cmp());

		register(arr, 0xa7, "goto", 2, 0, Opcode.GOTO, s -> s.jump(s.s2()));

		register(arr, 0xaa, "tableswitch", -1, 1, Opcode.PACKED_SWITCH, new TableSwitchDecoder());
		register(arr, 0xab, "lookupswitch", -1, 1, Opcode.SPARSE_SWITCH, new LookupSwitchDecoder());

		register(arr, 0xac, "ireturn", 0, 1, Opcode.RETURN, s -> s.pop(0));
		register(arr, 0xad, "lreturn", 0, 1, Opcode.RETURN, s -> s.pop(0));
		register(arr, 0xae, "freturn", 0, 1, Opcode.RETURN, s -> s.pop(0));
		register(arr, 0xaf, "dreturn", 0, 1, Opcode.RETURN, s -> s.pop(0));
		register(arr, 0xb0, "areturn", 0, 1, Opcode.RETURN, s -> s.pop(0));
		register(arr, 0xb1, "return", 0, 0, Opcode.RETURN_VOID, null);

		register(arr, 0xb2, "getstatic", 2, 1, Opcode.SGET, InsnIndexType.FIELD_REF, s -> s.idx(s.u2()).push(0, s.fieldType()));
		register(arr, 0xb3, "putstatic", 2, 1, Opcode.SPUT, InsnIndexType.FIELD_REF, s -> s.idx(s.u2()).pop(0));
		register(arr, 0xb4, "getfield", 2, 2, Opcode.IGET, InsnIndexType.FIELD_REF, s -> s.idx(s.u2()).pop(1).push(0, s.fieldType()));
		register(arr, 0xb5, "putfield", 2, 2, Opcode.IPUT, InsnIndexType.FIELD_REF, s -> s.idx(s.u2()).pop(0).pop(1));

		invoke(arr, 0xb6, "invokevirtual", 2, Opcode.INVOKE_VIRTUAL);
		invoke(arr, 0xb7, "invokespecial", 2, Opcode.INVOKE_SPECIAL);
		invoke(arr, 0xb8, "invokestatic", 2, Opcode.INVOKE_STATIC);
		invoke(arr, 0xb9, "invokeinterface", 4, Opcode.INVOKE_INTERFACE);
		invoke(arr, 0xba, "invokedynamic", 4, Opcode.INVOKE_CUSTOM);

		register(arr, 0xbb, "new", 2, 1, Opcode.NEW_INSTANCE, InsnIndexType.TYPE_REF, s -> s.idx(s.u2()).push(0));
		register(arr, 0xbc, "newarray", 1, 2, Opcode.NEW_ARRAY, InsnIndexType.TYPE_REF, s -> s.idx(s.u1()).pop(1).push(0).lit(1));
		register(arr, 0xbd, "anewarray", 2, 2, Opcode.NEW_ARRAY, InsnIndexType.TYPE_REF, s -> s.idx(s.u2()).pop(1).push(0).lit(1));
		register(arr, 0xbe, "arraylength", 0, 2, Opcode.ARRAY_LENGTH, oneRegWithResult(NARROW));
		register(arr, 0xbf, "athrow", 0, 1, Opcode.THROW, s -> s.pop(0).clear());

		register(arr, 0xc0, "checkcast", 2, 2, Opcode.CHECK_CAST, InsnIndexType.TYPE_REF, s -> s.idx(s.u2()).pop(1).push(0));
		register(arr, 0xc1, "instanceof", 2, 2, Opcode.INSTANCE_OF, InsnIndexType.TYPE_REF, s -> s.idx(s.u2()).pop(1).push(0));

		register(arr, 0xc2, "monitorenter", 0, 1, Opcode.MONITOR_ENTER, s -> s.pop(0));
		register(arr, 0xc3, "monitorexit", 0, 1, Opcode.MONITOR_EXIT, s -> s.pop(0));

		register(arr, 0xc4, "wide", -1, -1, Opcode.NOP, new WideDecoder());

		register(arr, 0xc5, "multianewarray", 3, -1, Opcode.NEW_ARRAY, InsnIndexType.TYPE_REF, newArrayMulti());
		register(arr, 0xc6, "ifnull", 2, 1, Opcode.IF_EQZ, zeroCmp());
		register(arr, 0xc7, "ifnonnull", 2, 1, Opcode.IF_NEZ, zeroCmp());

		register(arr, 0xc8, "goto_w", 4, 0, Opcode.GOTO, s -> s.jump(s.reader().readS4()));
	}

	private static IJavaInsnDecoder newArrayMulti() {
		return s -> {
			s.idx(s.u2());
			int dim = s.u1();
			JavaInsnData insn = s.insn();
			insn.setLiteral(dim);
			insn.setRegsCount(dim + 1);
			for (int i = dim; i > 0; i--) {
				s.pop(i);
			}
			s.push(0);
		};
	}

	private static IJavaInsnDecoder oneRegWithResult(SVType type) {
		return s -> s.pop(1).push(0, type);
	}

	private static IJavaInsnDecoder twoRegsWithResult(SVType type) {
		return s -> s.pop(2).pop(1).push(0, type);
	}

	private static IJavaInsnDecoder aget() {
		return s -> s.pop(2).pop(1).push(0);
	}

	private static IJavaInsnDecoder agetWide() {
		return s -> s.pop(2).pop(1).pushWide(0);
	}

	private static IJavaInsnDecoder aput() {
		return s -> s.pop(0).pop(2).pop(1);
	}

	private static IJavaInsnDecoder zeroCmp() {
		return s -> s.pop(0).jump(s.s2());
	}

	private static IJavaInsnDecoder cmp() {
		return s -> s.pop(1).pop(0).jump(s.s2());
	}

	private static void invoke(JavaInsnInfo[] arr, int opcode, String name, int payloadSize, Opcode apiOpcode) {
		InsnIndexType indexType = apiOpcode == Opcode.INVOKE_CUSTOM ? InsnIndexType.CALL_SITE : InsnIndexType.METHOD_REF;
		register(arr, opcode, name, payloadSize, -1, apiOpcode, indexType, new InvokeDecoder(payloadSize, apiOpcode));
	}

	private static void constInsn(JavaInsnInfo[] arr, int opcode, String name, Opcode apiOpcode, long literal) {
		register(arr, opcode, name, 0, 1, apiOpcode, InsnIndexType.NONE, state -> {
			state.insn().setLiteral(literal);
			state.push(0, apiOpcode == Opcode.CONST_WIDE ? SVType.WIDE : NARROW);
		});
	}

	private static void loadConst(JavaInsnInfo[] arr, int opcode, String name, boolean wide) {
		register(arr, opcode, name, wide ? 2 : 1, 2, Opcode.CONST, InsnIndexType.NONE, new LoadConstDecoder(wide));
	}

	private static void register(JavaInsnInfo[] arr, int opcode, String name, int payloadSize, int regsCount,
			Opcode apiOpcode, IJavaInsnDecoder decoder) {
		register(arr, opcode, name, payloadSize, regsCount, apiOpcode, InsnIndexType.NONE, decoder);
	}

	private static void register(JavaInsnInfo[] arr, int opcode, String name, int payloadSize, int regsCount,
			Opcode apiOpcode, InsnIndexType indexType, IJavaInsnDecoder decoder) {
		if (arr[opcode] != null) {
			throw new IllegalStateException("Duplicate opcode init: 0x" + Integer.toHexString(opcode));
		}
		arr[opcode] = new JavaInsnInfo(opcode, name, payloadSize, regsCount, apiOpcode, indexType, decoder);
	}

	@Nullable
	public static JavaInsnInfo get(int opcode) {
		return INSN_INFO[opcode];
	}
}
