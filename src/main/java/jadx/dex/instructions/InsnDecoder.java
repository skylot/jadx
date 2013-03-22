package jadx.dex.instructions;

import jadx.dex.info.FieldInfo;
import jadx.dex.instructions.args.ArgType;
import jadx.dex.instructions.args.InsnArg;
import jadx.dex.instructions.args.PrimitiveType;
import jadx.dex.instructions.args.RegisterArg;
import jadx.dex.nodes.DexNode;
import jadx.dex.nodes.InsnNode;
import jadx.dex.nodes.MethodNode;
import jadx.utils.exceptions.DecodeException;

import com.android.dx.io.Code;
import com.android.dx.io.OpcodeInfo;
import com.android.dx.io.Opcodes;
import com.android.dx.io.instructions.DecodedInstruction;
import com.android.dx.io.instructions.FillArrayDataPayloadDecodedInstruction;
import com.android.dx.io.instructions.PackedSwitchPayloadDecodedInstruction;
import com.android.dx.io.instructions.SparseSwitchPayloadDecodedInstruction;

public class InsnDecoder {

	private final MethodNode method;
	private final DecodedInstruction[] insnArr;
	private final DexNode dex;

	public InsnDecoder(MethodNode mthNode, Code mthCode) {
		this.method = mthNode;
		this.dex = method.dex();
		this.insnArr = DecodedInstruction.decodeAll(mthCode.getInstructions());
	}

	public InsnNode[] run() throws DecodeException {
		InsnNode[] instructions = new InsnNode[insnArr.length];

		for (int i = 0; i < insnArr.length; i++) {
			DecodedInstruction rawInsn = insnArr[i];
			if (rawInsn != null) {
				InsnNode insn = decode(rawInsn, i);
				if (insn != null) {
					insn.setOffset(i);
					insn.setInsnHashCode(calcHashCode(rawInsn));
				}
				instructions[i] = insn;
			} else {
				instructions[i] = null;
			}
		}
		return instructions;
	}

	private int calcHashCode(DecodedInstruction insn) {
		int hash = insn.getOpcode();
		hash = hash * 31 + insn.getClass().getName().hashCode();
		hash = hash * 31 + insn.getFormat().ordinal();
		hash = hash * 31 + insn.getRegisterCount();
		hash = hash * 31 + insn.getIndex();
		hash = hash * 31 + insn.getTarget();
		hash = hash * 31 + insn.getA();
		hash = hash * 31 + insn.getB();
		hash = hash * 31 + insn.getC();
		hash = hash * 31 + insn.getD();
		hash = hash * 31 + insn.getE();
		return hash;
	}

