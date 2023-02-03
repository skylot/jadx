package jadx.plugins.input.dex.insns;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.insns.InsnIndexType;
import jadx.api.plugins.input.insns.Opcode;

public class DexInsnInfo {

	private static final DexInsnInfo[] INSN_INFO;
	private static final Map<Integer, DexInsnInfo> PAYLOAD_INFO;

	static {
		DexInsnInfo[] arr = new DexInsnInfo[0x100];
		INSN_INFO = arr;
		register(arr, DexOpcodes.NOP, Opcode.NOP, DexInsnFormat.FORMAT_10X);

		register(arr, DexOpcodes.MOVE, Opcode.MOVE, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.MOVE_FROM16, Opcode.MOVE, DexInsnFormat.FORMAT_22X);
		register(arr, DexOpcodes.MOVE_16, Opcode.MOVE, DexInsnFormat.FORMAT_32X);

		register(arr, DexOpcodes.MOVE_WIDE, Opcode.MOVE_WIDE, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.MOVE_WIDE_FROM16, Opcode.MOVE_WIDE, DexInsnFormat.FORMAT_22X);
		register(arr, DexOpcodes.MOVE_WIDE_16, Opcode.MOVE_WIDE, DexInsnFormat.FORMAT_32X);

		register(arr, DexOpcodes.MOVE_OBJECT, Opcode.MOVE_OBJECT, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.MOVE_OBJECT_FROM16, Opcode.MOVE_OBJECT, DexInsnFormat.FORMAT_22X);
		register(arr, DexOpcodes.MOVE_OBJECT_16, Opcode.MOVE_OBJECT, DexInsnFormat.FORMAT_32X);

		register(arr, DexOpcodes.MOVE_RESULT, Opcode.MOVE_RESULT, DexInsnFormat.FORMAT_11X);
		register(arr, DexOpcodes.MOVE_RESULT_WIDE, Opcode.MOVE_RESULT, DexInsnFormat.FORMAT_11X);
		register(arr, DexOpcodes.MOVE_RESULT_OBJECT, Opcode.MOVE_RESULT, DexInsnFormat.FORMAT_11X);

		register(arr, DexOpcodes.MOVE_EXCEPTION, Opcode.MOVE_EXCEPTION, DexInsnFormat.FORMAT_11X);

		register(arr, DexOpcodes.RETURN_VOID, Opcode.RETURN_VOID, DexInsnFormat.FORMAT_10X);
		register(arr, DexOpcodes.RETURN, Opcode.RETURN, DexInsnFormat.FORMAT_11X);
		register(arr, DexOpcodes.RETURN_WIDE, Opcode.RETURN, DexInsnFormat.FORMAT_11X);
		register(arr, DexOpcodes.RETURN_OBJECT, Opcode.RETURN, DexInsnFormat.FORMAT_11X);

		register(arr, DexOpcodes.CONST_4, Opcode.CONST, DexInsnFormat.FORMAT_11N);
		register(arr, DexOpcodes.CONST_16, Opcode.CONST, DexInsnFormat.FORMAT_21S);
		register(arr, DexOpcodes.CONST, Opcode.CONST, DexInsnFormat.FORMAT_31I);
		register(arr, DexOpcodes.CONST_HIGH16, Opcode.CONST, DexInsnFormat.FORMAT_21H);

		register(arr, DexOpcodes.CONST_WIDE_16, Opcode.CONST_WIDE, DexInsnFormat.FORMAT_21S);
		register(arr, DexOpcodes.CONST_WIDE_32, Opcode.CONST_WIDE, DexInsnFormat.FORMAT_31I);
		register(arr, DexOpcodes.CONST_WIDE, Opcode.CONST_WIDE, DexInsnFormat.FORMAT_51I);
		register(arr, DexOpcodes.CONST_WIDE_HIGH16, Opcode.CONST_WIDE, DexInsnFormat.FORMAT_21H);

		register(arr, DexOpcodes.CONST_STRING, Opcode.CONST_STRING, DexInsnFormat.FORMAT_21C, InsnIndexType.STRING_REF);
		register(arr, DexOpcodes.CONST_STRING_JUMBO, Opcode.CONST_STRING, DexInsnFormat.FORMAT_31C, InsnIndexType.STRING_REF);

		register(arr, DexOpcodes.CONST_CLASS, Opcode.CONST_CLASS, DexInsnFormat.FORMAT_21C, InsnIndexType.TYPE_REF);

		register(arr, DexOpcodes.MONITOR_ENTER, Opcode.MONITOR_ENTER, DexInsnFormat.FORMAT_11X);
		register(arr, DexOpcodes.MONITOR_EXIT, Opcode.MONITOR_EXIT, DexInsnFormat.FORMAT_11X);

		register(arr, DexOpcodes.CHECK_CAST, Opcode.CHECK_CAST, DexInsnFormat.FORMAT_21C, InsnIndexType.TYPE_REF);
		register(arr, DexOpcodes.INSTANCE_OF, Opcode.INSTANCE_OF, DexInsnFormat.FORMAT_22C, InsnIndexType.TYPE_REF);
		register(arr, DexOpcodes.ARRAY_LENGTH, Opcode.ARRAY_LENGTH, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.NEW_INSTANCE, Opcode.NEW_INSTANCE, DexInsnFormat.FORMAT_21C, InsnIndexType.TYPE_REF);
		register(arr, DexOpcodes.NEW_ARRAY, Opcode.NEW_ARRAY, DexInsnFormat.FORMAT_22C, InsnIndexType.TYPE_REF);

		register(arr, DexOpcodes.FILLED_NEW_ARRAY, Opcode.FILLED_NEW_ARRAY, DexInsnFormat.FORMAT_35C, InsnIndexType.TYPE_REF);
		register(arr, DexOpcodes.FILLED_NEW_ARRAY_RANGE, Opcode.FILLED_NEW_ARRAY_RANGE, DexInsnFormat.FORMAT_3RC, InsnIndexType.TYPE_REF);
		register(arr, DexOpcodes.FILL_ARRAY_DATA, Opcode.FILL_ARRAY_DATA, DexInsnFormat.FORMAT_31T);

		register(arr, DexOpcodes.THROW, Opcode.THROW, DexInsnFormat.FORMAT_11X);

		register(arr, DexOpcodes.GOTO, Opcode.GOTO, DexInsnFormat.FORMAT_10T);
		register(arr, DexOpcodes.GOTO_16, Opcode.GOTO, DexInsnFormat.FORMAT_20T);
		register(arr, DexOpcodes.GOTO_32, Opcode.GOTO, DexInsnFormat.FORMAT_30T);

		register(arr, DexOpcodes.PACKED_SWITCH, Opcode.PACKED_SWITCH, DexInsnFormat.FORMAT_31T);
		register(arr, DexOpcodes.SPARSE_SWITCH, Opcode.SPARSE_SWITCH, DexInsnFormat.FORMAT_31T);

		register(arr, DexOpcodes.CMPL_FLOAT, Opcode.CMPL_FLOAT, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.CMPG_FLOAT, Opcode.CMPG_FLOAT, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.CMPL_DOUBLE, Opcode.CMPL_DOUBLE, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.CMPG_DOUBLE, Opcode.CMPG_DOUBLE, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.CMP_LONG, Opcode.CMP_LONG, DexInsnFormat.FORMAT_23X);

		register(arr, DexOpcodes.IF_EQ, Opcode.IF_EQ, DexInsnFormat.FORMAT_22T);
		register(arr, DexOpcodes.IF_NE, Opcode.IF_NE, DexInsnFormat.FORMAT_22T);
		register(arr, DexOpcodes.IF_LT, Opcode.IF_LT, DexInsnFormat.FORMAT_22T);
		register(arr, DexOpcodes.IF_GE, Opcode.IF_GE, DexInsnFormat.FORMAT_22T);
		register(arr, DexOpcodes.IF_GT, Opcode.IF_GT, DexInsnFormat.FORMAT_22T);
		register(arr, DexOpcodes.IF_LE, Opcode.IF_LE, DexInsnFormat.FORMAT_22T);

		register(arr, DexOpcodes.IF_EQZ, Opcode.IF_EQZ, DexInsnFormat.FORMAT_21T);
		register(arr, DexOpcodes.IF_NEZ, Opcode.IF_NEZ, DexInsnFormat.FORMAT_21T);
		register(arr, DexOpcodes.IF_LTZ, Opcode.IF_LTZ, DexInsnFormat.FORMAT_21T);
		register(arr, DexOpcodes.IF_GEZ, Opcode.IF_GEZ, DexInsnFormat.FORMAT_21T);
		register(arr, DexOpcodes.IF_GTZ, Opcode.IF_GTZ, DexInsnFormat.FORMAT_21T);
		register(arr, DexOpcodes.IF_LEZ, Opcode.IF_LEZ, DexInsnFormat.FORMAT_21T);

		register(arr, DexOpcodes.AGET, Opcode.AGET, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.AGET_WIDE, Opcode.AGET_WIDE, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.AGET_OBJECT, Opcode.AGET_OBJECT, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.AGET_BOOLEAN, Opcode.AGET_BOOLEAN, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.AGET_BYTE, Opcode.AGET_BYTE, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.AGET_CHAR, Opcode.AGET_CHAR, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.AGET_SHORT, Opcode.AGET_SHORT, DexInsnFormat.FORMAT_23X);

		register(arr, DexOpcodes.APUT, Opcode.APUT, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.APUT_WIDE, Opcode.APUT_WIDE, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.APUT_OBJECT, Opcode.APUT_OBJECT, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.APUT_BOOLEAN, Opcode.APUT_BOOLEAN, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.APUT_BYTE, Opcode.APUT_BYTE, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.APUT_CHAR, Opcode.APUT_CHAR, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.APUT_SHORT, Opcode.APUT_SHORT, DexInsnFormat.FORMAT_23X);

		register(arr, DexOpcodes.IGET, Opcode.IGET, DexInsnFormat.FORMAT_22C, InsnIndexType.FIELD_REF);
		register(arr, DexOpcodes.IGET_WIDE, Opcode.IGET, DexInsnFormat.FORMAT_22C, InsnIndexType.FIELD_REF);
		register(arr, DexOpcodes.IGET_OBJECT, Opcode.IGET, DexInsnFormat.FORMAT_22C, InsnIndexType.FIELD_REF);
		register(arr, DexOpcodes.IGET_BOOLEAN, Opcode.IGET, DexInsnFormat.FORMAT_22C, InsnIndexType.FIELD_REF);
		register(arr, DexOpcodes.IGET_BYTE, Opcode.IGET, DexInsnFormat.FORMAT_22C, InsnIndexType.FIELD_REF);
		register(arr, DexOpcodes.IGET_CHAR, Opcode.IGET, DexInsnFormat.FORMAT_22C, InsnIndexType.FIELD_REF);
		register(arr, DexOpcodes.IGET_SHORT, Opcode.IGET, DexInsnFormat.FORMAT_22C, InsnIndexType.FIELD_REF);

		register(arr, DexOpcodes.IPUT, Opcode.IPUT, DexInsnFormat.FORMAT_22C, InsnIndexType.FIELD_REF);
		register(arr, DexOpcodes.IPUT_WIDE, Opcode.IPUT, DexInsnFormat.FORMAT_22C, InsnIndexType.FIELD_REF);
		register(arr, DexOpcodes.IPUT_OBJECT, Opcode.IPUT, DexInsnFormat.FORMAT_22C, InsnIndexType.FIELD_REF);
		register(arr, DexOpcodes.IPUT_BOOLEAN, Opcode.IPUT, DexInsnFormat.FORMAT_22C, InsnIndexType.FIELD_REF);
		register(arr, DexOpcodes.IPUT_BYTE, Opcode.IPUT, DexInsnFormat.FORMAT_22C, InsnIndexType.FIELD_REF);
		register(arr, DexOpcodes.IPUT_CHAR, Opcode.IPUT, DexInsnFormat.FORMAT_22C, InsnIndexType.FIELD_REF);
		register(arr, DexOpcodes.IPUT_SHORT, Opcode.IPUT, DexInsnFormat.FORMAT_22C, InsnIndexType.FIELD_REF);

		register(arr, DexOpcodes.SGET, Opcode.SGET, DexInsnFormat.FORMAT_21C, InsnIndexType.FIELD_REF);
		register(arr, DexOpcodes.SGET_WIDE, Opcode.SGET, DexInsnFormat.FORMAT_21C, InsnIndexType.FIELD_REF);
		register(arr, DexOpcodes.SGET_OBJECT, Opcode.SGET, DexInsnFormat.FORMAT_21C, InsnIndexType.FIELD_REF);
		register(arr, DexOpcodes.SGET_BOOLEAN, Opcode.SGET, DexInsnFormat.FORMAT_21C, InsnIndexType.FIELD_REF);
		register(arr, DexOpcodes.SGET_BYTE, Opcode.SGET, DexInsnFormat.FORMAT_21C, InsnIndexType.FIELD_REF);
		register(arr, DexOpcodes.SGET_CHAR, Opcode.SGET, DexInsnFormat.FORMAT_21C, InsnIndexType.FIELD_REF);
		register(arr, DexOpcodes.SGET_SHORT, Opcode.SGET, DexInsnFormat.FORMAT_21C, InsnIndexType.FIELD_REF);

		register(arr, DexOpcodes.SPUT, Opcode.SPUT, DexInsnFormat.FORMAT_21C, InsnIndexType.FIELD_REF);
		register(arr, DexOpcodes.SPUT_WIDE, Opcode.SPUT, DexInsnFormat.FORMAT_21C, InsnIndexType.FIELD_REF);
		register(arr, DexOpcodes.SPUT_OBJECT, Opcode.SPUT, DexInsnFormat.FORMAT_21C, InsnIndexType.FIELD_REF);
		register(arr, DexOpcodes.SPUT_BOOLEAN, Opcode.SPUT, DexInsnFormat.FORMAT_21C, InsnIndexType.FIELD_REF);
		register(arr, DexOpcodes.SPUT_BYTE, Opcode.SPUT, DexInsnFormat.FORMAT_21C, InsnIndexType.FIELD_REF);
		register(arr, DexOpcodes.SPUT_CHAR, Opcode.SPUT, DexInsnFormat.FORMAT_21C, InsnIndexType.FIELD_REF);
		register(arr, DexOpcodes.SPUT_SHORT, Opcode.SPUT, DexInsnFormat.FORMAT_21C, InsnIndexType.FIELD_REF);

		register(arr, DexOpcodes.INVOKE_VIRTUAL, Opcode.INVOKE_VIRTUAL, DexInsnFormat.FORMAT_35C, InsnIndexType.METHOD_REF);
		register(arr, DexOpcodes.INVOKE_SUPER, Opcode.INVOKE_SUPER, DexInsnFormat.FORMAT_35C, InsnIndexType.METHOD_REF);
		register(arr, DexOpcodes.INVOKE_DIRECT, Opcode.INVOKE_DIRECT, DexInsnFormat.FORMAT_35C, InsnIndexType.METHOD_REF);
		register(arr, DexOpcodes.INVOKE_STATIC, Opcode.INVOKE_STATIC, DexInsnFormat.FORMAT_35C, InsnIndexType.METHOD_REF);
		register(arr, DexOpcodes.INVOKE_INTERFACE, Opcode.INVOKE_INTERFACE, DexInsnFormat.FORMAT_35C, InsnIndexType.METHOD_REF);

		register(arr, DexOpcodes.INVOKE_VIRTUAL_RANGE, Opcode.INVOKE_VIRTUAL_RANGE, DexInsnFormat.FORMAT_3RC, InsnIndexType.METHOD_REF);
		register(arr, DexOpcodes.INVOKE_SUPER_RANGE, Opcode.INVOKE_SUPER_RANGE, DexInsnFormat.FORMAT_3RC, InsnIndexType.METHOD_REF);
		register(arr, DexOpcodes.INVOKE_DIRECT_RANGE, Opcode.INVOKE_DIRECT_RANGE, DexInsnFormat.FORMAT_3RC, InsnIndexType.METHOD_REF);
		register(arr, DexOpcodes.INVOKE_STATIC_RANGE, Opcode.INVOKE_STATIC_RANGE, DexInsnFormat.FORMAT_3RC, InsnIndexType.METHOD_REF);
		register(arr, DexOpcodes.INVOKE_INTERFACE_RANGE, Opcode.INVOKE_INTERFACE_RANGE, DexInsnFormat.FORMAT_3RC, InsnIndexType.METHOD_REF);

		register(arr, DexOpcodes.NEG_INT, Opcode.NEG_INT, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.NOT_INT, Opcode.NOT_INT, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.NEG_LONG, Opcode.NEG_LONG, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.NOT_LONG, Opcode.NOT_LONG, DexInsnFormat.FORMAT_12X);

		register(arr, DexOpcodes.NEG_FLOAT, Opcode.NEG_FLOAT, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.NEG_DOUBLE, Opcode.NEG_DOUBLE, DexInsnFormat.FORMAT_12X);

		register(arr, DexOpcodes.INT_TO_LONG, Opcode.INT_TO_LONG, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.INT_TO_FLOAT, Opcode.INT_TO_FLOAT, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.INT_TO_DOUBLE, Opcode.INT_TO_DOUBLE, DexInsnFormat.FORMAT_12X);

		register(arr, DexOpcodes.LONG_TO_INT, Opcode.LONG_TO_INT, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.LONG_TO_FLOAT, Opcode.LONG_TO_FLOAT, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.LONG_TO_DOUBLE, Opcode.LONG_TO_DOUBLE, DexInsnFormat.FORMAT_12X);

		register(arr, DexOpcodes.FLOAT_TO_INT, Opcode.FLOAT_TO_INT, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.FLOAT_TO_LONG, Opcode.FLOAT_TO_LONG, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.FLOAT_TO_DOUBLE, Opcode.FLOAT_TO_DOUBLE, DexInsnFormat.FORMAT_12X);

		register(arr, DexOpcodes.DOUBLE_TO_INT, Opcode.DOUBLE_TO_INT, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.DOUBLE_TO_LONG, Opcode.DOUBLE_TO_LONG, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.DOUBLE_TO_FLOAT, Opcode.DOUBLE_TO_FLOAT, DexInsnFormat.FORMAT_12X);

		register(arr, DexOpcodes.INT_TO_BYTE, Opcode.INT_TO_BYTE, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.INT_TO_CHAR, Opcode.INT_TO_CHAR, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.INT_TO_SHORT, Opcode.INT_TO_SHORT, DexInsnFormat.FORMAT_12X);

		register(arr, DexOpcodes.ADD_INT, Opcode.ADD_INT, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.SUB_INT, Opcode.SUB_INT, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.MUL_INT, Opcode.MUL_INT, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.DIV_INT, Opcode.DIV_INT, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.REM_INT, Opcode.REM_INT, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.AND_INT, Opcode.AND_INT, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.OR_INT, Opcode.OR_INT, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.XOR_INT, Opcode.XOR_INT, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.SHL_INT, Opcode.SHL_INT, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.SHR_INT, Opcode.SHR_INT, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.USHR_INT, Opcode.USHR_INT, DexInsnFormat.FORMAT_23X);

		register(arr, DexOpcodes.ADD_LONG, Opcode.ADD_LONG, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.SUB_LONG, Opcode.SUB_LONG, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.MUL_LONG, Opcode.MUL_LONG, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.DIV_LONG, Opcode.DIV_LONG, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.REM_LONG, Opcode.REM_LONG, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.AND_LONG, Opcode.AND_LONG, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.OR_LONG, Opcode.OR_LONG, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.XOR_LONG, Opcode.XOR_LONG, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.SHL_LONG, Opcode.SHL_LONG, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.SHR_LONG, Opcode.SHR_LONG, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.USHR_LONG, Opcode.USHR_LONG, DexInsnFormat.FORMAT_23X);

		register(arr, DexOpcodes.ADD_FLOAT, Opcode.ADD_FLOAT, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.SUB_FLOAT, Opcode.SUB_FLOAT, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.MUL_FLOAT, Opcode.MUL_FLOAT, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.DIV_FLOAT, Opcode.DIV_FLOAT, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.REM_FLOAT, Opcode.REM_FLOAT, DexInsnFormat.FORMAT_23X);

		register(arr, DexOpcodes.ADD_DOUBLE, Opcode.ADD_DOUBLE, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.SUB_DOUBLE, Opcode.SUB_DOUBLE, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.MUL_DOUBLE, Opcode.MUL_DOUBLE, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.DIV_DOUBLE, Opcode.DIV_DOUBLE, DexInsnFormat.FORMAT_23X);
		register(arr, DexOpcodes.REM_DOUBLE, Opcode.REM_DOUBLE, DexInsnFormat.FORMAT_23X);

		register(arr, DexOpcodes.ADD_INT_2ADDR, Opcode.ADD_INT, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.SUB_INT_2ADDR, Opcode.SUB_INT, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.MUL_INT_2ADDR, Opcode.MUL_INT, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.DIV_INT_2ADDR, Opcode.DIV_INT, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.REM_INT_2ADDR, Opcode.REM_INT, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.AND_INT_2ADDR, Opcode.AND_INT, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.OR_INT_2ADDR, Opcode.OR_INT, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.XOR_INT_2ADDR, Opcode.XOR_INT, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.SHL_INT_2ADDR, Opcode.SHL_INT, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.SHR_INT_2ADDR, Opcode.SHR_INT, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.USHR_INT_2ADDR, Opcode.USHR_INT, DexInsnFormat.FORMAT_12X);

		register(arr, DexOpcodes.ADD_LONG_2ADDR, Opcode.ADD_LONG, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.SUB_LONG_2ADDR, Opcode.SUB_LONG, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.MUL_LONG_2ADDR, Opcode.MUL_LONG, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.DIV_LONG_2ADDR, Opcode.DIV_LONG, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.REM_LONG_2ADDR, Opcode.REM_LONG, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.AND_LONG_2ADDR, Opcode.AND_LONG, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.OR_LONG_2ADDR, Opcode.OR_LONG, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.XOR_LONG_2ADDR, Opcode.XOR_LONG, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.SHL_LONG_2ADDR, Opcode.SHL_LONG, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.SHR_LONG_2ADDR, Opcode.SHR_LONG, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.USHR_LONG_2ADDR, Opcode.USHR_LONG, DexInsnFormat.FORMAT_12X);

		register(arr, DexOpcodes.ADD_FLOAT_2ADDR, Opcode.ADD_FLOAT, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.SUB_FLOAT_2ADDR, Opcode.SUB_FLOAT, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.MUL_FLOAT_2ADDR, Opcode.MUL_FLOAT, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.DIV_FLOAT_2ADDR, Opcode.DIV_FLOAT, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.REM_FLOAT_2ADDR, Opcode.REM_FLOAT, DexInsnFormat.FORMAT_12X);

		register(arr, DexOpcodes.ADD_DOUBLE_2ADDR, Opcode.ADD_DOUBLE, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.SUB_DOUBLE_2ADDR, Opcode.SUB_DOUBLE, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.MUL_DOUBLE_2ADDR, Opcode.MUL_DOUBLE, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.DIV_DOUBLE_2ADDR, Opcode.DIV_DOUBLE, DexInsnFormat.FORMAT_12X);
		register(arr, DexOpcodes.REM_DOUBLE_2ADDR, Opcode.REM_DOUBLE, DexInsnFormat.FORMAT_12X);

		register(arr, DexOpcodes.ADD_INT_LIT16, Opcode.ADD_INT_LIT, DexInsnFormat.FORMAT_22S);
		register(arr, DexOpcodes.RSUB_INT, Opcode.RSUB_INT, DexInsnFormat.FORMAT_22S);
		register(arr, DexOpcodes.MUL_INT_LIT16, Opcode.MUL_INT_LIT, DexInsnFormat.FORMAT_22S);
		register(arr, DexOpcodes.DIV_INT_LIT16, Opcode.DIV_INT_LIT, DexInsnFormat.FORMAT_22S);
		register(arr, DexOpcodes.REM_INT_LIT16, Opcode.REM_INT_LIT, DexInsnFormat.FORMAT_22S);
		register(arr, DexOpcodes.AND_INT_LIT16, Opcode.AND_INT_LIT, DexInsnFormat.FORMAT_22S);
		register(arr, DexOpcodes.OR_INT_LIT16, Opcode.OR_INT_LIT, DexInsnFormat.FORMAT_22S);
		register(arr, DexOpcodes.XOR_INT_LIT16, Opcode.XOR_INT_LIT, DexInsnFormat.FORMAT_22S);

		register(arr, DexOpcodes.ADD_INT_LIT8, Opcode.ADD_INT_LIT, DexInsnFormat.FORMAT_22B);
		register(arr, DexOpcodes.RSUB_INT_LIT8, Opcode.RSUB_INT, DexInsnFormat.FORMAT_22B);
		register(arr, DexOpcodes.MUL_INT_LIT8, Opcode.MUL_INT_LIT, DexInsnFormat.FORMAT_22B);
		register(arr, DexOpcodes.DIV_INT_LIT8, Opcode.DIV_INT_LIT, DexInsnFormat.FORMAT_22B);
		register(arr, DexOpcodes.REM_INT_LIT8, Opcode.REM_INT_LIT, DexInsnFormat.FORMAT_22B);
		register(arr, DexOpcodes.AND_INT_LIT8, Opcode.AND_INT_LIT, DexInsnFormat.FORMAT_22B);
		register(arr, DexOpcodes.OR_INT_LIT8, Opcode.OR_INT_LIT, DexInsnFormat.FORMAT_22B);
		register(arr, DexOpcodes.XOR_INT_LIT8, Opcode.XOR_INT_LIT, DexInsnFormat.FORMAT_22B);
		register(arr, DexOpcodes.SHL_INT_LIT8, Opcode.SHL_INT_LIT, DexInsnFormat.FORMAT_22B);
		register(arr, DexOpcodes.SHR_INT_LIT8, Opcode.SHR_INT_LIT, DexInsnFormat.FORMAT_22B);
		register(arr, DexOpcodes.USHR_INT_LIT8, Opcode.USHR_INT_LIT, DexInsnFormat.FORMAT_22B);

		register(arr, DexOpcodes.INVOKE_POLYMORPHIC, Opcode.INVOKE_POLYMORPHIC, DexInsnFormat.FORMAT_45CC, InsnIndexType.METHOD_REF);
		register(arr, DexOpcodes.INVOKE_POLYMORPHIC_RANGE, Opcode.INVOKE_POLYMORPHIC_RANGE, DexInsnFormat.FORMAT_4RCC,
				InsnIndexType.METHOD_REF);

		register(arr, DexOpcodes.INVOKE_CUSTOM, Opcode.INVOKE_CUSTOM, DexInsnFormat.FORMAT_35C, InsnIndexType.CALL_SITE);
		register(arr, DexOpcodes.INVOKE_CUSTOM_RANGE, Opcode.INVOKE_CUSTOM_RANGE, DexInsnFormat.FORMAT_3RC, InsnIndexType.CALL_SITE);

		register(arr, DexOpcodes.CONST_METHOD_HANDLE, Opcode.CONST_METHOD_HANDLE, DexInsnFormat.FORMAT_21C);
		register(arr, DexOpcodes.CONST_METHOD_TYPE, Opcode.CONST_METHOD_TYPE, DexInsnFormat.FORMAT_21C);

		PAYLOAD_INFO = new ConcurrentHashMap<>(3);
		registerPayload(DexOpcodes.PACKED_SWITCH_PAYLOAD, Opcode.PACKED_SWITCH_PAYLOAD, DexInsnFormat.FORMAT_PACKED_SWITCH_PAYLOAD);
		registerPayload(DexOpcodes.SPARSE_SWITCH_PAYLOAD, Opcode.SPARSE_SWITCH_PAYLOAD, DexInsnFormat.FORMAT_SPARSE_SWITCH_PAYLOAD);
		registerPayload(DexOpcodes.FILL_ARRAY_DATA_PAYLOAD, Opcode.FILL_ARRAY_DATA_PAYLOAD, DexInsnFormat.FORMAT_FILL_ARRAY_DATA_PAYLOAD);
	}

