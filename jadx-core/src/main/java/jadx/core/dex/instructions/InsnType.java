package jadx.core.dex.instructions;

public enum InsnType {
	NOP, // replacement for removed instructions

	CONST,
	CONST_STR,
	CONST_CLASS,

	ARITH,
	NEG,

	MOVE,
	CAST,

	RETURN,
	GOTO,

	THROW,
	MOVE_EXCEPTION,

	CMP_L,
	CMP_G,
	IF,
	SWITCH,

	MONITOR_ENTER,
	MONITOR_EXIT,

	CHECK_CAST,
	INSTANCE_OF,

	ARRAY_LENGTH,
	FILL_ARRAY,
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

	// additional instructions
	CONSTRUCTOR,
	BREAK,
	CONTINUE,

	STR_CONCAT, // strings concatenation
	ARITH_ONEARG,

	TERNARY,
	ARGS, // just generate arguments

	NEW_MULTIDIM_ARRAY // TODO: now multidimensional arrays created using Array.newInstance function
}
