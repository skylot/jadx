package jadx.core.dex.instructions;

public enum InsnType {

	CONST,
	CONST_STR,
	CONST_CLASS,

	ARITH,
	NEG,
	NOT,

	MOVE,
	MOVE_MULTI,
	CAST,

	RETURN,
	GOTO,

	THROW,
	MOVE_EXCEPTION,

	CMP_L,
	CMP_G,
	IF,
	SWITCH,
	SWITCH_DATA,

	MONITOR_ENTER,
	MONITOR_EXIT,

	CHECK_CAST,
	INSTANCE_OF,

	ARRAY_LENGTH,
	FILL_ARRAY,
	FILL_ARRAY_DATA,
	FILLED_NEW_ARRAY,

	AGET,
	APUT,

	NEW_ARRAY,
	NEW_INSTANCE,

	IGET,
	IPUT,

	SGET,
	SPUT,

	INVOKE,
	MOVE_RESULT,

	// *** Additional instructions ***

	// replacement for removed instructions
	NOP,

	TERNARY,
	CONSTRUCTOR,

	BREAK,
	CONTINUE,

	// strings concatenation
	STR_CONCAT,

	// just generate one argument
	ONE_ARG,
	PHI,

	// fake insn to keep arguments which will be used in regions codegen
	REGION_ARG
}