	private InsnNode decode(DecodedInstruction insn, int offset) throws DecodeException {
		switch (insn.getOpcode()) {
			case Opcodes.NOP:
			case Opcodes.PACKED_SWITCH_PAYLOAD:
			case Opcodes.SPARSE_SWITCH_PAYLOAD:
			case Opcodes.FILL_ARRAY_DATA_PAYLOAD:
				return null;

				// move-result will be process in invoke and filled-new-array instructions
			case Opcodes.MOVE_RESULT:
			case Opcodes.MOVE_RESULT_WIDE:
			case Opcodes.MOVE_RESULT_OBJECT:
				return new InsnNode(method, InsnType.NOP, 0);

			case Opcodes.CONST:
			case Opcodes.CONST_4:
			case Opcodes.CONST_16:
			case Opcodes.CONST_HIGH16:
				return insn(InsnType.CONST, InsnArg.reg(insn, 0, ArgType.NARROW),
						InsnArg.lit(insn, ArgType.NARROW));

			case Opcodes.CONST_WIDE:
			case Opcodes.CONST_WIDE_16:
			case Opcodes.CONST_WIDE_32:
			case Opcodes.CONST_WIDE_HIGH16:
				return insn(InsnType.CONST, InsnArg.reg(insn, 0, ArgType.WIDE),
						InsnArg.lit(insn, ArgType.WIDE));

			case Opcodes.CONST_STRING:
			case Opcodes.CONST_STRING_JUMBO: {
				InsnNode node = new IndexInsnNode(method, InsnType.CONST, dex.getString(insn.getIndex()), 0);
				node.setResult(InsnArg.reg(insn, 0, ArgType.STRING));
				return node;
			}

			case Opcodes.CONST_CLASS: {
				InsnNode node = new IndexInsnNode(method, InsnType.CONST, dex.getType(insn.getIndex()), 0);
				node.setResult(InsnArg.reg(insn, 0, ArgType.CLASS));
				return node;
			}

			case Opcodes.MOVE:
			case Opcodes.MOVE_16:
			case Opcodes.MOVE_FROM16:
				return insn(InsnType.MOVE,
						InsnArg.reg(insn, 0, ArgType.NARROW),
						InsnArg.reg(insn, 1, ArgType.NARROW));

			case Opcodes.MOVE_WIDE:
			case Opcodes.MOVE_WIDE_16:
			case Opcodes.MOVE_WIDE_FROM16:
				return insn(InsnType.MOVE,
						InsnArg.reg(insn, 0, ArgType.WIDE),
						InsnArg.reg(insn, 1, ArgType.WIDE));

			case Opcodes.MOVE_OBJECT:
			case Opcodes.MOVE_OBJECT_16:
			case Opcodes.MOVE_OBJECT_FROM16:
				return insn(InsnType.MOVE,
						InsnArg.reg(insn, 0, ArgType.UNKNOWN_OBJECT),
						InsnArg.reg(insn, 1, ArgType.UNKNOWN_OBJECT));

			case Opcodes.ADD_INT:
			case Opcodes.ADD_INT_2ADDR:
				return arith(insn, ArithOp.ADD, ArgType.INT);

			case Opcodes.ADD_DOUBLE:
			case Opcodes.ADD_DOUBLE_2ADDR:
				return arith(insn, ArithOp.ADD, ArgType.DOUBLE);

			case Opcodes.ADD_FLOAT:
			case Opcodes.ADD_FLOAT_2ADDR:
				return arith(insn, ArithOp.ADD, ArgType.FLOAT);

			case Opcodes.ADD_LONG:
			case Opcodes.ADD_LONG_2ADDR:
				return arith(insn, ArithOp.ADD, ArgType.LONG);

			case Opcodes.ADD_INT_LIT8:
			case Opcodes.ADD_INT_LIT16:
				return arith_lit(insn, ArithOp.ADD, ArgType.INT);

			case Opcodes.SUB_INT:
			case Opcodes.SUB_INT_2ADDR:
				return arith(insn, ArithOp.SUB, ArgType.INT);

			case Opcodes.RSUB_INT:
				return new ArithNode(method, ArithOp.SUB,
						InsnArg.reg(insn, 0, ArgType.INT),
						InsnArg.reg(insn, 2, ArgType.INT),
						InsnArg.reg(insn, 1, ArgType.INT));

			case Opcodes.RSUB_INT_LIT8:
				return new ArithNode(method, ArithOp.SUB,
						InsnArg.reg(insn, 0, ArgType.INT),
						InsnArg.lit(insn, ArgType.INT),
						InsnArg.reg(insn, 1, ArgType.INT));

			case Opcodes.SUB_LONG:
			case Opcodes.SUB_LONG_2ADDR:
				return arith(insn, ArithOp.SUB, ArgType.LONG);

			case Opcodes.SUB_FLOAT:
			case Opcodes.SUB_FLOAT_2ADDR:
				return arith(insn, ArithOp.SUB, ArgType.FLOAT);

			case Opcodes.SUB_DOUBLE:
			case Opcodes.SUB_DOUBLE_2ADDR:
				return arith(insn, ArithOp.SUB, ArgType.DOUBLE);

			case Opcodes.MUL_INT:
			case Opcodes.MUL_INT_2ADDR:
				return arith(insn, ArithOp.MUL, ArgType.INT);

			case Opcodes.MUL_DOUBLE:
			case Opcodes.MUL_DOUBLE_2ADDR:
				return arith(insn, ArithOp.MUL, ArgType.DOUBLE);

			case Opcodes.MUL_FLOAT:
			case Opcodes.MUL_FLOAT_2ADDR:
				return arith(insn, ArithOp.MUL, ArgType.FLOAT);

			case Opcodes.MUL_LONG:
			case Opcodes.MUL_LONG_2ADDR:
				return arith(insn, ArithOp.MUL, ArgType.LONG);

			case Opcodes.MUL_INT_LIT8:
			case Opcodes.MUL_INT_LIT16:
				return arith_lit(insn, ArithOp.MUL, ArgType.INT);

			case Opcodes.DIV_INT:
			case Opcodes.DIV_INT_2ADDR:
				return arith(insn, ArithOp.DIV, ArgType.INT);

			case Opcodes.REM_INT:
			case Opcodes.REM_INT_2ADDR:
				return arith(insn, ArithOp.REM, ArgType.INT);

			case Opcodes.REM_LONG:
			case Opcodes.REM_LONG_2ADDR:
				return arith(insn, ArithOp.REM, ArgType.LONG);

			case Opcodes.REM_FLOAT:
			case Opcodes.REM_FLOAT_2ADDR:
				return arith(insn, ArithOp.REM, ArgType.FLOAT);

			case Opcodes.REM_DOUBLE:
			case Opcodes.REM_DOUBLE_2ADDR:
				return arith(insn, ArithOp.REM, ArgType.DOUBLE);

			case Opcodes.DIV_DOUBLE:
			case Opcodes.DIV_DOUBLE_2ADDR:
				return arith(insn, ArithOp.DIV, ArgType.DOUBLE);

			case Opcodes.DIV_FLOAT:
			case Opcodes.DIV_FLOAT_2ADDR:
				return arith(insn, ArithOp.DIV, ArgType.FLOAT);

			case Opcodes.DIV_LONG:
			case Opcodes.DIV_LONG_2ADDR:
				return arith(insn, ArithOp.DIV, ArgType.LONG);

			case Opcodes.DIV_INT_LIT8:
			case Opcodes.DIV_INT_LIT16:
				return arith_lit(insn, ArithOp.DIV, ArgType.INT);

			case Opcodes.REM_INT_LIT8:
			case Opcodes.REM_INT_LIT16:
				return arith_lit(insn, ArithOp.REM, ArgType.INT);

			case Opcodes.AND_INT:
			case Opcodes.AND_INT_2ADDR:
				return arith(insn, ArithOp.AND, ArgType.INT);

			case Opcodes.AND_INT_LIT8:
			case Opcodes.AND_INT_LIT16:
				return arith_lit(insn, ArithOp.AND, ArgType.INT);

			case Opcodes.XOR_INT_LIT8:
			case Opcodes.XOR_INT_LIT16:
				return arith_lit(insn, ArithOp.XOR, ArgType.INT);

			case Opcodes.AND_LONG:
			case Opcodes.AND_LONG_2ADDR:
				return arith(insn, ArithOp.AND, ArgType.LONG);

			case Opcodes.OR_INT:
			case Opcodes.OR_INT_2ADDR:
				return arith(insn, ArithOp.OR, ArgType.INT);

			case Opcodes.OR_INT_LIT8:
			case Opcodes.OR_INT_LIT16:
				return arith_lit(insn, ArithOp.OR, ArgType.INT);

			case Opcodes.XOR_INT:
			case Opcodes.XOR_INT_2ADDR:
				return arith(insn, ArithOp.XOR, ArgType.INT);

			case Opcodes.OR_LONG:
			case Opcodes.OR_LONG_2ADDR:
				return arith(insn, ArithOp.OR, ArgType.LONG);

			case Opcodes.XOR_LONG:
			case Opcodes.XOR_LONG_2ADDR:
				return arith(insn, ArithOp.XOR, ArgType.LONG);

			case Opcodes.USHR_INT:
			case Opcodes.USHR_INT_2ADDR:
				return arith(insn, ArithOp.USHR, ArgType.INT);

			case Opcodes.USHR_LONG:
			case Opcodes.USHR_LONG_2ADDR:
				return arith(insn, ArithOp.USHR, ArgType.LONG);

			case Opcodes.SHL_INT:
			case Opcodes.SHL_INT_2ADDR:
				return arith(insn, ArithOp.SHL, ArgType.INT);

			case Opcodes.SHL_LONG:
			case Opcodes.SHL_LONG_2ADDR:
				return arith(insn, ArithOp.SHL, ArgType.LONG);

			case Opcodes.SHR_INT:
			case Opcodes.SHR_INT_2ADDR:
				return arith(insn, ArithOp.SHR, ArgType.INT);

			case Opcodes.SHR_LONG:
			case Opcodes.SHR_LONG_2ADDR:
				return arith(insn, ArithOp.SHR, ArgType.LONG);

			case Opcodes.SHL_INT_LIT8:
				return arith_lit(insn, ArithOp.SHL, ArgType.INT);
			case Opcodes.SHR_INT_LIT8:
				return arith_lit(insn, ArithOp.SHR, ArgType.INT);
			case Opcodes.USHR_INT_LIT8:
				return arith_lit(insn, ArithOp.USHR, ArgType.INT);

			case Opcodes.NEG_INT:
				return neg(insn, ArgType.INT);
			case Opcodes.NEG_LONG:
				return neg(insn, ArgType.LONG);
			case Opcodes.NEG_FLOAT:
				return neg(insn, ArgType.FLOAT);
			case Opcodes.NEG_DOUBLE:
				return neg(insn, ArgType.DOUBLE);

			case Opcodes.INT_TO_BYTE:
				return cast(insn, ArgType.INT, ArgType.BYTE);
			case Opcodes.INT_TO_CHAR:
				return cast(insn, ArgType.INT, ArgType.CHAR);
			case Opcodes.INT_TO_SHORT:
				return cast(insn, ArgType.INT, ArgType.SHORT);
			case Opcodes.INT_TO_FLOAT:
				return cast(insn, ArgType.INT, ArgType.FLOAT);
			case Opcodes.INT_TO_DOUBLE:
				return cast(insn, ArgType.INT, ArgType.DOUBLE);
			case Opcodes.INT_TO_LONG:
				return cast(insn, ArgType.INT, ArgType.LONG);

			case Opcodes.FLOAT_TO_INT:
				return cast(insn, ArgType.FLOAT, ArgType.INT);
			case Opcodes.FLOAT_TO_DOUBLE:
				return cast(insn, ArgType.FLOAT, ArgType.DOUBLE);
			case Opcodes.FLOAT_TO_LONG:
				return cast(insn, ArgType.FLOAT, ArgType.LONG);

			case Opcodes.DOUBLE_TO_INT:
				return cast(insn, ArgType.DOUBLE, ArgType.INT);
			case Opcodes.DOUBLE_TO_FLOAT:
				return cast(insn, ArgType.DOUBLE, ArgType.FLOAT);
			case Opcodes.DOUBLE_TO_LONG:
				return cast(insn, ArgType.DOUBLE, ArgType.LONG);

			case Opcodes.LONG_TO_INT:
				return cast(insn, ArgType.LONG, ArgType.INT);
			case Opcodes.LONG_TO_FLOAT:
				return cast(insn, ArgType.LONG, ArgType.FLOAT);
			case Opcodes.LONG_TO_DOUBLE:
				return cast(insn, ArgType.LONG, ArgType.DOUBLE);

			case Opcodes.IF_EQ:
			case Opcodes.IF_EQZ:
				return new IfNode(method, insn, IfOp.EQ);

			case Opcodes.IF_NE:
			case Opcodes.IF_NEZ:
				return new IfNode(method, insn, IfOp.NE);

			case Opcodes.IF_GT:
			case Opcodes.IF_GTZ:
				return new IfNode(method, insn, IfOp.GT);

			case Opcodes.IF_GE:
			case Opcodes.IF_GEZ:
				return new IfNode(method, insn, IfOp.GE);

			case Opcodes.IF_LT:
			case Opcodes.IF_LTZ:
				return new IfNode(method, insn, IfOp.LT);

			case Opcodes.IF_LE:
			case Opcodes.IF_LEZ:
				return new IfNode(method, insn, IfOp.LE);

			case Opcodes.CMP_LONG:
				return cmp(insn, InsnType.CMP_L, ArgType.LONG);
			case Opcodes.CMPL_FLOAT:
				return cmp(insn, InsnType.CMP_L, ArgType.FLOAT);
			case Opcodes.CMPL_DOUBLE:
				return cmp(insn, InsnType.CMP_L, ArgType.DOUBLE);

			case Opcodes.CMPG_FLOAT:
				return cmp(insn, InsnType.CMP_G, ArgType.FLOAT);
			case Opcodes.CMPG_DOUBLE:
				return cmp(insn, InsnType.CMP_G, ArgType.DOUBLE);

			case Opcodes.GOTO:
			case Opcodes.GOTO_16:
			case Opcodes.GOTO_32:
				return new GotoNode(method, insn.getTarget());

			case Opcodes.THROW:
				return insn(InsnType.THROW, null,
						InsnArg.reg(insn, 0, ArgType.unknown(PrimitiveType.OBJECT)));

			case Opcodes.MOVE_EXCEPTION:
				return insn(InsnType.MOVE_EXCEPTION,
						InsnArg.reg(insn, 0, ArgType.unknown(PrimitiveType.OBJECT)));

			case Opcodes.RETURN_VOID:
				return new InsnNode(method, InsnType.RETURN, 0);

			case Opcodes.RETURN:
			case Opcodes.RETURN_WIDE:
			case Opcodes.RETURN_OBJECT:
				return insn(InsnType.RETURN,
						null,
						InsnArg.reg(insn, 0, method.getMethodInfo().getReturnType()));

			case Opcodes.INSTANCE_OF: {
				InsnNode node = new IndexInsnNode(method, InsnType.INSTANCE_OF, dex.getType(insn.getIndex()), 1);
				node.setResult(InsnArg.reg(insn, 0, ArgType.BOOLEAN));
				node.addArg(InsnArg.reg(insn, 1, ArgType.UNKNOWN_OBJECT));
				return node;
			}

			case Opcodes.CHECK_CAST: {
				ArgType castType = dex.getType(insn.getIndex());
				InsnNode node = new IndexInsnNode(method, InsnType.CHECK_CAST, castType, 1);
				node.setResult(InsnArg.reg(insn, 0, castType));
				node.addArg(InsnArg.reg(insn, 0, ArgType.UNKNOWN_OBJECT));
				return node;
			}

			case Opcodes.IGET:
			case Opcodes.IGET_BOOLEAN:
			case Opcodes.IGET_BYTE:
			case Opcodes.IGET_CHAR:
			case Opcodes.IGET_SHORT:
			case Opcodes.IGET_WIDE:
			case Opcodes.IGET_OBJECT: {
				FieldInfo field = FieldInfo.fromDex(dex, insn.getIndex());
				InsnNode node = new IndexInsnNode(method, InsnType.IGET, field, 1);
				node.setResult(InsnArg.reg(insn, 0, field.getType()));
				node.addArg(InsnArg.reg(insn, 1, field.getDeclClass().getType()));
				return node;
			}

			case Opcodes.IPUT:
			case Opcodes.IPUT_BOOLEAN:
			case Opcodes.IPUT_BYTE:
			case Opcodes.IPUT_CHAR:
			case Opcodes.IPUT_SHORT:
			case Opcodes.IPUT_WIDE:
			case Opcodes.IPUT_OBJECT: {
				FieldInfo field = FieldInfo.fromDex(dex, insn.getIndex());
				InsnNode node = new IndexInsnNode(method, InsnType.IPUT, field, 2);
				node.addArg(InsnArg.reg(insn, 0, field.getType()));
				node.addArg(InsnArg.reg(insn, 1, field.getDeclClass().getType()));
				return node;
			}

			case Opcodes.SGET:
			case Opcodes.SGET_BOOLEAN:
			case Opcodes.SGET_BYTE:
			case Opcodes.SGET_CHAR:
			case Opcodes.SGET_SHORT:
			case Opcodes.SGET_WIDE:
			case Opcodes.SGET_OBJECT: {
				FieldInfo field = FieldInfo.fromDex(dex, insn.getIndex());
				InsnNode node = new IndexInsnNode(method, InsnType.SGET, field, 0);
				node.setResult(InsnArg.reg(insn, 0, field.getType()));
				return node;
			}

			case Opcodes.SPUT:
			case Opcodes.SPUT_BOOLEAN:
			case Opcodes.SPUT_BYTE:
			case Opcodes.SPUT_CHAR:
			case Opcodes.SPUT_SHORT:
			case Opcodes.SPUT_WIDE:
			case Opcodes.SPUT_OBJECT: {
				FieldInfo field = FieldInfo.fromDex(dex, insn.getIndex());
				InsnNode node = new IndexInsnNode(method, InsnType.SPUT, field, 1);
				node.addArg(InsnArg.reg(insn, 0, field.getType()));
				return node;
			}

			case Opcodes.ARRAY_LENGTH: {
				InsnNode node = new InsnNode(method, InsnType.ARRAY_LENGTH, 1);
				node.setResult(InsnArg.reg(insn, 0, ArgType.INT));
				node.addArg(InsnArg.reg(insn, 1, ArgType.unknown(PrimitiveType.ARRAY)));
				return node;
			}

			case Opcodes.AGET:
				return arrayGet(insn, ArgType.NARROW);
			case Opcodes.AGET_BOOLEAN:
				return arrayGet(insn, ArgType.BOOLEAN);
			case Opcodes.AGET_BYTE:
				return arrayGet(insn, ArgType.BYTE);
			case Opcodes.AGET_CHAR:
				return arrayGet(insn, ArgType.CHAR);
			case Opcodes.AGET_SHORT:
				return arrayGet(insn, ArgType.SHORT);
			case Opcodes.AGET_WIDE:
				return arrayGet(insn, ArgType.WIDE);
			case Opcodes.AGET_OBJECT:
				return arrayGet(insn, ArgType.UNKNOWN_OBJECT);

			case Opcodes.APUT:
				return arrayPut(insn, ArgType.NARROW);
			case Opcodes.APUT_BOOLEAN:
				return arrayPut(insn, ArgType.BOOLEAN);
			case Opcodes.APUT_BYTE:
				return arrayPut(insn, ArgType.BYTE);
			case Opcodes.APUT_CHAR:
				return arrayPut(insn, ArgType.CHAR);
			case Opcodes.APUT_SHORT:
				return arrayPut(insn, ArgType.SHORT);
			case Opcodes.APUT_WIDE:
				return arrayPut(insn, ArgType.WIDE);
			case Opcodes.APUT_OBJECT:
				return arrayPut(insn, ArgType.UNKNOWN_OBJECT);

			case Opcodes.INVOKE_STATIC:
				return invoke(insn, offset, InvokeType.STATIC, false);

			case Opcodes.INVOKE_STATIC_RANGE:
				return invoke(insn, offset, InvokeType.STATIC, true);

			case Opcodes.INVOKE_DIRECT:
				return invoke(insn, offset, InvokeType.DIRECT, false);
			case Opcodes.INVOKE_INTERFACE:
				return invoke(insn, offset, InvokeType.INTERFACE, false);
			case Opcodes.INVOKE_SUPER:
				return invoke(insn, offset, InvokeType.SUPER, false);
			case Opcodes.INVOKE_VIRTUAL:
				return invoke(insn, offset, InvokeType.VIRTUAL, false);

			case Opcodes.INVOKE_DIRECT_RANGE:
				return invoke(insn, offset, InvokeType.DIRECT, true);
			case Opcodes.INVOKE_INTERFACE_RANGE:
				return invoke(insn, offset, InvokeType.INTERFACE, true);
			case Opcodes.INVOKE_SUPER_RANGE:
				return invoke(insn, offset, InvokeType.SUPER, true);
			case Opcodes.INVOKE_VIRTUAL_RANGE:
				return invoke(insn, offset, InvokeType.VIRTUAL, true);

			case Opcodes.NEW_INSTANCE:
				return insn(InsnType.NEW_INSTANCE,
						InsnArg.reg(insn, 0, dex.getType(insn.getIndex())));

			case Opcodes.NEW_ARRAY:
				return insn(InsnType.NEW_ARRAY,
						InsnArg.reg(insn, 0, dex.getType(insn.getIndex())),
						InsnArg.reg(insn, 1, ArgType.INT));

			case Opcodes.FILL_ARRAY_DATA:
				return fillArray(insn);

			case Opcodes.FILLED_NEW_ARRAY:
				return filledNewArray(insn, offset, false);
			case Opcodes.FILLED_NEW_ARRAY_RANGE:
				return filledNewArray(insn, offset, true);

			case Opcodes.PACKED_SWITCH:
				return decodeSwitch(insn, offset, true);

			case Opcodes.SPARSE_SWITCH:
				return decodeSwitch(insn, offset, false);

			case Opcodes.MONITOR_ENTER:
				return insn(InsnType.MONITOR_ENTER,
						null,
						InsnArg.reg(insn, 0, ArgType.UNKNOWN_OBJECT));

			case Opcodes.MONITOR_EXIT:
				return insn(InsnType.MONITOR_EXIT,
						null,
						InsnArg.reg(insn, 0, ArgType.UNKNOWN_OBJECT));
		}

		throw new DecodeException("Unknown instruction: " + OpcodeInfo.getName(insn.getOpcode()));
	}

