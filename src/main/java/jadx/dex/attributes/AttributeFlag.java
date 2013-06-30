package jadx.dex.attributes;

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

	INCONSISTENT_CODE, // warning about incorrect decompilation
}
