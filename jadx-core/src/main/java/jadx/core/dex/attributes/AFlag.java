package jadx.core.dex.attributes;

public enum AFlag {
	TRY_ENTER,
	TRY_LEAVE,

	LOOP_START,
	LOOP_END,

	SYNTHETIC,

	RETURN, // block contains only return instruction
	ORIG_RETURN,

	DONT_WRAP,
	DONT_INLINE,
	DONT_GENERATE, // process as usual, but don't output to generated code
	DONT_RENAME, // do not rename during deobfuscation
	REMOVE, // can be completely removed
	ADDED_TO_REGION,

	FINALLY_INSNS,

	SKIP_FIRST_ARG,
	SKIP_ARG, // skip argument in invoke call
	ANONYMOUS_CONSTRUCTOR,
	ANONYMOUS_CLASS,

	THIS,

	/**
	 * RegisterArg attribute for method arguments
	 */
	METHOD_ARGUMENT,

	/**
	 * Type of RegisterArg or SSAVar can't be changed
	 */
	IMMUTABLE_TYPE,

	CUSTOM_DECLARE, // variable for this register don't need declaration
	DECLARE_VAR,

	ELSE_IF_CHAIN,

	WRAPPED,
	ARITH_ONEARG,

	FALL_THROUGH,

	EXPLICIT_GENERICS,

	INCONSISTENT_CODE, // warning about incorrect decompilation
}
