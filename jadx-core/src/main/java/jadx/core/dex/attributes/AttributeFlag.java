package jadx.core.dex.attributes;

public enum AttributeFlag {
	TRY_ENTER,
	TRY_LEAVE,

	LOOP_START,
	LOOP_END,

	SYNTHETIC,

	BREAK,
	RETURN, // block contains only return instruction

	DECLARE_VAR,

	DONT_SHRINK,
	DONT_GENERATE,
	SKIP,

	SKIP_FIRST_ARG,
	ANONYMOUS_CONSTRUCTOR,

	ELSE_IF_CHAIN,

	INCONSISTENT_CODE, // warning about incorrect decompilation
}