	private InsnNode decodeSwitch(DecodedInstruction insn, int offset, boolean packed) {
		int payloadOffset = insn.getTarget();
		DecodedInstruction payload = insnArr[payloadOffset];
		int[] keys;
		int[] targets;
		if (packed) {
			PackedSwitchPayloadDecodedInstruction ps = (PackedSwitchPayloadDecodedInstruction) payload;
			targets = ps.getTargets();
			keys = new int[targets.length];
			int k = ps.getFirstKey();
			for (int i = 0; i < keys.length; i++)
				keys[i] = k++;
		} else {
			SparseSwitchPayloadDecodedInstruction ss = (SparseSwitchPayloadDecodedInstruction) payload;
			targets = ss.getTargets();
			keys = ss.getKeys();
		}
		// convert from relative to absolute offsets
		for (int i = 0; i < targets.length; i++) {
			targets[i] = targets[i] - payloadOffset + offset;
		}
		int nextOffset = getNextInsnOffset(insnArr, offset);
		return new SwitchNode(method, InsnArg.reg(insn, 0, ArgType.NARROW),
				keys, targets, nextOffset);
	}

	private InsnNode fillArray(DecodedInstruction insn) {
		DecodedInstruction payload = insnArr[insn.getTarget()];
		return new FillArrayOp(method, insn.getA(), (FillArrayDataPayloadDecodedInstruction) payload);
	}

