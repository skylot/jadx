package jadx.core.dex.attributes;

public enum AttributeFlag {
	TRY_ENTER,
	TRY_LEAVE,

	LOOP_START,
	LOOP_END,

	SYNTHETIC,

	BREAK,
	RETURN, // block contains only return instruction

	DONT_SHRINK,
	DONT_GENERATE,
	SKIP,

	SKIP_FIRST_ARG,

	INCONSISTENT_CODE, // warning about incorrect decompilation
}
