package jadx.core.dex.attributes;

public enum AFlag {
	TRY_ENTER,
	TRY_LEAVE,

	LOOP_START,
	LOOP_END,

	SYNTHETIC,
	FINAL, // SSAVar attribute for make var final

	RETURN, // block contains only return instruction
	ORIG_RETURN,

	DECLARE_VAR,
	DONT_WRAP,

	DONT_SHRINK,
	DONT_INLINE,
	DONT_GENERATE,
	SKIP,
	REMOVE,

	SKIP_FIRST_ARG,
	SKIP_ARG, // skip argument in invoke call
	ANONYMOUS_CONSTRUCTOR,
	ANONYMOUS_CLASS,
	THIS,

	ELSE_IF_CHAIN,

	WRAPPED,
	ARITH_ONEARG,

	FALL_THROUGH,

	INCONSISTENT_CODE, // warning about incorrect decompilation
}
