package jadx.core.dex.attributes;

public enum AFlag {
	MTH_ENTER_BLOCK,
	MTH_EXIT_BLOCK,

	TRY_ENTER,
	TRY_LEAVE,

	LOOP_START,
	LOOP_END,

	SYNTHETIC,

	RETURN, // block contains only return instruction
	ORIG_RETURN,

	DONT_WRAP,
	DONT_INLINE,
	DONT_INLINE_CONST,
	DONT_GENERATE, // process as usual, but don't output to generated code
	COMMENT_OUT, // process as usual, but comment insn in generated code
	REMOVE, // can be completely removed
	REMOVE_SUPER_CLASS, // don't add super class

	HIDDEN, // instruction used inside other instruction but not listed in args

	DONT_RENAME, // do not rename during deobfuscation
	FORCE_RAW_NAME, // force use of raw name instead alias

	ADDED_TO_REGION,

	EXC_TOP_SPLITTER,
	EXC_BOTTOM_SPLITTER,
	FINALLY_INSNS,

	SKIP_FIRST_ARG,
	SKIP_ARG, // skip argument in invoke call
	NO_SKIP_ARGS,

	ANONYMOUS_CONSTRUCTOR,
	INLINE_INSTANCE_FIELD,

	THIS,
	SUPER,

	/**
	 * Mark Android resources class
	 */
	ANDROID_R_CLASS,

	/**
	 * RegisterArg attribute for method arguments
	 */
	METHOD_ARGUMENT,

	/**
	 * Type of RegisterArg or SSAVar can't be changed
	 */
	IMMUTABLE_TYPE,

	/**
	 * Force inline instruction with inline assign
	 */
	FORCE_ASSIGN_INLINE,

	CUSTOM_DECLARE, // variable for this register don't need declaration
	DECLARE_VAR,

	ELSE_IF_CHAIN,

	WRAPPED,
	ARITH_ONEARG,

	FALL_THROUGH,

	VARARG_CALL,

	/**
	 * Use constants with explicit type: cast '(byte) 1' or type letter '7L'
	 */
	EXPLICIT_PRIMITIVE_TYPE,
	EXPLICIT_CAST,
	SOFT_CAST, // synthetic cast to help type inference (allow unchecked casts for generics)

	INCONSISTENT_CODE, // warning about incorrect decompilation

	REQUEST_IF_REGION_OPTIMIZE, // run if region visitor again
	REQUEST_CODE_SHRINK,
	RERUN_SSA_TRANSFORM,

	METHOD_CANDIDATE_FOR_INLINE,
	USE_LINES_HINTS, // source lines info in methods can be trusted

	DISABLE_BLOCKS_LOCK,

	// Class processing flags
	RESTART_CODEGEN, // codegen must be executed again
	RELOAD_AT_CODEGEN_STAGE, // class can't be analyzed at 'process' stage => unload before 'codegen' stage
	CLASS_DEEP_RELOAD, // perform deep class unload (reload) before process
	CLASS_UNLOADED, // class was completely unloaded

	DONT_UNLOAD_CLASS, // don't unload class after code generation (only for tests and debug!)
}