	private InsnNode filledNewArray(DecodedInstruction insn, int offset, boolean isRange) {
		int resReg = getMoveResultRegister(insnArr, offset);
		ArgType arrType = dex.getType(insn.getIndex());
		ArgType elType = arrType.getArrayElement();
		InsnArg[] regs = new InsnArg[insn.getRegisterCount()];
		if (isRange) {
			int r = insn.getA();
			for (int i = 0; i < insn.getRegisterCount(); i++) {
				regs[i] = InsnArg.reg(r, elType);
				r++;
			}
		} else {
			for (int i = 0; i < insn.getRegisterCount(); i++)
				regs[i] = InsnArg.reg(insn, i, elType);
		}
		return insn(InsnType.FILLED_NEW_ARRAY,
				resReg == -1 ? null : InsnArg.reg(resReg, arrType),
				regs);
	}

	private InsnNode cmp(DecodedInstruction insn, InsnType itype, ArgType argType) {
		InsnNode inode = new InsnNode(method, itype, 2);
		inode.setResult(InsnArg.reg(insn, 0, ArgType.INT));
		inode.addArg(InsnArg.reg(insn, 1, argType));
		inode.addArg(InsnArg.reg(insn, 2, argType));
		return inode;
	}

	private InsnNode cast(DecodedInstruction insn, ArgType from, ArgType to) {
		InsnNode inode = new IndexInsnNode(method, InsnType.CAST, to, 1);
		inode.setResult(InsnArg.reg(insn, 0, to));
		inode.addArg(InsnArg.reg(insn, 1, from));
		return inode;
	}

