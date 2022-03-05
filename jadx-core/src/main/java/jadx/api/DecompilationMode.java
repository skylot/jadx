package jadx.api;

public enum DecompilationMode {
	/**
	 * Trying best options (default)
	 */
	AUTO,

	/**
	 * Restore code structure (normal java code)
	 */
	RESTRUCTURE,

	/**
	 * Simplified instructions (linear with goto's)
	 */
	SIMPLE,

	/**
	 * Raw instructions without modifications
	 */
	FALLBACK
}