	private static void register(DexInsnInfo[] arr, int opcode, Opcode apiOpcode, DexInsnFormat format) {
		arr[opcode] = new DexInsnInfo(opcode, apiOpcode, format, InsnIndexType.NONE);
	}

	private static void register(DexInsnInfo[] arr, int opcode, Opcode apiOpcode, DexInsnFormat format, InsnIndexType indexType) {
		arr[opcode] = new DexInsnInfo(opcode, apiOpcode, format, indexType);
	}

	private static void registerPayload(int opcode, Opcode apiOpcode, DexInsnFormat format) {
		PAYLOAD_INFO.put(opcode, new DexInsnInfo(opcode, apiOpcode, format, InsnIndexType.NONE));
	}

	@Nullable
	public static DexInsnInfo get(int opcodeUnit) {
		int opcode = opcodeUnit & 0xFF;
		if (opcode == 0 && opcodeUnit != 0) {
			return PAYLOAD_INFO.get(opcodeUnit);
		}
		return INSN_INFO[opcode];
	}

	private final int opcode;
	private final Opcode apiOpcode;
	private final DexInsnFormat format;
	private final InsnIndexType indexType;

	public DexInsnInfo(int opcode, Opcode apiOpcode, DexInsnFormat format, InsnIndexType indexType) {
		this.opcode = opcode;
		this.apiOpcode = apiOpcode;
		this.format = format;
		this.indexType = indexType;
	}

	public int getOpcode() {
		return opcode;
	}

	public Opcode getApiOpcode() {
		return apiOpcode;
	}

	public DexInsnFormat getFormat() {
		return format;
	}

	public InsnIndexType getIndexType() {
		return indexType;
	}

	@Override
	public String toString() {
		return String.format("0x%X :%d%d", opcode, format.getLength(), format.getRegsCount());
	}
}