	private InsnNode invoke(DecodedInstruction insn, int offset, InvokeType type, boolean isRange) {
		int resReg = getMoveResultRegister(insnArr, offset);
		return new InvokeNode(method, insn, type, isRange, resReg);
	}

	private InsnNode arrayGet(DecodedInstruction insn, ArgType argType) {
		InsnNode inode = new InsnNode(method, InsnType.AGET, 2);
		inode.setResult(InsnArg.reg(insn, 0, argType));
		inode.addArg(InsnArg.reg(insn, 1, ArgType.unknown(PrimitiveType.ARRAY)));
		inode.addArg(InsnArg.reg(insn, 2, ArgType.INT));
		return inode;
	}

	private InsnNode arrayPut(DecodedInstruction insn, ArgType argType) {
		InsnNode inode = new InsnNode(method, InsnType.APUT, 3);
		inode.addArg(InsnArg.reg(insn, 1, ArgType.unknown(PrimitiveType.ARRAY)));
		inode.addArg(InsnArg.reg(insn, 2, ArgType.INT));
		inode.addArg(InsnArg.reg(insn, 0, argType));
		return inode;
	}

	private InsnNode arith(DecodedInstruction insn, ArithOp op, ArgType type) {
		return new ArithNode(method, insn, op, type, false);
	}

	private InsnNode arith_lit(DecodedInstruction insn, ArithOp op, ArgType type) {
		return new ArithNode(method, insn, op, type, true);
	}

	private InsnNode neg(DecodedInstruction insn, ArgType type) {
		InsnNode inode = new InsnNode(method, InsnType.NEG, 1);
		inode.setResult(InsnArg.reg(insn, 0, type));
		inode.addArg(InsnArg.reg(insn, 1, type));
		return inode;
	}

	private InsnNode insn(InsnType type, RegisterArg res, InsnArg... args) {
		InsnNode inode = new InsnNode(method, type, args == null ? 0 : args.length);
		inode.setResult(res);
		if (args != null)
			for (InsnArg arg : args)
				inode.addArg(arg);
		return inode;
	}

	private int getMoveResultRegister(DecodedInstruction[] insnArr, int offset) {
		int nextOffset = getNextInsnOffset(insnArr, offset);
		if (nextOffset >= 0) {
			DecodedInstruction next = insnArr[nextOffset];
			int opc = next.getOpcode();
			if (opc == Opcodes.MOVE_RESULT
					|| opc == Opcodes.MOVE_RESULT_WIDE
					|| opc == Opcodes.MOVE_RESULT_OBJECT) {
				return next.getA();
			}
		}
		return -1;
	}

	public static int getPrevInsnOffset(Object[] insnArr, int offset) {
		int i = offset - 1;
		while (i >= 0 && insnArr[i] == null)
			i--;
		if (i < 0)
			return -1;
		return i;
	}

	public static int getNextInsnOffset(Object[] insnArr, int offset) {
		int i = offset + 1;
		while (i < insnArr.length && insnArr[i] == null)
			i++;
		if (i >= insnArr.length)
			return -1;
		return i;
	}
}
